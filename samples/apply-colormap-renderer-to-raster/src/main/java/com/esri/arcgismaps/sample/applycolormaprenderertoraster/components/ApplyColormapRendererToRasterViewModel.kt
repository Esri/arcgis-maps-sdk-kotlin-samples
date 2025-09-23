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

package com.esri.arcgismaps.sample.applycolormaprenderertoraster.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.mapping.symbology.raster.ColormapRenderer
import com.arcgismaps.raster.Raster
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel that creates a map with a raster layer and applies a colormap renderer to the raster.
 *
 * The sample expects the raster file to be placed in the application's external files directory
 * under a folder matching the sample name and a subfolder "raster-file" containing "Shasta.tif".
 * This mirrors the behavior of other samples that provision offline assets. If the raster is not
 * present the ViewModel will attempt to load and will surface an error through [messageDialogVM].
 */
class ApplyColormapRendererToRasterViewModel(private val app: Application) : AndroidViewModel(app) {

    // Provision path where sample data is expected if provided by the downloader activity
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() + File.separator +
            app.getString(com.esri.arcgismaps.sample.applycolormaprenderertoraster.R.string.apply_colormap_renderer_to_raster_app_name)
    }

    // Raster created from a local file.
    private val raster: Raster by lazy {
        Raster.createWithPath(provisionPath + File.separator + "ShastaBW.tif")
    }

    // Raster layer that will display the raster.
    private val rasterLayer: RasterLayer by lazy {
        // Create a simple colormap: 150 red entries followed by 151 yellow entries
        val colors = mutableListOf<Color>().apply {
            repeat(150) { add(Color.red) }
            repeat(151) { add(Color.yellow) }
        }

        // Create and assign the ColormapRenderer
        val colormapRenderer = ColormapRenderer(colors = colors)
        RasterLayer(raster).apply {
            renderer = colormapRenderer
        }
    }

    // The ArcGISMap used by the sample.
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISImageryStandard).apply {
        operationalLayers += rasterLayer
    }

    // Message dialog view model for error handling
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            rasterLayer.load().onSuccess {
                // When the raster layer is loaded, center the map on the raster's full extent
                rasterLayer.fullExtent?.center?.let { centerPoint ->
                    arcGISMap.initialViewpoint = Viewpoint(center = centerPoint, scale = 80_000.0)
                }
            }.onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }
}
