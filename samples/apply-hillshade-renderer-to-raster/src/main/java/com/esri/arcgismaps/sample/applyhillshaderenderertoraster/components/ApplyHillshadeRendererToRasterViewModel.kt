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
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.mapping.symbology.raster.HillshadeRenderer
import com.arcgismaps.raster.MosaicDatasetRaster
import com.arcgismaps.raster.SlopeType
import com.esri.arcgismaps.sample.applyhillshaderenderertoraster.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import java.io.File

class ApplyHillshadeRendererToRasterViewModel(application: Application) :
    AndroidViewModel(application) {

    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() +
                File.separator + application.getString(R.string.apply_hillshade_renderer_to_raster_app_name)
    }

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight).apply {
        initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // set default values for the HillshadeRenderer parameter values
    var currentHillshadeRenderer = HillshadeParameterValues(
        altitude = 0.0,
        azimuth = 0.0,
        slopeType = SlopeType.None,
        mAltitude = 45,
        zFactor = 0.000016,
        pixelSizeFactor = 1.0,
        pixelSizePower = 1.0,
        outputBitDepth = 8,
    )

    init {
        // Create raster
        val raster = MosaicDatasetRaster(
            databasePath = "$provisionPath/srtm-hillshade/srtm.tiff",
            name = "Hillshade"
        )

        // Create raster layer
        val rasterLayer = RasterLayer(raster)

        arcGISMap.operationalLayers.add(rasterLayer)

        updateRenderer(rasterLayer)
    }

    private fun updateRenderer(rasterLayer: RasterLayer) {
        // Create blend renderer
        val hillshadeRenderer = HillshadeRenderer.create(
            altitude = currentHillshadeRenderer.altitude,
            azimuth = currentHillshadeRenderer.azimuth,
            zFactor = currentHillshadeRenderer.zFactor,
            slopeType = SlopeType.None,
            pixelSizeFactor = currentHillshadeRenderer.pixelSizeFactor,
            pixelSizePower = currentHillshadeRenderer.pixelSizePower
        )

        rasterLayer.renderer = hillshadeRenderer
    }
}

data class HillshadeParameterValues(
    val altitude: Double,
    val azimuth: Double,
    val slopeType: SlopeType,
    val outputBitDepth: Int,
    val pixelSizePower: Double,
    val pixelSizeFactor: Double,
    val zFactor: Double,
    val mAltitude: Int
)