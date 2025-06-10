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

package com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.components

import android.app.Application
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import kotlinx.coroutines.launch

class MapViewModel(app: Application) : AndroidViewModel(app) {

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic)
    val graphicsOverlay = GraphicsOverlay()
    val geometryEditor = GeometryEditor()

    private val _statusText = mutableStateOf("Tap on map or use current location to create start point")
    val statusText: MutableState<String> = _statusText

    private val _showElevationDialog = mutableStateOf(false)
    val showElevationDialog: MutableState<Boolean> = _showElevationDialog

    private val _elevationInput = mutableFloatStateOf(0f)
    val elevationInput: MutableFloatState = _elevationInput

    var graphic = Graphic()

    var polyline: Polyline? = null

    private val _isGeometryBeingEdited = mutableStateOf(false)
    val isGeometryBeingEdited: MutableState<Boolean> = _isGeometryBeingEdited

    init {
        startPolylineEditing()
    }

    /**
     * Initialize the location display.
     */
    fun initialize(locationDisplay: LocationDisplay) {
        with(viewModelScope) {
            launch {
                locationDisplay.dataSource.start()
            }
            launch {
                geometryEditor.geometry.collect {
                    (it as? Polyline)?.let { polyline ->
                        _isGeometryBeingEdited.value =
                            polyline.parts.isNotEmpty() && (polyline.parts.firstOrNull()?.points?.toList()?.size!! > 1)
                    }
                }
            }
        }
    }

    /**
     * Starts the GeometryEditor for creating a polyline.
     */
    fun startPolylineEditing() {
        geometryEditor.start(GeometryType.Polyline)
        _statusText.value = "Polyline editing started. Tap to add points."
    }

    /**
     * Completes the polyline and adds it as a graphic to the map.
     */
    fun completePolyline() {
        polyline = geometryEditor.stop() as? Polyline
        if (polyline != null) {
            graphic = Graphic(
                polyline,
                symbol = SimpleLineSymbol(style = SimpleLineSymbolStyle.Solid, color = Color.red, width = 2f)
            )
            _showElevationDialog.value = true
            graphicsOverlay.graphics.add(graphic)

            _statusText.value =
                "Polyline completed. Tap again to continue adding polylines or proceed to rendering in Augmented Reality."
        } else {
            _statusText.value = "No geometry created. Try again."
        }
    }

    fun onElevationConfirmed(elevation: Float) {
        _elevationInput.floatValue = elevation
        _showElevationDialog.value = false
        polyline?.let { pipelineGeometry ->
            SharedRepository.pipeInfoList.add(PipeInfo(pipelineGeometry, _elevationInput.floatValue))
        }
    }

    fun onElevationDialogDismissed() {
        _showElevationDialog.value = false
    }
}

data class PipeInfo(
    val polyline: Polyline,
    val offset: Float
)
