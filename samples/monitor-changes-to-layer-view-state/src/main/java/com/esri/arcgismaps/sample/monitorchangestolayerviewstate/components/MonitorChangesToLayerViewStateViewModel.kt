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

package com.esri.arcgismaps.sample.monitorchangestolayerviewstate.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.view.LayerViewStatus
import com.arcgismaps.portal.Portal
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel that creates the map and feature layer, observes load state and exposes
 * simple state flows for UI to display layer visibility and the current layer view status.
 */
class MonitorChangesToLayerViewStateViewModel(app: Application) : AndroidViewModel(app) {
    // The Satellite (MODIS) Thermal Hotspots and Fire Activity portal item with a feature layer.
    private val featureLayer: FeatureLayer by lazy {
        val portalItem = PortalItem(
            portal = Portal("https://www.arcgis.com"),
            itemId = "b8f4033069f141729ffb298b7418b653"
        )
        FeatureLayer.createWithItem(item = portalItem).apply {
            minScale = 1e8
            maxScale = 6e6
        }
    }

    // Map with Topographic basemap and initial viewpoint
    val arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = Viewpoint(
            center = Point(x = -13e6, y = 51e5, spatialReference = SpatialReference.webMercator()),
            scale = 2e7
        )
        // Add the feature layer to the map's operational layers
        operationalLayers.add(featureLayer)

    }

    // Expose whether the layer is visible, UI can trigger flow in updateLayerVisibility().
    private val _layerIsVisibleFlow = MutableStateFlow(true)
    val layerIsVisibleFlow = _layerIsVisibleFlow.asStateFlow()

    // Expose the current LayerViewStatus simple names for the UI
    private val _layerStatusLabelsFlow = MutableStateFlow(listOf("N/A"))
    val layerStatusLabelsFlow = _layerStatusLabelsFlow.asStateFlow()

    // Message dialog for displaying errors
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            // Load the map which contains the configured feature layer
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(error)
            }
        }
    }

    /**
     * Called from the MapView's onLayerViewStateChanged callback.
     */
    fun onLayerViewStateChanged(
        layer: Layer,
        layerViewStatusList: List<LayerViewStatus>
    ) {
        // Only observe state of the added feature layer.
        if (layer.id != featureLayer.id) return

        // Convert the current layer view status list
        // to a list of plain strings using ::class.java.simpleName
        _layerStatusLabelsFlow.value = if (layerViewStatusList.isNotEmpty()) {
            layerViewStatusList.map { it::class.java.simpleName }
        } else {
            listOf("N/A")
        }
    }

    /**
     * Toggle visibility of the sample feature layer and update flow state.
     */
    fun updateLayerVisibility(visible: Boolean) {
        _layerIsVisibleFlow.value = visible
        featureLayer.isVisible = visible
    }
}
