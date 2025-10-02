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
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
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

private const val DEFAULT_MIN = 10.0
private const val DEFAULT_MAX = 150.0
private const val DEFAULT_PERCENT_MIN = 0.0
private const val DEFAULT_PERCENT_MAX = 50.0
private const val DEFAULT_STD_DEVIATION_FACTOR = 0.5

/**
 * ViewModel for the "Apply stretch renderer" sample.
 */
class ApplyStretchRendererViewModel(private val app: Application) : AndroidViewModel(app) {
    private var appliedMinValue = DEFAULT_MIN
    private var appliedMaxValue = DEFAULT_MAX
    private var appliedPercentMin = DEFAULT_PERCENT_MIN
    private var appliedPercentMax = DEFAULT_PERCENT_MAX
    private var appliedStdDeviationFactor = DEFAULT_STD_DEVIATION_FACTOR
    private var appliedStretchType = StretchType.MinMax

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

    // The raster data (Shasta.tif) should be downloaded to external storage on launch
    private val raster: Raster by lazy {
        val rasterFile = File(provisionPath, "raster-file${File.separator}Shasta.tif")
        require(rasterFile.exists()) { "Invalid raster data: file does not exist at ${rasterFile.path}" }
        Raster.createWithPath(rasterFile.path)
    }

    // The raster layer to which the stretch renderer will be applied
    private val rasterLayer: RasterLayer = RasterLayer(raster)

    // Stretch type options for UI
    val stretchTypeOptions: List<String> = listOf("MinMax", "Percent Clip", "Std Deviation")

    // Current selected stretch type
    private val _selectedStretchType = MutableStateFlow(appliedStretchType)
    val selectedStretchType = _selectedStretchType.asStateFlow()

    // Min-Max parameters (values represent pixel value range)
    private val _minValue = MutableStateFlow(DEFAULT_MIN)
    val minValue = _minValue.asStateFlow()

    private val _maxValue = MutableStateFlow(DEFAULT_MAX)
    val maxValue = _maxValue.asStateFlow()

    // Percent clip parameters (values represent percent 0..100)
    private val _percentMin = MutableStateFlow(DEFAULT_PERCENT_MIN)
    val percentMin = _percentMin.asStateFlow()

    private val _percentMax = MutableStateFlow(DEFAULT_PERCENT_MAX)
    val percentMax = _percentMax.asStateFlow()

    // Standard deviation factor (typical range 0.25..4.0)
    private val _stdDeviationFactor = MutableStateFlow(DEFAULT_STD_DEVIATION_FACTOR)
    val stdDeviationFactor = _stdDeviationFactor.asStateFlow()

    init {
        viewModelScope.launch {
            rasterLayer.load().onSuccess {
                arcGISMap.operationalLayers.add(rasterLayer)
            }.onFailure {
                messageDialogVM.showMessageDialog(it)
            }

            arcGISMap.load().onFailure {
                messageDialogVM.showMessageDialog(it)
            }

            val center = rasterLayer.fullExtent?.center ?: Point(0.0, 0.0, SpatialReference.wgs84())
            mapViewProxy.setViewpoint(Viewpoint(center = center, scale = 80_000.0))
            updateRenderer()
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
        updateRenderer()
    }

    /** Update MinMax minimum value (clamped to 0..(max-1)). */
    fun updateMinValue(value: Double) {
        val max = _maxValue.value
        _minValue.value = value.coerceIn(DEFAULT_MIN, max - 1.0)
        updateRenderer()
    }

    /** Update MinMax maximum value (clamped to (min+1)..255). */
    fun updateMaxValue(value: Double) {
        val min = _minValue.value
        _maxValue.value = value.coerceIn(min + 1.0, 255.0)
        updateRenderer()
    }

    /** Update Percent Clip minimum percent (clamped to 0..percentMax). */
    fun updatePercentMin(value: Double) {
        val max = _percentMax.value
        _percentMin.value = value.coerceIn(DEFAULT_MIN, max)
        updateRenderer()
    }

    /** Update Percent Clip maximum percent (clamped to percentMin..100). */
    fun updatePercentMax(value: Double) {
        val min = _percentMin.value
        _percentMax.value = value.coerceIn(min, 100.0)
        updateRenderer()
    }

    /** Update Standard Deviation factor (clamped to 0.25..4.0). */
    fun updateStdDeviationFactor(value: Double) {
        _stdDeviationFactor.value = value.coerceIn(0.25, 4.0)
        updateRenderer()
    }

    /** Construct and apply a StretchRenderer to the raster layer using current UI parameters. */
    private fun updateRenderer() {
        appliedStretchType = _selectedStretchType.value
        val parameters: StretchParameters = when (_selectedStretchType.value) {
            StretchType.MinMax -> {
                // save the new initial values
                appliedMaxValue = _maxValue.value
                appliedMinValue = _minValue.value

                // apply the values to the renderer
                MinMaxStretchParameters(
                    minValues = listOf(_minValue.value),
                    maxValues = listOf(_maxValue.value)
                )
            }

            StretchType.PercentClip -> {
                // save the new initial values
                appliedPercentMin = _percentMin.value
                appliedPercentMax = _percentMax.value

                // apply the values to the renderer
                PercentClipStretchParameters(
                    min = _percentMin.value,
                    max = _percentMax.value
                )
            }

            StretchType.StandardDeviation -> {
                // save the new initial value
                appliedStdDeviationFactor = _stdDeviationFactor.value

                // apply the value to the renderer
                StandardDeviationStretchParameters(
                    factor = _stdDeviationFactor.value
                )
            }
        }
        rasterLayer.renderer = StretchRenderer(
            parameters = parameters,
            gammas = emptyList(),
            estimateStatistics = true,
            colorRamp = null
        )
    }

    /** Dismiss any unapplied changes and reset UI to last applied values. */
    fun dismissChanges() {
        _selectedStretchType.value = appliedStretchType
        _minValue.value = appliedMinValue
        _maxValue.value = appliedMaxValue

        _percentMin.value = appliedPercentMin
        _percentMax.value = appliedPercentMax

        _stdDeviationFactor.value = appliedStdDeviationFactor
    }

    /** Reset MinMax parameters to their initial default values. */
    private fun resetToInitialMinMaxValues() {
        appliedMinValue = DEFAULT_MIN
        appliedMaxValue = DEFAULT_MAX
        _minValue.value = DEFAULT_MIN
        _maxValue.value = DEFAULT_MAX
    }

    /** Reset Percent Clip parameters to their initial default values. */
    private fun resetToInitialPercentClipValues() {
        appliedPercentMin = DEFAULT_PERCENT_MIN
        appliedPercentMax = DEFAULT_PERCENT_MAX
        _percentMin.value = DEFAULT_PERCENT_MIN
        _percentMax.value = DEFAULT_PERCENT_MAX
    }

    /** Reset Standard Deviation factor to its initial default value. */
    private fun resetToInitialStdDeviationValue() {
        appliedStdDeviationFactor = DEFAULT_STD_DEVIATION_FACTOR
        _stdDeviationFactor.value = DEFAULT_STD_DEVIATION_FACTOR
    }

    /** Reset all parameters to their initial default values. */
    fun resetAllChanges() {
        _selectedStretchType.value = StretchType.MinMax
        resetToInitialMinMaxValues()
        resetToInitialPercentClipValues()
        resetToInitialStdDeviationValue()
        updateRenderer()
    }
}

/** Enum representing available stretch parameter types. */
enum class StretchType { MinMax, PercentClip, StandardDeviation; }
