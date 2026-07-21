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

package com.esri.arcgismaps.sample.changemapviewbackground.components

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.layers.ArcGISTiledLayer
import com.arcgismaps.mapping.view.BackgroundGrid
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.arcgismaps.Color as ArcGISColor

class ChangeMapViewBackgroundViewModel(app: Application) : AndroidViewModel(app) {

    // A map with a single tiled layer representing world time zones, used as the basemap.
    val arcGISMap = ArcGISMap(
        basemap = Basemap(baseLayer = ArcGISTiledLayer(uri = WORLD_TIME_ZONES_URI))
    )

    // The background grid drawn behind the map's content. Mutating its properties
    // updates the MapView immediately since it's the same instance passed to MapView.
    val backgroundGrid = BackgroundGrid().apply {
        color = ArcGISColor.black
        lineColor = ArcGISColor.white
        lineWidth = 2f
        size = 32f
    }

    // Create a state flow to hold the UI state for the supporting pane controls
    private val _adaptiveUiState = MutableStateFlow(AdaptiveUiState.defaultState)

    // Expose the state flow as read-only for the UI
    val adaptiveUiState = _adaptiveUiState.asStateFlow()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the map and handle any errors by showing a message dialog
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Updates the background grid's fill color.
     */
    fun updateColor(color: Color) {
        _adaptiveUiState.update { currentState ->
            currentState.copy(color = color)
        }
        backgroundGrid.color = color.toArcGISColor()
    }

    /**
     * Updates the background grid's line color.
     */
    fun updateLineColor(color: Color) {
        _adaptiveUiState.update { currentState ->
            currentState.copy(lineColor = color)
        }
        backgroundGrid.lineColor = color.toArcGISColor()
    }

    /**
     * Updates the background grid's line width, in device-independent pixels (DIP).
     */
    fun updateLineWidth(lineWidth: Float) {
        _adaptiveUiState.update { currentState ->
            currentState.copy(lineWidth = lineWidth)
        }
        backgroundGrid.lineWidth = lineWidth
    }

    /**
     * Updates the size of each grid square, in device-independent pixels (DIP).
     */
    fun updateSize(size: Float) {
        _adaptiveUiState.update { currentState ->
            currentState.copy(size = size)
        }
        backgroundGrid.size = size
    }

    companion object {
        /** The valid range for the grid's line width slider. */
        val lineWidthRange = 0f..10f

        /** The valid range for the grid's size slider. */
        val sizeRange = 2f..50f

        private const val WORLD_TIME_ZONES_URI =
            "https://sampleserver6.arcgisonline.com/arcgis/rest/services/WorldTimeZones/MapServer"
    }
}

/**
 * Central UI state for the sample's controls.
 */
data class AdaptiveUiState(
    val color: Color,
    val lineColor: Color,
    val lineWidth: Float,
    val size: Float
) {
    companion object {
        val defaultState = AdaptiveUiState(
            color = Color.Black,
            lineColor = Color.White,
            lineWidth = 2f,
            size = 32f
        )
    }
}

/**
 * Converts a Compose [Color] to the ArcGIS Maps SDK's [ArcGISColor], since
 * BackgroundGrid.color/lineColor are typed as com.arcgismaps.Color rather
 * than androidx.compose.ui.graphics.Color.
 */
private fun Color.toArcGISColor(): ArcGISColor {
    val argb = toArgb()
    return ArcGISColor.fromRgba(
        (argb shr 16) and 0xFF, // r
        (argb shr 8) and 0xFF,  // g
        argb and 0xFF,          // b
        (argb shr 24) and 0xFF  // a
    )
}