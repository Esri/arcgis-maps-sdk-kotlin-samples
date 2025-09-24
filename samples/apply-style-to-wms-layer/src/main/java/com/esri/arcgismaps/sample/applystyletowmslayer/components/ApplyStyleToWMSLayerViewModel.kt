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

package com.esri.arcgismaps.sample.applystyletowmslayer.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.WmsLayer
import com.arcgismaps.mapping.layers.WmsSublayer
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ApplyStyleToWmsLayerViewModel(app: Application) : AndroidViewModel(app) {
    // WMS layer displaying Minnesota's county boundaries with multiple styles.
    private val wmsLayer = WmsLayer(
        url = "https://imageserver.gisdata.mn.gov/cgi-bin/mncomp?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities",
        layerNames = listOf("mncomp")
    )

    // Map used by the MapView.
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISLightGray).apply {
        // Add the WMS layer to the map's operational layers
        operationalLayers.add(wmsLayer)
    }

    // MapViewProxy to perform view operations (like setting the viewpoint) safely from a ViewModel.
    val mapViewProxy = MapViewProxy()

    // Message dialog ViewModel for error reporting.
    val messageDialogVM = MessageDialogViewModel()

    // Flow of available WMS styles.
    private val _stylesFlow = MutableStateFlow<List<String>>(emptyList())
    val stylesFlow = _stylesFlow.asStateFlow()

    // Selected style index.
    private val _selectedStyleIndex = MutableStateFlow(0)
    val selectedStyleIndex = _selectedStyleIndex.asStateFlow()

    // The WMS sublayer for which we will change the style.
    private var wmsSublayer: WmsSublayer? = null


    init {
        viewModelScope.launch {
            createAndLoadWmsLayer()
        }
    }

    /**
     * Creates the WMS layer, loads it, zooms to its full extent, and exposes its styles.
     */
    private suspend fun createAndLoadWmsLayer() {
        wmsLayer.load().onSuccess {
            // Zoom to the full extent of the WMS layer, if available
            wmsLayer.fullExtent?.let { fullExtent ->
                mapViewProxy.setViewpointAnimated(Viewpoint(boundingGeometry = fullExtent))
            }

            // Get the first WMS sublayer
            wmsSublayer = wmsLayer.sublayers.value.firstOrNull() as? WmsSublayer

            // Obtain available styles from the WMS layer info
            val styles = wmsLayer.layerInfos.firstOrNull()?.styles ?: emptyList()
            if (styles.isEmpty()) {
                messageDialogVM.showMessageDialog("No styles found for the WMS layer.")
                return@onSuccess
            }

            _stylesFlow.value = styles
            // Set initial selection to the first available style
            _selectedStyleIndex.value = 0
            wmsSublayer?.currentStyle = styles[0]
        }.onFailure { error ->
            messageDialogVM.showMessageDialog(
                title = "Failed to load WMS layer",
                description = error.message.toString()
            )
        }
    }

    /**
     * Updates the selected style by index and applies it to the WMS sublayer.
     */
    fun updateSelectedStyle(index: Int) {
        val styles = _stylesFlow.value
        if (index in styles.indices) {
            _selectedStyleIndex.value = index
            wmsSublayer?.currentStyle = styles[index]
        }
    }
}
