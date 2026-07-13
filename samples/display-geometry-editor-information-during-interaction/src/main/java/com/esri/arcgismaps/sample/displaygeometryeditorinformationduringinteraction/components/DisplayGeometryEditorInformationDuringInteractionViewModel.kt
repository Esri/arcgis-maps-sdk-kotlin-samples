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
import com.arcgismaps.mapping.view.geometryeditor.VertexTool
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2

class DisplayGeometryEditorInformationDuringInteractionViewModel(app: Application) :
    AndroidViewModel(app) {

    // Create a map with an initial viewpoint of Redlands, CA.
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
        initialViewpoint = Viewpoint(
            center = Point(
                x = -13.045e6, y = 4.0356e6,
                spatialReference = SpatialReference.webMercator()
            ),
            scale = 4e4
        )
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // Create a MapViewProxy that will be used to identify features in the MapView
    val mapViewProxy = MapViewProxy()

    // Track graphics identified on tap
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
        }
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

    // State flow string message of rotation angle, scale factors, or coordinates, for presentation in UI
    private val _interactionTransformationMessage = MutableStateFlow<String?>(null)
    val interactionTransformationMessage = _interactionTransformationMessage.asStateFlow()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }.onSuccess {
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
                    val interactionMessage = interactionPreviewChanged.interactionPreview
                        ?.toFormattedMessage(geometryEditor.geometry.value)
                    _interactionTransformationMessage.value = interactionMessage
                }
            }
        }
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
     * and starts the GeometryEditor using the identified graphic's geometry.
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
}

/**
 * Converts the transformation information to a formatted string using the editor interaction preview, type, or [geometry].
 */
private fun GeometryEditorInteractionPreview.toFormattedMessage(geometry: Geometry?): String? {
    return when (interactionType) {
        is GeometryEditorInteractionType.Rotate -> buildRotationMessage(geometry)
        is GeometryEditorInteractionType.Scale -> buildScaleMessage(geometry)
        is GeometryEditorInteractionType.Move -> buildMoveMessage()
        else -> null
    }
}

/**
 * Calculates the rotation angle based on the current interaction preview and
 * returns a string representation of the rotation angle in degrees.
 */
private fun GeometryEditorInteractionPreview.buildRotationMessage(geometry: Geometry?): String? {
    val sourceGeometry = geometry ?: return null
    val center = sourceGeometry.extent.center
    val originalPoint = sourceGeometry.firstRotationReferencePoint(center) ?: return null
    val previewPoint = previewGeometry.firstRotationReferencePoint(center) ?: return null
    if (originalPoint == previewPoint) return null

    val vector1X = originalPoint.x - center.x
    val vector2X = previewPoint.x - center.x
    val vector1Y = originalPoint.y - center.y
    val vector2Y = previewPoint.y - center.y

    val cross = vector1X * vector2Y - vector1Y * vector2X
    val dot = vector1X * vector2X + vector1Y * vector2Y
    val angle = atan2(cross, dot) * (180.0 / PI)
    val clockwiseNormalized = ((-angle % 360) + 360) % 360

    return "Rotation Angle (degrees): ${clockwiseNormalized.toDisplayString()}"
}

/**
 * Calculates the scale factors based on the current interaction preview and
 * returns a string representation of the scale factors.
 */
private fun GeometryEditorInteractionPreview.buildScaleMessage(geometry: Geometry?): String? {
    val geometryExtent = geometry?.extent ?: return null
    if (geometryExtent.width == 0.0 || geometryExtent.height == 0.0) return null
    val previewExtent = previewGeometry.extent

    return formatValuePair(
        label = "Scale Factor (X, Y)",
        first = previewExtent.width / geometryExtent.width,
        second = previewExtent.height / geometryExtent.height
    )
}

/**
 * Calculates the center point of the current interaction preview to
 * return a formatted message for move interactions.
 */
private fun GeometryEditorInteractionPreview.buildMoveMessage(): String {
    return formatValuePair(
        label = "Center (X, Y)",
        first = previewGeometry.extent.center.x,
        second = previewGeometry.extent.center.y
    )
}

/**
 * Returns the first point in the geometry that is not the [center] point.
 */
private fun Geometry.firstRotationReferencePoint(center: Point): Point? {
    return when (this) {
        is Polyline, is Polygon -> parts[0].points.firstOrNull { point -> point != center }
        is Multipoint -> points.firstOrNull { point -> point != center }
        else -> null
    }
}

private fun formatValuePair(label: String, first: Double, second: Double): String {
    return "$label: ${first.toDisplayString()}, ${second.toDisplayString()}"
}

private fun Double.toDisplayString(): String {
    return String.format(Locale.getDefault(), "%.2f", this)
}

// JSON formatted strings for displayed geometries.
private const val redlandsPolygonJson = """{ "rings": [[[-13046991.222211758,4034618.5047884779],
            [-13046991.222211758,4035962.0723415823],
            [-13045677.652220398,4035962.0723415823],
            [-13045677.652220398,4034618.5047884779],
            [-13046991.222211758,4034618.5047884779]]],
            "spatialReference":{"wkid":3857}}"""
private const val redlandsPolylineJson = """{ "paths": [[[-13044533.805088846,4034221.5100018946],
            [-13043597.938505623,4034197.1337576872],
            [-13043597.938505623,4035135.572073034],
            [-13044522.634505576,4035170.5449295067]]],
            "spatialReference":{"wkid":3857}}"""
private const val redlandsMultipointJson = """{ "points": [[-13045283.292102993,4035739.1925106063],
            [-13045314.922186911,4036533.8852012255],
            [-13044798.24723932,4036138.7808295386],
            [-13044354.514637273,4035719.3623426706],
            [-13044281.57229173,4036473.0999132735]],
            "spatialReference":{"wkid":3857}}"""
