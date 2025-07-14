/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.editgeometrieswithprogrammaticreticletool.components

import android.app.Application
import androidx.compose.ui.unit.dp
import com.arcgismaps.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Multipart
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.Symbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditorMidVertex
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditorVertex
import com.arcgismaps.mapping.view.geometryeditor.ProgrammaticReticleTool
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val pinkneysGreenJson = """
    {"rings":
        [[[-84843.262719916485,6713749.9329888355],[-85833.376589175183,6714679.7122141244],
        [-85406.822347959576,6715063.9827222107],[-85184.329997390232,6715219.6195847588],
        [-85092.653857582554,6715119.5391713539],[-85090.446872787768,6714792.7656492386],
        [-84915.369168906298,6714297.8798246197],[-84854.295522911285,6714080.907587287],
        [-84843.262719916485,6713749.9329888355]]],
    "spatialReference":
        {"wkid":102100,"latestWkid":3857}}
"""

private const val beechLodgeBoundaryJson = """
    {"paths":
        [[[-87090.652708065536,6714158.9244240439],[-87247.362370337316,6714232.880689906],
        [-87226.314032974493,6714605.4697726099],[-86910.499335316243,6714488.006312645],
        [-86750.82198052686,6714401.1768307304],[-86749.846825938366,6714305.8450344801]]],
    "spatialReference":
        {"wkid":102100,"latestWkid":3857}}
"""

private const val treeMarkersJson = """
    {"points":
        [[-86750.751150056443,6713749.4529355941],[-86879.381793060631,6713437.3335486846],
        [-87596.503104619667,6714381.7342108283],[-87553.257569537804,6714402.0910389507],
        [-86831.019903597829,6714398.4128562529],[-86854.105933315877,6714396.1957954112],
        [-86800.624094892439,6713992.3374453448]],
    "spatialReference":
        {"wkid":102100,"latestWkid":3857}}
"""

private val geometryTypes = mapOf(
    "Point" to GeometryType.Point,
    "Multipoint" to GeometryType.Multipoint,
    "Polyline" to GeometryType.Polyline,
    "Polygon" to GeometryType.Polygon,
)

private val Color.Companion.blue
    get() = fromRgba(0, 0, 255, 255)

private val Color.Companion.orangeRed
    get() = fromRgba(128, 128, 0, 255)

private val Color.Companion.transparentRed
    get() = fromRgba( 255, 0, 0, 70)

class EditGeometriesWithProgrammaticReticleToolViewModel(app: Application) : AndroidViewModel(app) {

    private var reticleState = ReticleState.Default
    private var selectedGraphic: Graphic? = null
    private val programmaticReticleTool = ProgrammaticReticleTool()

    private val _allowVertexCreation = MutableStateFlow(true)
    val allowVertexCreation: StateFlow<Boolean> = _allowVertexCreation

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISImagery).apply {
            initialViewpoint = Viewpoint(latitude = 51.523806, longitude = -0.775395, scale = 4e4)
        }

    private val _startingGeometryType = MutableStateFlow("Polygon")
    val startingGeometryType: StateFlow<String> = _startingGeometryType

    private val _multiButtonText = MutableStateFlow("")
    val multiButtonText: StateFlow<String> = _multiButtonText

    private val _multiButtonEnabled = MutableStateFlow(true)
    val multiButtonEnabled: StateFlow<Boolean> = _multiButtonEnabled

    private val graphicsOverlay = GraphicsOverlay()
    val graphicsOverlays = listOf(graphicsOverlay)

    val geometryEditor = GeometryEditor().apply {
        tool = programmaticReticleTool
    }

    val messageDialogVM = MessageDialogViewModel()

    val mapViewProxy = MapViewProxy()

    val canDeleteSelectedElement = geometryEditor.selectedElement.map { selectedElement ->
        selectedElement?.canDelete ?: false
    }

    init {
        viewModelScope.run {
            launch {
                arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
            }
            launch {
                geometryEditor.pickedUpElement.collect {
                    updateReticleState()
                }
            }
            launch {
                geometryEditor.hoveredElement.collect {
                    updateReticleState()
                }
            }
        }

        createInitialGraphics()
        updateMultiButtonState() // set initial 'start editor' button text
    }

    /**
     * Stop the editor, discarding the current edit geometry.
     */
    fun onCancelButtonClick() {
        geometryEditor.stop()
        resetExistingEditState()
    }

    /**
     * Delete the currently selected element.
     *
     * Throws if there is no element or the current element can't be deleted.
     */
    fun onDeleteButtonPressed() {
        geometryEditor.deleteSelectedElement()
    }

    /**
     * Stops the editor, saving the edit geometry into a graphics overlay (updating the existing
     * graphic or adding a new one based on how the edit session was started).
     */
    fun onDoneButtonClick() {
        val geometry = geometryEditor.stop()
        if (geometry != null) {
            val selectedGraphic = selectedGraphic
            if (selectedGraphic != null) {
                selectedGraphic.geometry = geometry
            } else {
                graphicsOverlay.graphics.add(Graphic(geometry = geometry, symbol = geometry.defaultSymbol))
            }
        }
        resetExistingEditState()
    }

    /**
     * If the editor is not started, identifies a tapped graphic and starts the editor with its
     * geometry.
     *
     * If the editor is started, sets the viewpoint to the identified (mid-)vertex.
     */
    fun onMapViewTap(tapEvent: SingleTapConfirmedEvent) {
        viewModelScope.launch {
            if (geometryEditor.isStarted.value) {
                selectAndSetViewpointAt(tapEvent.screenCoordinate)
            } else {
                startWithIdentifiedGeometry(tapEvent.screenCoordinate)
            }
        }
    }

    /**
     * Performs different actions based on the editor's current hovered and picked up element, as
     * well as whether vertex creation is allowed. The behaviour is as follows:
     * - If the editor is stopped, starts it with a new geometry with the currently-selected geometry type
     * - Otherwise, if there is a picked up element, places it under the reticle
     * - Otherwise, if a vertex or mid-vertex is hovered over, picks it up
     * - Otherwise, inserts a new vertex at the reticle position
     */
    fun onMultiButtonClick() {
        if (!geometryEditor.isStarted.value) {
            geometryEditor.start(geometryTypes.getOrDefault(startingGeometryType.value, GeometryType.Polygon))
            updateReticleState()
            return
        }

        when (reticleState) {
            ReticleState.Default, ReticleState.PickedUp -> programmaticReticleTool.placeElementAtReticle()
            ReticleState.HoveringVertex, ReticleState.HoveringMidVertex -> {
                programmaticReticleTool.selectElementAtReticle()
                programmaticReticleTool.pickUpSelectedElement()
            }
        }
    }

    /**
     * Redoes the last undone geometry editor action, if any
     */
    fun onRedoButtonPressed() {
        geometryEditor.redo()
    }

    /**
     * If there is a picked up element, places the element back in its original position.
     * Otherwise, undoes the last geometry editor action, if any.
     */
    fun onUndoButtonPressed() {
        if (geometryEditor.pickedUpElement.value != null) {
            geometryEditor.cancelCurrentAction()
        } else {
            geometryEditor.undo()
        }
    }

    /**
     * Enables or disables vertex creation, which affects the feedback lines and vertices, the
     * allowed multi-button actions, and the presence of the grow effect for mid-vertices.
     */
    fun setAllowVertexCreation(newValue: Boolean) {
        _allowVertexCreation.value = newValue
        programmaticReticleTool.vertexCreationPreviewEnabled = newValue
        // Picking up a mid-vertex will lead to a new vertex being created. Only show feedback for
        // this if vertex creation is enabled.
        programmaticReticleTool.style.growEffect?.applyToMidVertices = newValue
        updateMultiButtonState()
    }

    /**
     * Set the starting geometry type, used when not starting with an existing geometry.
     */
    fun setStartingGeometryType(newGeometryType: String) {
        _startingGeometryType.value = newGeometryType
    }

    /**
     * Identifies for a geometry editor element at the given screen coordinates. If a vertex
     * or mid-vertex is found, selects it and centers it in the view.
     */
    private suspend fun selectAndSetViewpointAt(tapPosition: ScreenCoordinate) {
        val identifyResult = mapViewProxy.identifyGeometryEditor(tapPosition, tolerance = 15.dp).getOrNull() ?: return
        val topElement = identifyResult.elements.firstOrNull() ?: return
        when (topElement) {
            is GeometryEditorVertex -> {
                geometryEditor.selectVertex(topElement.partIndex, topElement.vertexIndex)
                mapViewProxy.setViewpointCenter(topElement.point)
            }
            is GeometryEditorMidVertex -> {
                if (allowVertexCreation.value) {
                    geometryEditor.selectMidVertex(topElement.partIndex, topElement.segmentIndex)
                    mapViewProxy.setViewpointCenter(topElement.point)
                }
            }
            else -> { /* Only zoom to vertices and mid-vertices. */ }
        }
    }

    /**
     * Identifies for an existing graphic in the graphic overlay. If found, starts the geometry editor
     * using the graphic's geometry. Hides the existing graphic for the duration of the edit session.
     * Sets a new viewpoint center based on the graphic position (depending on what type of edits
     * are allowed).
     */
    private suspend fun startWithIdentifiedGeometry(tapPosition: ScreenCoordinate) {
        val identifyResult = mapViewProxy.identify(graphicsOverlay,tapPosition, tolerance = 15.dp).getOrNull() ?: return
        val graphic = identifyResult.graphics.firstOrNull() ?: return
        geometryEditor.start(graphic.geometry ?: return)

        graphic.geometry?.let { geometry ->
            if (allowVertexCreation.value) {
                mapViewProxy.setViewpointCenter(geometry.extent.center)
            } else {
                geometry.lastPoint?.let { lastPoint ->
                    mapViewProxy.setViewpointCenter(lastPoint)
                }
            }
        }

        graphic.isSelected = true
        graphic.isVisible = false
        selectedGraphic = graphic
        updateReticleState()
    }

    /**
     * Populates the graphics overlay with some starting graphics for editing.
     */
    private fun createInitialGraphics() {
        val treeMarkers = Geometry.fromJsonOrNull(treeMarkersJson) as Multipoint
        graphicsOverlay.graphics.add(Graphic(geometry = treeMarkers, symbol = treeMarkers.defaultSymbol))

        val beechLodgeBoundary = Geometry.fromJsonOrNull(beechLodgeBoundaryJson) as Polyline
        graphicsOverlay.graphics.add(Graphic(geometry = beechLodgeBoundary, symbol = beechLodgeBoundary.defaultSymbol))

        val pinkeysGreen = Geometry.fromJsonOrNull(pinkneysGreenJson) as Polygon
        graphicsOverlay.graphics.add(Graphic(geometry = pinkeysGreen, symbol = pinkeysGreen.defaultSymbol))
    }

    /**
     * Called whenever something happens that may change what the multi-functional button does
     * (e.g. editor stopping/starting, hovered or picked-up element changing).
     *
     * The private [reticleState] property decides what happens when the multi-functional button
     * is pressed, and is used to derive the text and enabled-ness of the button.
     */
    private fun updateReticleState() {
        if (geometryEditor.pickedUpElement.value != null) {
            reticleState = ReticleState.PickedUp
            updateMultiButtonState()
            return
        }

        reticleState = when (geometryEditor.hoveredElement.value) {
            is GeometryEditorVertex -> ReticleState.HoveringVertex
            is GeometryEditorMidVertex -> ReticleState.HoveringMidVertex
            else -> ReticleState.Default
        }

        updateMultiButtonState()
    }

    /**
     * Sets the text and enabled-ness of the multi-functional button based on the current reticle
     * state as well as whether vertex creation is allowed.
     *
     * Note that the enabled-ness of the button is used to prevent vertex insertion when vertex
     * creation is disabled.
     */
    private fun updateMultiButtonState() {
        if (!geometryEditor.isStarted.value) {
            _multiButtonText.value = "Start Geometry Editor"
            _multiButtonEnabled.value = true
            return
        }

        if (allowVertexCreation.value) {
            _multiButtonEnabled.value = true
            _multiButtonText.value = when (reticleState) {
                ReticleState.Default -> "Insert Point"
                ReticleState.PickedUp -> "Drop Point"
                ReticleState.HoveringVertex, ReticleState.HoveringMidVertex -> "Pick Up Point"
            }
        } else {
            _multiButtonText.value = when (reticleState) {
                ReticleState.PickedUp -> "Drop Point"
                ReticleState.Default, ReticleState.HoveringVertex, ReticleState.HoveringMidVertex -> "Pick Up Point"
            }
            _multiButtonEnabled.value = reticleState == ReticleState.HoveringVertex || reticleState == ReticleState.PickedUp
        }
    }

    /**
     * Clear the state for the currently selected graphic.
     */
    private fun resetExistingEditState() {
        selectedGraphic?.let { selectedGraphic ->
            selectedGraphic.isSelected = false
            selectedGraphic.isVisible = true
        }
        selectedGraphic = null
        updateMultiButtonState()
    }

    private enum class ReticleState {
        Default,
        PickedUp,
        HoveringVertex,
        HoveringMidVertex,
    }
}

/**
 * A default symbol for the given geometry.
 */
private val Geometry.defaultSymbol: Symbol
    get() = when (this) {
        is Envelope -> throw IllegalStateException("Envelopes not supported by the geometry editor.")
        is Polygon -> SimpleFillSymbol(
            SimpleFillSymbolStyle.Solid,
            Color.transparentRed,
            outline = SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color.black, 1f)
        )
        is Polyline -> SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.blue, 2f)
        is Multipoint -> SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.yellow, 5f)
        is Point -> SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Square, Color.orangeRed, 10f)
    }

/**
 * The last point of the first part of the given geometry (assumes there is at least one point).
 */
private val Geometry.lastPoint: Point?
    get() = when (this) {
        is Envelope -> throw IllegalStateException("Envelopes not supported by the geometry editor.")
        is Multipart -> parts.firstOrNull()?.endPoint
        is Point -> this
        is Multipoint -> points.lastOrNull()
    }

