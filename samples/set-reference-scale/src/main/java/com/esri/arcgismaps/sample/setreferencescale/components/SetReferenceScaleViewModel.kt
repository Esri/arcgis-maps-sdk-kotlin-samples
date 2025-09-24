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

package com.esri.arcgismaps.sample.setreferencescale.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel to manage map, reference scale and layer toggles for the SetReferenceScale sample.
 */
class SetReferenceScaleViewModel(application: Application) : AndroidViewModel(application) {

    // Create the map from the Isle of Wight portal item.
    val arcGISMap = ArcGISMap(
        item = PortalItem(
            portal = Portal("https://www.arcgis.com"),
            itemId = "3953413f3bd34e53a42bf70f2937a408"
        )
    ).apply {
        // Set the initial reference scale.
        referenceScale = 250_000.0
    }

    // Proxy for controlling the MapView viewpoint scale.
    val mapViewProxy = MapViewProxy()

    // Reference scale options.
    val referenceScaleOptions = listOf(500_000.0, 250_000.0, 100_000.0, 50_000.0)

    // Expose the selected reference scale as a StateFlow.
    private val _selectedReferenceScale = MutableStateFlow(arcGISMap.referenceScale)
    val selectedReferenceScale = _selectedReferenceScale.asStateFlow()

    // Expose the current map scale as a StateFlow (NaN until the MapView reports a scale).
    private val _mapScale = MutableStateFlow(Double.NaN)
    val mapScale = _mapScale.asStateFlow()

    // Expose a list of layers with the map's reference scale state.
    private val _layers = MutableStateFlow<List<LayerToggleState>>(emptyList())
    val layers = _layers.asStateFlow()

    // Helper for showing errors/messages in the UI.
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the map then build the layers list.
        viewModelScope.launch {
            arcGISMap.load().onSuccess {
                val layerStates = arcGISMap.operationalLayers
                    .filterIsInstance<FeatureLayer>()
                    .map { layer ->
                        // If layer is a FeatureLayer, access its properties.
                        LayerToggleState(
                            name = layer.name,
                            scaleSymbols = layer.scaleSymbols
                        )
                    }
                _layers.value = layerStates
            }.onFailure { throwable ->
                messageDialogVM.showMessageDialog(throwable)
            }
        }
    }

    /**
     * Called when new reference scale is selected and update the map's reference scale.
     */
    fun onReferenceScaleSelected(index: Int) {
        val scale = referenceScaleOptions[index]
        _selectedReferenceScale.value = scale
        arcGISMap.referenceScale = scale
    }

    /**
     * Called by the MapView when the map scale changes.
     */
    fun onMapScaleChanged(scale: Double) {
        _mapScale.value = scale
    }

    /**
     * Toggle whether a [FeatureLayer] instance operational layer honors the map reference scale.
     */
    fun onLayerScaleSymbolToggled(toggleState: LayerToggleState, enabled: Boolean) {
        // Set the new value for the layer's scale symbols.
        val newLayerState = toggleState.copy(scaleSymbols = enabled)
        // Update the new layer state on the map.
        arcGISMap.operationalLayers
            .filterIsInstance<FeatureLayer>()
            .find { newLayerState.name == it.name }?.apply {
                scaleSymbols = newLayerState.scaleSymbols
            }
        // Update the UI state.
        _layers.value = _layers.value.map { existingLayerState ->
            if (existingLayerState.name == newLayerState.name) newLayerState else existingLayerState
        }
    }

    /**
     * Set the map scale to the currently selected reference scale using the [mapViewProxy].
     */
    fun setMapScaleToReference() {
        viewModelScope.launch {
            mapViewProxy.setViewpointScale(_selectedReferenceScale.value)
        }
    }
}

// Data class representing a UI row for an operational layer of the map.
data class LayerToggleState(val name: String, val scaleSymbols: Boolean)
