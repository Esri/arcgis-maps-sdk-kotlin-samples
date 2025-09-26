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

package com.esri.arcgismaps.sample.applystretchrenderer.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.mapping.symbology.raster.MinMaxStretchParameters
import com.arcgismaps.mapping.symbology.raster.PercentClipStretchParameters
import com.arcgismaps.mapping.symbology.raster.StandardDeviationStretchParameters
import com.arcgismaps.mapping.symbology.raster.StretchParameters
import com.arcgismaps.mapping.symbology.raster.StretchRenderer
import com.arcgismaps.raster.Raster
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.applystretchrenderer.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for the "Apply stretch renderer" sample.
 */
class ApplyStretchRendererViewModel(private val app: Application) : AndroidViewModel(app) {

    // The map used in the sample, with imagery basemap
    val arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISImageryStandard)

    // MapViewProxy used to set viewpoint after layer loads
    val mapViewProxy = MapViewProxy()

    // Message dialog view model for error handling
    val messageDialogVM = MessageDialogViewModel()

    // Provision path for local offline resources (raster-file/Shasta.tif)
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() + File.separator +
                app.getString(R.string.apply_stretch_renderer_app_name)
    }

    // The raster layer to which the stretch renderer will be applied
    private var rasterLayer: RasterLayer? = null

    // Stretch type options for UI
    val stretchTypeOptions: List<String> = listOf("MinMax", "Percent Clip", "Std Deviation")

    // Current selected stretch type
    private val _selectedStretchType = MutableStateFlow(StretchType.MinMax)
    val selectedStretchType = _selectedStretchType.asStateFlow()

    // Min-Max parameters (values represent pixel value range)
    private val _minValue = MutableStateFlow(10.0)
    val minValue = _minValue.asStateFlow()

    private val _maxValue = MutableStateFlow(150.0)
    val maxValue = _maxValue.asStateFlow()

    // Percent clip parameters (values represent percent 0..100)
    private val _percentMin = MutableStateFlow(0.0)
    val percentMin = _percentMin.asStateFlow()

    private val _percentMax = MutableStateFlow(50.0)
    val percentMax = _percentMax.asStateFlow()

    // Standard deviation factor (typical range 0.25..4.0)
    private val _stdDeviationFactor = MutableStateFlow(0.5)
    val stdDeviationFactor = _stdDeviationFactor.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // Load the raster layer and apply an initial renderer
                val raster = Raster.createWithPath(File(provisionPath, "raster-file${File.separator}Shasta.tif").path)
                rasterLayer = RasterLayer(raster)
                rasterLayer?.load()?.getOrThrow()

                arcGISMap.apply {
                    rasterLayer?.let {
                        operationalLayers.add(it)
                    }
                }.load().getOrThrow()

                val center = rasterLayer?.fullExtent?.center ?: throw Exception("Failed to get raster center")
                mapViewProxy.setViewpoint(Viewpoint(center = center, scale = 80_000.0))
                updateRenderer()
            } catch (ex: Exception) {
                messageDialogVM.showMessageDialog(ex)
            }
        }
    }

    /** Update the current stretch type by index from [stretchTypeOptions]. */
    fun updateStretchTypeByIndex(index: Int) {
        val safeIndex = index.coerceIn(0, stretchTypeOptions.lastIndex)
        _selectedStretchType.value = when (safeIndex) {
            0 -> StretchType.MinMax
            1 -> StretchType.PercentClip
            else -> StretchType.StandardDeviation
        }
    }

    /** Update MinMax minimum value (clamped to 0..(max-1)). */
    fun updateMinValue(value: Double) {
        val max = _maxValue.value
        _minValue.value = value.coerceIn(0.0, max - 1.0)
    }

    /** Update MinMax maximum value (clamped to (min+1)..255). */
    fun updateMaxValue(value: Double) {
        val min = _minValue.value
        _maxValue.value = value.coerceIn(min + 1.0, 255.0)
    }

    /** Update Percent Clip minimum percent (clamped to 0..percentMax). */
    fun updatePercentMin(value: Double) {
        val max = _percentMax.value
        _percentMin.value = value.coerceIn(0.0, max)
    }

    /** Update Percent Clip maximum percent (clamped to percentMin..100). */
    fun updatePercentMax(value: Double) {
        val min = _percentMin.value
        _percentMax.value = value.coerceIn(min, 100.0)
    }

    /** Update Standard Deviation factor (clamped to 0.25..4.0). */
    fun updateStdDeviationFactor(value: Double) {
        _stdDeviationFactor.value = value.coerceIn(0.25, 4.0)
    }

    /** Construct and apply a StretchRenderer to the raster layer using current UI parameters. */
    fun updateRenderer() {
        val parameters: StretchParameters = when (_selectedStretchType.value) {
            StretchType.MinMax -> MinMaxStretchParameters(
                minValues = listOf(_minValue.value),
                maxValues = listOf(_maxValue.value)
            )

            StretchType.PercentClip -> PercentClipStretchParameters(
                min = _percentMin.value,
                max = _percentMax.value
            )

            StretchType.StandardDeviation -> StandardDeviationStretchParameters(
                factor = _stdDeviationFactor.value
            )
        }
        rasterLayer?.renderer = StretchRenderer(
            parameters = parameters,
            gammas = emptyList(),
            estimateStatistics = true,
            colorRamp = null
        )
    }
}

/** Enum representing available stretch parameter types. */
enum class StretchType { MinMax, PercentClip, StandardDeviation; }
