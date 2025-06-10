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

package com.esri.arcgismaps.sample.applyrasterrenderingrule.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.raster.ImageServiceRaster
import com.arcgismaps.raster.RenderingRule
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val IMAGE_SERVICE_URL = "https://sampleserver6.arcgisonline.com/arcgis/rest/services/CharlotteLAS/ImageServer"

class ApplyRasterRenderingRuleViewModel(app: Application) : AndroidViewModel(app) {

    val mapViewProxy = MapViewProxy()

    var arcGISMap = ArcGISMap(BasemapStyle.ArcGISStreets)

    // List of raster layers, each with a different rendering rule
    private val _rasterLayers = MutableStateFlow<List<RasterLayer>>(emptyList())
    val rasterLayers = _rasterLayers.asStateFlow()

    // List of rendering rule names for the dropdown
    private val _renderingRuleNames = MutableStateFlow<List<String>>(emptyList())
    val renderingRuleNames = _renderingRuleNames.asStateFlow()

    // Currently selected rendering rule name
    private val _selectedRenderingRuleName = MutableStateFlow("")
    val selectedRenderingRuleName = _selectedRenderingRuleName.asStateFlow()

    // Message dialog for error handling
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            createRasterLayers()
        }
    }

    /**
     * Loads the image service raster, gets all rendering rule infos, and creates a raster layer for each rule.
     */
    private suspend fun createRasterLayers() {
        // Create and load the base image service raster
        val imageServiceRaster = ImageServiceRaster(IMAGE_SERVICE_URL)
        imageServiceRaster.load().onFailure {
            messageDialogVM.showMessageDialog(
                title = "Failed to load image service raster",
                description = it.message.toString()
            )
            return
        }
        val serviceInfo = imageServiceRaster.serviceInfo
        // Check availability of rendering rule infos
        val renderingRuleInfos = serviceInfo?.renderingRuleInfos ?: emptyList()
        if (renderingRuleInfos.isEmpty()) {
            messageDialogVM.showMessageDialog("No rendering rules found for this image service.")
            return
        }
        val rasterLayers = mutableListOf<RasterLayer>()
        val ruleNames = mutableListOf<String>()
        // 'None' option: no rendering rule
        val baseRaster = ImageServiceRaster(IMAGE_SERVICE_URL)
        val baseLayer = RasterLayer(baseRaster).apply { name = "None" }
        rasterLayers.add(baseLayer)
        // For each rendering rule info, create a new raster layer
        for (ruleInfo in renderingRuleInfos) {
            val raster = ImageServiceRaster(IMAGE_SERVICE_URL)
            raster.renderingRule = RenderingRule(ruleInfo)
            val rasterLayer = RasterLayer(raster).apply { name = ruleInfo.name }
            rasterLayers.add(rasterLayer)
            ruleNames.add(ruleInfo.name)
        }
        // Load all raster layers
        rasterLayers.forEach { layer ->
            layer.load().onFailure {
                messageDialogVM.showMessageDialog(
                    title = "Failed to load raster layer",
                    description = it.message.toString()
                )
            }
        }
        _rasterLayers.value = rasterLayers
        _renderingRuleNames.value = ruleNames
        // Set initial selection to 'None'
        _selectedRenderingRuleName.value = "None"
        // Set the initial layer on the map
        setRasterLayer("None")
    }

    /**
     * Sets the raster layer with the given name as the only operational layer on the map.
     * Zooms to the layer's extent if available.
     */
    fun setRasterLayer(ruleName: String) {
        val rasterLayer = _rasterLayers.value.firstOrNull { it.name == ruleName }
        if (rasterLayer != null) {
            arcGISMap.operationalLayers.clear()
            arcGISMap.operationalLayers.add(rasterLayer)
            _selectedRenderingRuleName.value = ruleName
            // Zoom to the raster layer's extent if available
            viewModelScope.launch {
                rasterLayer.fullExtent?.let { extent ->
                    mapViewProxy.setViewpoint(Viewpoint(extent))
                }
            }
        }
    }
}
