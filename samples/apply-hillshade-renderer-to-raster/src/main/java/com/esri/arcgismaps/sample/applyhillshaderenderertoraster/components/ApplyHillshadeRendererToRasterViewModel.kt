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

package com.esri.arcgismaps.sample.applyhillshaderenderertoraster.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.mapping.symbology.raster.HillshadeRenderer
import com.arcgismaps.raster.Raster
import com.arcgismaps.raster.SlopeType
import com.esri.arcgismaps.sample.applyhillshaderenderertoraster.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File

class ApplyHillshadeRendererToRasterViewModel(application: Application) :
    AndroidViewModel(application) {

    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(
            R.string.apply_hillshade_renderer_to_raster_app_name
        )
    }

    // Blank map to display raster layer
    val arcGISMap = ArcGISMap()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // Create raster
    private val raster by lazy {
        Raster.createWithPath(path = "$provisionPath/srtm-hillshade/srtm.tiff")
    }

    // Create raster layer
    private val rasterLayer by lazy {
        RasterLayer(raster)
    }

    fun initialize() {
        viewModelScope.launch {
            // Load raster layer
            rasterLayer.load().getOrElse { messageDialogVM.showMessageDialog(it.toString()) }
            // Add raster layer to a blank map
            arcGISMap.operationalLayers.add(rasterLayer)
            // Apply the renderer values
            updateRenderer(altitude = 45.0, azimuth = 0.0, slopeType = SlopeType.None)
        }
    }

    /**
     * Updates the current raster layer with a new [HillshadeRenderer] using the provided parameters.
     */
    fun updateRenderer(altitude: Double, azimuth: Double, slopeType: SlopeType) {
        // Create blend renderer
        val hillshadeRenderer = HillshadeRenderer.create(
            altitude = altitude,
            azimuth = azimuth,
            slopeType = slopeType,
            zFactor = 0.000016,
            pixelSizeFactor = 1.0,
            pixelSizePower = 1.0,
            outputBitDepth = 8
        )
        rasterLayer.renderer = hillshadeRenderer
    }
}
