/* Copyright 2026 Esri
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.mapping.symbology.raster.MinMaxStretchParameters
import com.arcgismaps.mapping.symbology.raster.PercentClipStretchParameters
import com.arcgismaps.mapping.symbology.raster.RgbRenderer
import com.arcgismaps.mapping.symbology.raster.StandardDeviationStretchParameters
import com.arcgismaps.mapping.symbology.raster.StretchParameters
import com.arcgismaps.raster.Raster
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.applyrgbrenderer.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ApplyRgbRendererViewModel(app: Application) : AndroidViewModel(app) {
    // Create a state flow to hold the UI state for the supporting pane controls
    private val _uiState = MutableStateFlow(RgbRendererUiState.defaultState)

    // Expose the state flow as read-only for the UI
    val uiState = _uiState.asStateFlow()

    // Create a MapViewProxy, used to set viewpoint
    val mapViewProxy = MapViewProxy()

    // Provision path for local offline resources
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path + File.separator + app.getString(
            R.string.apply_rgb_renderer_app_name
        )
    }

    // Create a Raster from a multispectral raster file (the raster data file is downloaded to
    // external storage when the app is launched)
    private val raster by lazy {
        val rasterFile = File(provisionPath, "raster-file${File.separator}Shasta.tif")
        Raster.createWithPath(rasterFile.path)
    }

    // Create a RasterLayer from the raster
    private val rasterLayer = RasterLayer(raster)

    // Create a Basemap from the raster layer and set it to the ArcGISMap
    val arcGISMap: ArcGISMap = ArcGISMap(Basemap(rasterLayer))

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            rasterLayer.load().onFailure {
                messageDialogVM.showMessageDialog(it)
            }.onSuccess {
                arcGISMap.load().onFailure {
                    messageDialogVM.showMessageDialog(it)
                }.onSuccess {
                    rasterLayer.fullExtent?.center?.let { center ->
                        mapViewProxy.setViewpoint(Viewpoint(center = center, scale = 80_000.0))
                    }
                    updateRenderer()
                }
            }
        }
    }

    /**
     * Construct and apply an RgbRenderer to the raster layer using current UI parameters.
     */
    private fun updateRenderer() {
        // Construct StretchParameters
        val parameters: StretchParameters = when (uiState.value.stretchType) {
            StretchType.MinMax -> {
                // Use MinMaxStretchParameters
                MinMaxStretchParameters(
                    minValues = uiState.value.minMaxMinValues,
                    maxValues = uiState.value.minMaxMaxValues
                )
            }

            StretchType.PercentClip -> {
                // Use PercentClipStretchParameters
                PercentClipStretchParameters(
                    min = uiState.value.percentClipMinValue,
                    max = uiState.value.percentClipMaxValue
                )
            }

            StretchType.StandardDeviation -> {
                // Use StandardDeviationStretchParameters
                StandardDeviationStretchParameters(
                    factor = uiState.value.stdDevFactor
                )
            }
        }

        // Create an RgbRenderer and set it on the raster layer
        rasterLayer.renderer = RgbRenderer(
            stretchParameters = parameters,
            bandIndexes = emptyList(),
            gammas = emptyList(),
            estimateStatistics = true
        )
    }

    /**
     * Updates the currently selected StretchType.
     */
    fun updateStretchType(stretchType: StretchType) {
        _uiState.update { currentState ->
            currentState.copy(stretchType = stretchType)
        }
        updateRenderer()
    }

    /**
     * Updates the currently selected Min and Max values for the band with the given [index].
     */
    fun onMinMaxValuesChange(index: Int, minValue: Double, maxValue: Double) {
        _uiState.update { currentState ->
            val minValues: MutableList<Double> = mutableListOf()
            minValues.addAll(currentState.minMaxMinValues)
            minValues[index] = minValue
            val maxValues: MutableList<Double> = mutableListOf()
            maxValues.addAll(currentState.minMaxMaxValues)
            maxValues[index] = maxValue
            currentState.copy(minMaxMinValues = minValues, minMaxMaxValues = maxValues)
        }
        updateRenderer()
    }

    /**
     * Updates the currently selected percentClipMinValue.
     */
    fun onPercentClipMinValueChange(percentClipMinValue: Double) {
        _uiState.update { currentState ->
            currentState.copy(percentClipMinValue = percentClipMinValue)
        }
        updateRenderer()
    }

    /**
     * Updates the currently selected percentClipMaxValue.
     */
    fun onPercentClipMaxValueChange(percentClipMaxValue: Double) {
        _uiState.update { currentState ->
            currentState.copy(percentClipMaxValue = percentClipMaxValue)
        }
        updateRenderer()
    }

    /**
     * Updates the currently selected stdDevFactor.
     */
    fun onStdDevFactorChange(stdDevFactor: Double) {
        _uiState.update { currentState ->
            currentState.copy(stdDevFactor = stdDevFactor)
        }
        updateRenderer()
    }

    /**
     * Resets all parameters to their initial state.
     */
    fun resetAllChanges() {
        _uiState.value = RgbRendererUiState.defaultState
        updateRenderer()
    }
}

data class RgbRendererUiState(
    val stretchType: StretchType,
    val minMaxMinValues: List<Double>,
    val minMaxMaxValues: List<Double>,
    val percentClipMinValue: Double,
    val percentClipMaxValue: Double,
    val stdDevFactor: Double
) {
    companion object {
        val defaultState = RgbRendererUiState(
            stretchType = StretchType.MinMax,
            minMaxMinValues = listOf(10.0, 10.0, 10.0),
            minMaxMaxValues = listOf(150.0, 150.0, 150.0),
            percentClipMinValue = 5.0,
            percentClipMaxValue = 5.0,
            stdDevFactor = 0.5
        )
    }
}

/**
 * Enum representing available stretch parameter types.
 */
enum class StretchType {
    MinMax, PercentClip, StandardDeviation
}
