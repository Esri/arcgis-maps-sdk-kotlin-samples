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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.mapping.symbology.raster.HillshadeRenderer
import com.arcgismaps.raster.Raster
import com.arcgismaps.raster.SlopeType
import com.esri.arcgismaps.sample.applyhillshaderenderertoraster.R
import java.io.File

class ApplyHillshadeRendererToRasterViewModel(application: Application) :
    AndroidViewModel(application) {

    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(
            R.string.apply_hillshade_renderer_to_raster_app_name
        )
    }

    // Create raster
    private val raster = Raster.createWithPath(path = "$provisionPath/srtm-hillshade/srtm.tiff")

    // Create raster layer
    private val rasterLayer = RasterLayer(raster)

    // Blank map to display raster layer
    val arcGISMap = ArcGISMap(Basemap(rasterLayer))

    // Track UI values to be applied by the update renderer
    var currentAltitude by mutableDoubleStateOf(45.0)
    var currentAzimuth by mutableDoubleStateOf(0.0)
    var currentSlopeType by mutableStateOf<SlopeType>(SlopeType.None)


    init {
        // Apply the renderer values
        updateRenderer(
            altitude = currentAltitude,
            azimuth = currentAzimuth,
            slopeType = currentSlopeType
        )
    }

    /**
     * Updates the current raster layer with a new [HillshadeRenderer] using the provided parameters.
     */
    fun updateRenderer(
        altitude: Double,
        azimuth: Double,
        slopeType: SlopeType
    ) {
        // Create blend renderer
        val hillshadeRenderer = HillshadeRenderer.create(
            altitude = altitude,
            azimuth = azimuth,
            zFactor = Z_FACTOR,
            slopeType = slopeType,
            pixelSizeFactor = PIXEL_SIZE_FACTOR,
            pixelSizePower = PIXEL_SIZE_POWER,
            outputBitDepth = OUTPUT_BIT_DEPTH
        )
        rasterLayer.renderer = hillshadeRenderer

        currentAzimuth = azimuth
        currentAltitude = altitude
        currentSlopeType = slopeType
    }

    companion object {
        // Adjusts the vertical scaling of the terrain to ensure accurate representation of elevation changes
        const val Z_FACTOR: Double = 0.000016
        const val PIXEL_SIZE_FACTOR: Double = 1.0
        const val PIXEL_SIZE_POWER: Double = 1.0
        const val OUTPUT_BIT_DEPTH: Int = 8
    }
}
