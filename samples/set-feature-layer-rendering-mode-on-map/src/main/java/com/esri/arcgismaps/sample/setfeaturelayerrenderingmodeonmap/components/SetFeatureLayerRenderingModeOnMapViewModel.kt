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

package com.esri.arcgismaps.sample.setfeaturelayerrenderingmodeonmap.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.FeatureRenderingMode
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel for the "Set feature layer rendering mode on map" sample.
 *
 * Creates two ArcGISMap instances: one where feature layers use dynamic rendering,
 * and another that uses static rendering. Exposes MapViewProxy instances for each
 * MapView so the UI can animate the viewpoint using the same target viewpoint.
 */
class SetFeatureLayerRenderingModeOnMapViewModel(app: Application) : AndroidViewModel(app) {

    // Message dialog VM used to report loading errors
    val messageDialogVM = MessageDialogViewModel()

    // Map and proxy for the dynamic-rendered map
    val mapViewProxyDynamic = MapViewProxy()
    val arcGISMapDynamic: ArcGISMap = ArcGISMap()

    // Map and proxy for the static-rendered map
    val mapViewProxyStatic = MapViewProxy()
    val arcGISMapStatic: ArcGISMap = ArcGISMap()

    private val _isZoomedIn = MutableStateFlow(true)
    val isZoomedIn = _isZoomedIn.asStateFlow()

    // Lazy properties for zoomed in and zoomed out viewpoints
    private val zoomedInViewpoint by lazy {
        Viewpoint(
            center = Point(x = -118.45, y = 34.395, spatialReference = SpatialReference.wgs84()),
            scale = 50000.0,
            rotation = 90.0
        )
    }

    private val zoomedOutViewpoint by lazy {
        Viewpoint(
            center = Point(x = -118.37, y = 34.46, spatialReference = SpatialReference.wgs84()),
            scale = 650000.0,
            rotation = 0.0
        )
    }

    init {
        // Define service URLs for point, polyline and polygon feature services
        val pointServiceUrl = "https://sampleserver6.arcgisonline.com/arcgis/rest/services/Energy/Geology/FeatureServer/0"
        val polylineServiceUrl = "https://sampleserver6.arcgisonline.com/arcgis/rest/services/Energy/Geology/FeatureServer/8"
        val polygonServiceUrl = "https://sampleserver6.arcgisonline.com/arcgis/rest/services/Energy/Geology/FeatureServer/9"

        // Create ServiceFeatureTable instances and FeatureLayers for the dynamic map
        val pointTableDynamic = ServiceFeatureTable(uri = pointServiceUrl)
        val polylineTableDynamic = ServiceFeatureTable(uri = polylineServiceUrl)
        val polygonTableDynamic = ServiceFeatureTable(uri = polygonServiceUrl)

        val pointLayerDynamic = FeatureLayer.createWithFeatureTable(pointTableDynamic).apply {
            renderingMode = FeatureRenderingMode.Dynamic
        }
        val polylineLayerDynamic = FeatureLayer.createWithFeatureTable(polylineTableDynamic).apply {
            renderingMode = FeatureRenderingMode.Dynamic
        }
        val polygonLayerDynamic = FeatureLayer.createWithFeatureTable(polygonTableDynamic).apply {
            renderingMode = FeatureRenderingMode.Dynamic
        }

        // Add dynamic layers to the dynamic map
        arcGISMapDynamic.operationalLayers.addAll(
            listOf(pointLayerDynamic, polylineLayerDynamic, polygonLayerDynamic)
        )

        // Create ServiceFeatureTable instances and FeatureLayers for the static map
        val pointTableStatic = ServiceFeatureTable(uri = pointServiceUrl)
        val polylineTableStatic = ServiceFeatureTable(uri = polylineServiceUrl)
        val polygonTableStatic = ServiceFeatureTable(uri = polygonServiceUrl)

        val pointLayerStatic = FeatureLayer.createWithFeatureTable(pointTableStatic).apply {
            renderingMode = FeatureRenderingMode.Static
        }
        val polylineLayerStatic = FeatureLayer.createWithFeatureTable(polylineTableStatic).apply {
            renderingMode = FeatureRenderingMode.Static
        }
        val polygonLayerStatic = FeatureLayer.createWithFeatureTable(polygonTableStatic).apply {
            renderingMode = FeatureRenderingMode.Static
        }

        // Add static layers to the static map
        arcGISMapStatic.operationalLayers.addAll(
            listOf(pointLayerStatic, polylineLayerStatic, polygonLayerStatic)
        )

        // Set a shared initial viewpoint for both maps
        val initialViewpoint = Viewpoint(
            center = Point(x = -118.45, y = 34.395, spatialReference = SpatialReference.wgs84()),
            scale = 50000.0
        )
        arcGISMapDynamic.initialViewpoint = initialViewpoint
        arcGISMapStatic.initialViewpoint = initialViewpoint

        // Load maps and handle any failures
        viewModelScope.launch {
            arcGISMapDynamic.load().onFailure { messageDialogVM.showMessageDialog(it) }
            arcGISMapStatic.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Called by the UI when the zoom button is pressed. Animates both map views to the
     * target viewpoint (zoomed in or zoomed out) using their respective MapViewProxy.
     */
    fun onZoomButtonClicked() {
        val targetViewpoint = if (_isZoomedIn.value) zoomedOutViewpoint else zoomedInViewpoint

        viewModelScope.launch {
            mapViewProxyDynamic.setViewpointAnimated(targetViewpoint, 5.seconds)
        }
        viewModelScope.launch {
            mapViewProxyStatic.setViewpointAnimated(targetViewpoint, 5.seconds)
        }
        _isZoomedIn.value = !_isZoomedIn.value
    }
}
