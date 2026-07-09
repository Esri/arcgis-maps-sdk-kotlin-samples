/* Copyright 2026 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.esri.arcgismaps.sample.displaygeometryeditorinformationduringinteraction.components

import android.app.Application
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Multipart
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditorInteractionPreview
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditorInteractionType
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditorTool
import com.arcgismaps.mapping.view.geometryeditor.VertexTool
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.atan2

class DisplayGeometryEditorInformationDuringInteractionViewModel(app: Application) :
    AndroidViewModel(app) {
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
        initialViewpoint = Viewpoint(
            center = Point(
                x = -13045202.018086127,
                y = 4035612.571361517,
                SpatialReference.webMercator()
            ),
            scale = 40000.0
        )
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // Create a MapViewProxy that will be used to identify features in the MapView and set the viewpoint
    val mapViewProxy = MapViewProxy()

    // Create a graphic to hold graphics identified on tap
    private var identifiedGraphic: Graphic? = null

    // Create a graphics overlay
    val graphicsOverlay = GraphicsOverlay()

    // Create a geometry editor
    val geometryEditor = GeometryEditor().apply {
        tool = VertexTool().apply {
            configuration.apply {
                allowVertexCreation = false
                allowMidVertexSelection = false
                allowDeletingSelectedElement = false
                allowVertexSelection = false
                allowPartCreation = false
            }
        } as GeometryEditorTool
    }

    private val polygonLineSymbol = SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Dash,
        color = Color.black,
        width = 1f
    )
    private val polygonSymbol = SimpleFillSymbol(
        style = SimpleFillSymbolStyle.Solid,
        color = Color.fromRgba(r = 255, g = 0, b = 0, a = 100),
        outline = polygonLineSymbol
    )
    private val polylineSymbol = SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Solid,
        color = Color.blue,
        width = 2f
    )
    private val pointSymbol = SimpleMarkerSymbol(
        style = SimpleMarkerSymbolStyle.Square,
        color = Color.red,
        size = 10f
    )

    // State flow of rotation angle, scale factors, or coordinates, for presentation in UI
    private val _interactionTransformationFlow = MutableStateFlow<String?>(null)
    val interactionTransformationFlow = _interactionTransformationFlow.asStateFlow()

    init {
        viewModelScope.launch {

            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Failed to load map",
                    description = error.message.toString()
                )
            }.onSuccess {
                // Create graphics for the initial geometries and add them to the graphics overlay
                graphicsOverlay.graphics.addAll(
                    listOf(
                        Graphic(
                            geometry = Geometry.fromJsonOrNull(redlandsPolygonJson),
                            symbol = polygonSymbol
                        ),
                        Graphic(
                            geometry = Geometry.fromJsonOrNull(redlandsPolylineJson),
                            symbol = polylineSymbol
                        ),
                        Graphic(
                            geometry = Geometry.fromJsonOrNull(redlandsMultipointJson),
                            symbol = pointSymbol
                        )
                    )
                )

                geometryEditor.interactionPreviewChanged.collect { interactionPreviewChanged ->
                    interactionPreviewChanged.interactionPreview?.let { interactionPreview ->
                        _interactionTransformationFlow.value =
                            buildInteractionPreviewFormattedMessage(interactionPreview)
                    }

                }

            }
        }
    }

    /**
     * Calculates the transformation information based on the current interaction preview and
     * updates the state flow.
     */
    private fun buildInteractionPreviewFormattedMessage(interactionPreview: GeometryEditorInteractionPreview): String? {
        // For each type of interaction, create a TransformationInformation object with the relevant data
        when (interactionPreview.interactionType) {
            is GeometryEditorInteractionType.Rotate -> {
                return calculateRotation(interactionPreview)
            }

            is GeometryEditorInteractionType.Scale -> {
                return calculateScaleFactors(interactionPreview)
            }

            is GeometryEditorInteractionType.Move -> {
                // Get the center point of the preview geometry.
                val centerPoint = interactionPreview.previewGeometry.extent.center
                return "Center (X, Y): " +
                        "${String.format(Locale.getDefault(), "%.2f", centerPoint.x)}, " +
                        String.format(Locale.getDefault(), "%.2f", centerPoint.y)
            }

            else -> return null
        }
    }

    /**
     * Calculates the scale factors based on the current interaction preview and
     * returns a string representation of the scale factors.
     */
    private fun calculateScaleFactors(preview: GeometryEditorInteractionPreview): String? {
        // Get the extent of the existing geometry excluding the current interaction
        val geometryExtent = geometryEditor.geometry.value?.extent
        val previewExtent = preview.previewGeometry.extent
        return if (
            geometryExtent != null &&
            geometryExtent.width != 0.0 &&
            geometryExtent.height != 0.0
        ) {
            // Calculate the scale factors using the two extents
            val scaleX = previewExtent.width.div(geometryExtent.width)
            val scaleY = previewExtent.height.div(geometryExtent.height)

            "Scale Factor (X,Y): ${
                String.format(
                    Locale.getDefault(),
                    "%.2f",
                    scaleX
                )
            }, ${String.format(Locale.getDefault(), "%.2f", scaleY)}"
        } else {
            null
        }
    }

    /**
     * Calculates the rotation angle based on the current interaction preview and
     * returns a string representation of the rotation angle in degrees.
     */
    fun calculateRotation(interactionPreview: GeometryEditorInteractionPreview): String? {
        if (interactionPreview.interactionType is GeometryEditorInteractionType.Rotate) {
            // Get the original geometry for comparison
            val originalGeometry = geometryEditor.geometry.value ?: return null
            // Get the center point of the original geometry.
            val center = originalGeometry.extent.center
            // Create variables to hold the original and preview points for rotation calculation.
            var originalPoint: Point?
            var previewPoint: Point?

            // Determine the type of geometry being previewed and extract the relevant points for rotation calculation.
            when (val previewGeom = interactionPreview.previewGeometry) {
                is Polyline, is Polygon -> {
                    originalPoint =
                        (originalGeometry as Multipart).parts[0].points.firstOrNull { point -> point != center }
                    previewPoint =
                        previewGeom.parts[0].points.firstOrNull { point -> point != center }
                }

                is Multipoint -> {
                    originalPoint =
                        (originalGeometry as Multipoint).points.firstOrNull { point -> point != center }
                    previewPoint = previewGeom.points.firstOrNull { point -> point != center }
                }

                else -> {
                    // Not expecting any other geometry types in this sample.
                    throw IllegalArgumentException("Unexpected geometry type: $previewGeom")
                }
            }

            // Calculate the rotation angle if both original and preview points are available and different from each other.
            val op = originalPoint ?: return null
            val pp = previewPoint ?: return null
            if (op == pp) return null

            val vector1X = op.x - center.x
            val vector2X = pp.x - center.x
            val vector1Y = op.y - center.y
            val vector2Y = pp.y - center.y

            val cross = vector1X * vector2Y - vector1Y * vector2X
            val dot = vector1X * vector2X + vector1Y * vector2Y

            val angle = atan2(cross, dot) * (180.0 / Math.PI) // Convert to degrees
            val clockwiseNormalized = ((-angle % 360) + 360) % 360
            return "Rotation Angle (degrees): ${
                String.format(
                    Locale.getDefault(),
                    "%.2f",
                    clockwiseNormalized
                )
            }"
        }
        return null
    }


    /**
     * Stops the GeometryEditor and updates the identified graphic.
     */
    fun stopEditor() {
        // Check if there was a previously identified graphic
        identifiedGraphic?.let { graphic ->
            // Update the identified graphic geometry
            graphic.geometry = geometryEditor.stop()
            // Set the original graphic to visible again
            graphic.isVisible = true
        }
    }

    /**
     * Discards the current changes made in the GeometryEditor.
     */
    fun discardEdits() {
        geometryEditor.stop()
        // Update previously identified graphic to be visible again
        identifiedGraphic?.let { graphic ->
            graphic.isVisible = true
        }
    }

    /**
     * Undoes the last event on the geometry editor if possible.
     */
    fun undoEdit() {
        if (geometryEditor.canUndo.value) {
            geometryEditor.undo()
        }
    }

    /**
     * Redoes the last event on the geometry editor if possible.
     */
    fun redoEdit() {
        if (geometryEditor.canRedo.value) {
            geometryEditor.redo()
        }
    }

    /**
     * Identifies the graphic at the tapped screen coordinate in the provided [singleTapConfirmedEvent]
     * and starts the GeometryEditor using the identified graphic's geometry. Hide the BottomSheet on
     * [singleTapConfirmedEvent].
     */
    fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        viewModelScope.launch {
            if (!geometryEditor.isStarted.value) {

                // Attempt to identify a graphic at the location the user tapped
                val graphicsResult = mapViewProxy.identifyGraphicsOverlays(
                    screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                    tolerance = 10.0.dp,
                    returnPopupsOnly = false
                ).getOrNull()

                if (graphicsResult != null) {
                    if (graphicsResult.isNotEmpty()) {
                        // Get the tapped graphic
                        identifiedGraphic = graphicsResult.first().graphics.first()
                        // Start the geometry editor with the identified graphic
                        identifiedGraphic?.let { graphic ->
                            graphic.geometry?.let { geometry ->
                                geometryEditor.start(geometry)
                                geometryEditor.selectGeometry()
                                graphic.isVisible = false
                            }
                        }
                    }
                }
            }
        }
    }

    // json formatted strings for initial geometries
    private val redlandsPolygonJson = """{ "rings": [[[-13046991.222211758,4034618.5047884779],
            [-13046991.222211758,4035962.0723415823],
            [-13045677.652220398,4035962.0723415823],
            [-13045677.652220398,4034618.5047884779],
            [-13046991.222211758,4034618.5047884779]]],
            "spatialReference":{"wkid":3857}}"""
    private val redlandsPolylineJson = """{ "paths": [[[-13044533.805088846,4034221.5100018946],
            [-13043597.938505623,4034197.1337576872],
            [-13043597.938505623,4035135.572073034],
            [-13044522.634505576,4035170.5449295067]]],
            "spatialReference":{"wkid":3857}}"""
    private val redlandsMultipointJson = """{ "points": [[-13045283.292102993,4035739.1925106063],
            [-13045314.922186911,4036533.8852012255],
            [-13044798.24723932,4036138.7808295386],
            [-13044354.514637273,4035719.3623426706],
            [-13044281.57229173,4036473.0999132735]],
            "spatialReference":{"wkid":3857}}"""

}
