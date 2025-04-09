/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.addrasterfromfile.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.raster.Raster
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.addrasterfromfile.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File

class AddRasterFromFileViewModel(application: Application) : AndroidViewModel(application) {

    private val provisionPath: String by lazy { application.getExternalFilesDir(null)?.path.toString() +
            File.separator +
            application.getString(R.string.add_raster_from_file_app_name)
    }

    val mapViewProxy = MapViewProxy()

    // create a raster
    private val raster = Raster.createWithPath(provisionPath +
            File.separator + "raster-file" + File.separator + "Shasta.tif")

    // create a raster layer
    private val rasterLayer = RasterLayer(raster)

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISImagery).apply {
        operationalLayers.add(rasterLayer)
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map",
                    error.message.toString()
                )
            }
            rasterLayer.load().onSuccess {
                // Set the viewpoint to the raster layer's extent
                val extent = rasterLayer.fullExtent
                if (extent != null) {
                    mapViewProxy.setViewpointGeometry(extent, paddingInDips = 20.0)
                }
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load raster layer",
                    error.message.toString()
                )
            }
        }
    }
}
