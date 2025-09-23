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

package com.esri.arcgismaps.sample.applyblendrenderertohillshade.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.mapping.symbology.raster.BlendRenderer
import com.arcgismaps.mapping.symbology.raster.ColorRamp
import com.arcgismaps.mapping.symbology.raster.PresetColorRampType
import com.arcgismaps.raster.Raster
import com.arcgismaps.raster.SlopeType
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for the "Apply blend renderer to hillshade" sample.
 *
 * This ViewModel builds two raster basemaps (imagery and elevation) and applies
 * a BlendRenderer to give a hillshade blended appearance. The sample exposes
 * a few renderer parameters (altitude, azimuth, slope type and a color ramp
 * preset) and updates the renderer when these change.
 */
class ApplyBlendRendererToHillshadeViewModel(private val app: Application) : AndroidViewModel(app) {

    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() + File.separator +
                app.getString(com.esri.arcgismaps.sample.applyblendrenderertohillshade.R.string.apply_blend_renderer_to_hillshade_app_name)
    }

    // Raster containing imagery (Shasta).
    private val imageryRaster: Raster by lazy {
        Raster.createWithPath("$provisionPath${File.separator}raster-file${File.separator}Shasta.tif")
    }

    // Raster layer for imagery.
    private val imageryRasterLayer: RasterLayer by lazy { RasterLayer(imageryRaster) }

    // Raster containing elevation values (grayscale elevation)
    private val elevationRaster: Raster by lazy {
        Raster.createWithPath("$provisionPath${File.separator}Shasta_Elevation.tif")
    }

    // Raster layer for elevation (used as a basemap when applying a color ramp).
    private val elevationRasterLayer: RasterLayer by lazy { RasterLayer(elevationRaster) }

    // A Basemap for the imagery raster layer.
    private val imageryBasemap: Basemap by lazy {
        Basemap(baseLayer = imageryRasterLayer)
    }

    // A Basemap for the elevation raster layer.
    private val elevationBasemap: Basemap by lazy {
        Basemap(baseLayer = elevationRasterLayer)
    }

    // The ArcGISMap exposed to the UI. The basemap will be
    // set to the imagery basemap once the raster layers are loaded
    // and may be switched to the elevation basemap when a color ramp is applied through the UI.
    val arcGISMap = ArcGISMap()

    // MapViewProxy provided to the MapView composable.
    val mapViewProxy = MapViewProxy()

    // Message dialog view model to report errors to the UI
    val messageDialogVM = MessageDialogViewModel()

    // Renderer parameters which can be modified from the UI
    var altitude by mutableDoubleStateOf(45.0)
        private set

    var azimuth by mutableDoubleStateOf(0.0)
        private set

    var slopeType by mutableStateOf<SlopeType>(SlopeType.None)
        private set

    // Color ramp presets exposed as user-friendly strings. The underlying ColorRamp is created
    // on demand in updateRenderer() when a preset is selected.
    val colorRampPresets = listOf("None", "DEM Light", "Screen Display", "Elevation")

    // Index of the selected color ramp preset; 0 means "None" (no color ramp).
    var selectedColorRampPresetIndex by mutableIntStateOf(0)
        private set

    init {
        // Load the raster layers and load and set up the map. Perform long-running loads in a coroutine
        // and surface any errors through the MessageDialogViewModel.
        viewModelScope.launch {
            try {
                // Load both raster layers. If any fails, report the error.
                elevationBasemap.load().onFailure { throw it }
                imageryBasemap.load().onFailure { throw it }
                updateRenderer()
                arcGISMap.load().onFailure { throw it }
            } catch (ex: Throwable) {
                messageDialogVM.showMessageDialog(ex)
            }
        }
    }

    /**
     * Constructs and applies a BlendRenderer
     * using the current UI parameters. Also switches the map basemap to the elevation
     * basemap when a color ramp is active so the color ramp is visible.
     */
    private fun updateRenderer() {
        val colorRamp = createColorRampFromPresetIndex(selectedColorRampPresetIndex)
        val renderer = BlendRenderer(
            elevationRaster = elevationRaster,
            outputMinValues = listOf(9.0),
            outputMaxValues = listOf(255.0),
            sourceMinValues = emptyList(),
            sourceMaxValues = emptyList(),
            noDataValues = emptyList(),
            gammas = emptyList(),
            colorRamp = colorRamp,
            altitude = altitude,
            azimuth = azimuth,
            slopeType = slopeType
        )

        if (colorRamp != null) {
            // When a color ramp is applied, set the basemap to the elevation raster
            elevationRasterLayer.renderer = renderer
            arcGISMap.setBasemap(elevationBasemap)
        } else {
            // Since no ColorRamp is selected, apply BlendRenderer to imagery raster layer.
            imageryRasterLayer.renderer = renderer
            arcGISMap.setBasemap(imageryBasemap)
        }
    }

    /**
     * Update the altitude value and re-apply the renderer.
     */
    fun updateAltitude(newAltitude: Double) {
        altitude = newAltitude
        updateRenderer()
    }

    /**
     * Update the azimuth value and re-apply the renderer.
     */
    fun updateAzimuth(newAzimuth: Double) {
        azimuth = newAzimuth
        updateRenderer()
    }

    /**
     * Update the slope type for the hillshade renderer and re-apply.
     */
    fun updateSlopeType(newSlopeType: SlopeType) {
        slopeType = newSlopeType
        updateRenderer()
    }

    /**
     * Update the selected color ramp preset index and re-apply the renderer.
     */
    fun updateColorRampPresetIndex(index: Int) {
        selectedColorRampPresetIndex = index.coerceIn(0, colorRampPresets.lastIndex)
        updateRenderer()
    }

    /**
     * Helper that creates a Kotlin ColorRamp from the selected preset index.
     * If the preset is "None" this returns null.
     */
    private fun createColorRampFromPresetIndex(index: Int): ColorRamp? {
        return when (index) {
            1 -> ColorRamp.create(type = PresetColorRampType.DemLight, size = 256)
            2 -> ColorRamp.create(type = PresetColorRampType.DemScreen, size = 256)
            3 -> ColorRamp.create(type = PresetColorRampType.Elevation, size = 256)
            else -> null
        }
    }

    companion object {
        // Small helper list for exposing slope types with a user-friendly label in the UI.
        val slopeTypeOptions: List<Pair<String, SlopeType>> = listOf(
            "None" to SlopeType.None,
            "Degree" to SlopeType.Degree,
            "Percent Rise" to SlopeType.PercentRise,
            "Scaled" to SlopeType.Scaled
        )
    }
}
