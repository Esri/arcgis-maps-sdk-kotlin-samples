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

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISImagery).apply {
            initialViewpoint = Viewpoint(latitude = 51.523806, longitude = -0.775395, scale = 4e4)
        }

    private val _currentGeometryType = MutableStateFlow("Polygon")
    val startingGeometryType = _currentGeometryType

    private val _multiButtonText = MutableStateFlow("")
    val multiButtonText = _multiButtonText

    private val _multiButtonEnabled = MutableStateFlow(true)
    val multiButtonEnabled = _multiButtonEnabled

    private val graphicsOverlay = GraphicsOverlay()
    val graphicsOverlays = listOf(graphicsOverlay)

    val geometryEditor = GeometryEditor().apply {
        tool = programmaticReticleTool
    }

    val messageDialogVM = MessageDialogViewModel()

    val mapViewProxy = MapViewProxy()

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
        updateMultiButtonText() // set initial 'start editor' button text
    }

    fun onCancelButtonClick() {
        geometryEditor.stop()
        resetExistingEditState()
    }

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

    fun onMapViewTap(tapEvent: SingleTapConfirmedEvent) {
        viewModelScope.launch {
            if (geometryEditor.isStarted.value) {
                // TODO: identify editor and set viewpoint
            } else {
                startWithIdentifiedGeometry(tapEvent.screenCoordinate)
            }
        }
    }

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

    fun setStartingGeometryType(newGeometryType: String) {
        _currentGeometryType.value = newGeometryType
    }

    private suspend fun startWithIdentifiedGeometry(tapPosition: ScreenCoordinate) {
        val identifyResult = mapViewProxy.identify(graphicsOverlay,tapPosition, tolerance = 15.dp).getOrNull() ?: return
        val graphic = identifyResult.graphics.firstOrNull() ?: return
        geometryEditor.start(graphic.geometry ?: return)
        graphic.isSelected = true
        graphic.isVisible = false
        selectedGraphic = graphic
        updateReticleState()
    }

    private fun createInitialGraphics() {
        val treeMarkers = Geometry.fromJsonOrNull(treeMarkersJson) as Multipoint
        graphicsOverlay.graphics.add(Graphic(geometry = treeMarkers, symbol = treeMarkers.defaultSymbol))

        val beechLodgeBoundary = Geometry.fromJsonOrNull(beechLodgeBoundaryJson) as Polyline
        graphicsOverlay.graphics.add(Graphic(geometry = beechLodgeBoundary, symbol = beechLodgeBoundary.defaultSymbol))

        val pinkeysGreen = Geometry.fromJsonOrNull(pinkneysGreenJson) as Polygon
        graphicsOverlay.graphics.add(Graphic(geometry = pinkeysGreen, symbol = pinkeysGreen.defaultSymbol))
    }

    private fun updateReticleState() {
        if (geometryEditor.pickedUpElement.value != null) {
            reticleState = ReticleState.PickedUp
            updateMultiButtonText()
            return
        }

        reticleState = when (geometryEditor.hoveredElement.value) {
            is GeometryEditorVertex -> ReticleState.HoveringVertex
            is GeometryEditorMidVertex -> ReticleState.HoveringMidVertex
            else -> ReticleState.Default
        }

        updateMultiButtonText()
    }

    private fun updateMultiButtonText() {
        if (!geometryEditor.isStarted.value) {
            _multiButtonText.value = "Start Geometry Editor"
            return
        }

        _multiButtonText.value = when (reticleState) {
            ReticleState.Default -> "Insert Point"
            ReticleState.PickedUp -> "Drop Point"
            ReticleState.HoveringVertex, ReticleState.HoveringMidVertex -> "Pick Up Point"
        }
    }

    private fun resetExistingEditState() {
        selectedGraphic?.let {
            it.isSelected = false
            it.isVisible = true
        }
        selectedGraphic = null
        updateMultiButtonText()
    }

    private enum class ReticleState {
        Default,
        PickedUp,
        HoveringVertex,
        HoveringMidVertex,
    }
}

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
