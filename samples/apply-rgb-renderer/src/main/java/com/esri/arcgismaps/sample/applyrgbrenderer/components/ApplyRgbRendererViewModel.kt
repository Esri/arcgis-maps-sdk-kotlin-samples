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

package com.esri.arcgismaps.sample.applyrgbrenderer.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.mapping.symbology.raster.HistogramEqualizationStretchParameters
import com.arcgismaps.mapping.symbology.raster.MinMaxStretchParameters
import com.arcgismaps.mapping.symbology.raster.PercentClipStretchParameters
import com.arcgismaps.mapping.symbology.raster.StandardDeviationStretchParameters
import com.arcgismaps.mapping.symbology.raster.RgbRenderer
import com.arcgismaps.raster.Raster
import com.esri.arcgismaps.sample.applyrgbrenderer.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for the Apply RGB Renderer sample.
 *
 * This ViewModel attempts to load a local multispectral raster (Shasta.tif) from the
 * app's external files directory under a folder named "ApplyRgbRenderer". If the raster
 * file is found it will be added as a basemap using a RasterLayer. If not found the
 * ViewModel will keep a default basemap and show a message explaining how to provide
 * the raster for the full sample experience.
 *
 * The ViewModel exposes properties to control the different stretch parameter types
 * used to create an RGBRenderer that can be applied to the raster layer.
 */
class ApplyRgbRendererViewModel(private val app: Application) : AndroidViewModel(app) {

    // The ArcGISMap that the UI consumes. Start with a simple basemap while the raster is loaded.
    var arcGISMap by mutableStateOf(ArcGISMap(BasemapStyle.ArcGISTopographic))
        private set

    // Simple viewpoint so the map doesn't start zoomed out to world by default
    private val defaultViewpoint = Viewpoint(latitude = 40.0, longitude = -122.5, scale = 5e6)

    // Message dialog view model for error/info messages
    val messageDialogVM = MessageDialogViewModel()

    // Provision path under external files for the sample raster. Place the Shasta.tif file here if you want the
    // sample to load a local raster: <external-files>/ApplyRgbRenderer/Shasta.tif
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() + File.separator + app.getString(R.string.apply_rgb_renderer_app_name) + File.separator + "raster-file"
    }

    // Raster file expected by the sample
    private val rasterFile: File by lazy { File(provisionPath, "Shasta.tif") }

    // Create raster
    private val raster = Raster.createWithPath(path = "$provisionPath/Shasta.tif")

    // Create raster layer
    private val rasterLayer = RasterLayer(raster)

    // UI state for renderer settings
    enum class StretchType { HistogramEqualization, MinMax, PercentClip, StandardDeviation }

    var selectedStretchType by mutableStateOf(StretchType.HistogramEqualization)
        private set

    // Min/Max colors (used with MinMax stretch)
    var minColorIndex by mutableStateOf(0)
        private set
    var maxColorIndex by mutableStateOf(1)
        private set

    // Preset color choices for the sample (ArcGIS Color objects)
    val presetColors: List<Pair<String, Color>> = listOf(
        "Black" to Color.black,
        "White" to Color.white,
        "Green" to Color.green,
        "Red" to Color.red,
        "Blue" to Color.cyan, // cyan used as blue-ish; SDK Color.blue companion may not exist
        "Transparent" to Color.transparent
    )

    // PercentClip UI values
    var percentClipMin by mutableStateOf(10.0)
        private set
    var percentClipMax by mutableStateOf(90.0)
        private set

    // Standard deviation factor
    var standardDeviationFactor by mutableStateOf(0.5)
        private set

    init {
        // Create a simple map initially with a viewpoint
        arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
            initialViewpoint = defaultViewpoint
        }

        // Attempt to load the raster if available. This keeps init lightweight and uses coroutine for IO.
        viewModelScope.launch {
            try {
                // Create a basemap from the raster layer and apply to the map
                arcGISMap = ArcGISMap(Basemap(baseLayer = rasterLayer))
                // Load the map and raster layer, show errors via message dialog
                arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
                rasterLayer.load().onFailure { messageDialogVM.showMessageDialog(it) }
            } catch (ex: Exception) {
                messageDialogVM.showMessageDialog(
                    ex
                )
                // fall back to a default map
                arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
                    initialViewpoint = defaultViewpoint
                }
            }
        }
    }

    // Update UI controls
    fun updateStretchType(type: StretchType) {
        selectedStretchType = type
    }

    fun updateMinColorIndex(index: Int) {
        minColorIndex = index.coerceIn(0, presetColors.lastIndex)
    }

    fun updateMaxColorIndex(index: Int) {
        maxColorIndex = index.coerceIn(0, presetColors.lastIndex)
    }

    fun updatePercentClip(min: Double, max: Double) {
        percentClipMin = min.coerceIn(0.0, 100.0)
        percentClipMax = max.coerceIn(0.0, 100.0)
        if (percentClipMin > percentClipMax) {
            val tmp = percentClipMin
            percentClipMin = percentClipMax
            percentClipMax = tmp
        }
    }

    fun updateStandardDeviationFactor(factor: Double) {
        standardDeviationFactor = factor.coerceIn(0.0, 16.0)
    }

    /**
     * Applies an RGB renderer to the raster layer using the selected stretch parameters.
     * If a raster layer is not available the function will show a message explaining
     * that a local raster is required.
     */
    fun applyRgbRenderer() {
        val layer = rasterLayer
        if (layer == null) {
            messageDialogVM.showMessageDialog(
                title = "No raster layer",
                description = "An RGB renderer requires a raster layer. Place 'Shasta.tif' in: $provisionPath and restart the sample."
            )
            return
        }

        viewModelScope.launch {
            try {
                // Build stretch parameters depending on the selected stretch type
                val stretchParameters = when (selectedStretchType) {
                    StretchType.HistogramEqualization -> HistogramEqualizationStretchParameters()

                    StretchType.MinMax -> {
                        // Convert the chosen colors to three-band numeric arrays.
                        // The Raster SDK expects numeric band endpoints in the same units as the raster's pixel values.
                        // For the purpose of this sample we use simple RGB 0/255 values for min/max which is a common case.
                        val minColor = presetColors[minColorIndex].second
                        val maxColor = presetColors[maxColorIndex].second
                        val minValues = colorToRgbArray(minColor)
                        val maxValues = colorToRgbArray(maxColor)
                        MinMaxStretchParameters(minValues = minValues, maxValues = maxValues)
                    }

                    StretchType.PercentClip -> PercentClipStretchParameters(min = percentClipMin, max = percentClipMax)

                    StretchType.StandardDeviation -> StandardDeviationStretchParameters(factor = standardDeviationFactor)
                }

                // Create an RGB renderer with the chosen stretch parameters. Use default band indexes and gammas so
                // the renderer will use the raster's bands for R,G,B in order. EstimateStatistics true helps produce
                // a pleasing stretch when the raster has not had statistics computed.
                val rgbRenderer = RgbRenderer(stretchParameters = stretchParameters, bandIndexes = listOf(), gammas = listOf(), estimateStatistics = true)

                layer.renderer = rgbRenderer
            } catch (ex: Exception) {
                messageDialogVM.showMessageDialog("Failed to apply RGB renderer", ex.message ?: "Unknown error")
            }
        }
    }

    // Helper: convert an ArcGIS Color to a list of three Double RGB values [R, G, B] in 0..255 range
    private fun colorToRgbArray(color: Color): List<Double> {
        return when (color) {
            Color.black -> listOf(0.0, 0.0, 0.0)
            Color.white -> listOf(255.0, 255.0, 255.0)
            Color.green -> listOf(0.0, 255.0, 0.0)
            Color.red -> listOf(255.0, 0.0, 0.0)
            Color.cyan -> listOf(0.0, 255.0, 255.0)
            Color.transparent -> listOf(0.0, 0.0, 0.0)
            else -> listOf(0.0, 0.0, 0.0)
        }
    }
}
