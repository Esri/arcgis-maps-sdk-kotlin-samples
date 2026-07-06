/* Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.displaycomposablemapview.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(app: Application) : AndroidViewModel(app) {

    // TODO - Use mutable state if the map instance changes to allow screen to observe updates.
    val arcGISMap = ArcGISMap(spatialReference = SpatialReference.webMercator()).apply {
        initialViewpoint = Viewpoint(34.05, -117.19, 2e6)
    }

    // Create a state flow to hold the UI state for the supporting pane controls
    private val _adaptiveUiState = MutableStateFlow(AdaptiveUiState.defaultState)

    // Expose the state flow as read-only for the UI
    val adaptiveUiState = _adaptiveUiState.asStateFlow()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Set the initial basemap based on the default state
        updateBasemap(selectedBasemap = _adaptiveUiState.value.basemapOptions)
        // Load the map and handle any errors by showing a message dialog
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * TODO: Screen triggers the function to reflect an update for UI state and ArcGIS objects in this view model.
     */
    fun updateLayerVisibility(isVisible: Boolean) {
        // Update the UI state:
        _adaptiveUiState.update { currentState ->
            currentState.copy(isLayersEnabled = isVisible)
        }
        // Apply the state change the viewmodel objects:
        applyLayerVisibility(isVisible = isVisible)
    }

    /**
     * TODO: Screen triggers the function to reflect an update for UI state and ArcGIS objects in this view model.
     */
    fun updateBasemap(selectedBasemap: BasemapOptions) {
        // Update the UI state:
        _adaptiveUiState.update { currentState ->
            currentState.copy(basemapOptions = selectedBasemap)
        }
        // Apply the state change the viewmodel objects:
        val basemap = Basemap(basemapStyle = selectedBasemap.getBasemapStyle())
        arcGISMap.setBasemap(basemap = basemap)
        viewModelScope.launch {
            basemap.load()
            applyLayerVisibility(isVisible = _adaptiveUiState.value.isLayersEnabled)
        }
    }

    /**
     * TODO: Use private functions to encapsulate the logic for applying state changes to the ArcGIS objects in this view model.
     */
    private fun applyLayerVisibility(isVisible: Boolean) {
        arcGISMap.basemap.value?.referenceLayers?.forEach { layer ->
            layer.isVisible = isVisible
        }
    }
}

/**
 * TODO: Central UI state for sample controls, use the default state to preview sample.
 *  Rename <adaptive> to a noun/adjective from the sample title, e.g. IdentifyUiState.
 */
data class AdaptiveUiState(
    val basemapOptions: BasemapOptions,
    val isLayersEnabled: Boolean
) {
    companion object {
        val defaultState = AdaptiveUiState(
            basemapOptions = BasemapOptions.Light,
            isLayersEnabled = true
        )

    }
}

/**
 * TODO: Use custom enums / data class to provide ease of UI options.
 */
enum class BasemapOptions {
    Light, Dark
}

/**
 * TODO: Use Kotlin extensions for types to simplify sample workflow.
 */
fun BasemapOptions.getBasemapStyle(): BasemapStyle {
    return when (this) {
        BasemapOptions.Light -> BasemapStyle.ArcGISLightGray
        BasemapOptions.Dark -> BasemapStyle.ArcGISDarkGray
    }
}
