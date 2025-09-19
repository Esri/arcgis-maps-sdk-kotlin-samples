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

package com.esri.arcgismaps.sample.addrasterfromservice.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.LoadStatus
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.raster.ImageServiceRaster
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Add raster from service sample.
 */
class AddRasterFromServiceViewModel(application: Application) : AndroidViewModel(application) {

    // Map used by the Compose MapView
    var arcGISMap = ArcGISMap(BasemapStyle.ArcGISDarkGray).apply {
            initialViewpoint = Viewpoint(
                Point(-13637000.0, 4550000.0, SpatialReference.webMercator()),
                100000.0
            )
        }

    // MapViewProxy allows the ViewModel to request viewpoint changes on the MapView
    val mapViewProxy = MapViewProxy()

    // The image service raster (NOAA bathymetry service)
    private val imageServiceRaster = ImageServiceRaster(
        url = "https://gis.ngdc.noaa.gov/arcgis/rest/services/bag_bathymetry/ImageServer"
    )

    // Raster layer that will display the image service raster
    private val rasterLayer = RasterLayer(imageServiceRaster)

    // Flow exposing the current load status of the raster layer for the UI to observe
    private val _rasterLoadStatus = MutableStateFlow<LoadStatus>(LoadStatus.NotLoaded)
    val rasterLoadStatus = _rasterLoadStatus.asStateFlow()

    // Message dialog view model for error reporting
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Add the raster layer to the map's operational layers
        arcGISMap.operationalLayers.add(rasterLayer)

        // Start loading the raster layer and observe its load status
        viewModelScope.launch {
            // Attempt to load the raster layer and report any immediate failures
            rasterLayer.load().onFailure { throwable ->
                messageDialogVM.showMessageDialog(
                    title = "Failed to load raster layer",
                    description = throwable.message.toString()
                )
            }.onSuccess {
                _rasterLoadStatus.value = LoadStatus.Loaded
            }
        }
    }
}
