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

package com.esri.arcgismaps.sample.updatelabelsandsymbolstoscaleforvisualaccessibility.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.arcgisservices.LabelingPlacement
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.labeling.ArcadeLabelExpression
import com.arcgismaps.mapping.labeling.LabelDefinition
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.symbology.TextSymbol
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UpdateLabelsAndSymbolsToScaleForVisualAccessibilityViewModel(app: Application) : AndroidViewModel(app) {

    val arcGISMap = ArcGISMap(spatialReference = SpatialReference.webMercator()).apply {
        initialViewpoint = Viewpoint(34.05, -117.19, 2e6)
    }


    // Create a state flow to hold the UI state for the supporting pane controls
    private val _adaptiveUiState = MutableStateFlow(AdaptiveUiState.defaultState)

    // Expose the state flow as read-only for the UI
    val adaptiveUiState = _adaptiveUiState.asStateFlow()

    private val _restaurantViewpoint = MutableStateFlow<Viewpoint?>(null)
    val restaurantViewpoint = _restaurantViewpoint.asStateFlow()

    private val foodFeatureLayer: FeatureLayer = FeatureLayer.createWithFeatureTable(
        ServiceFeatureTable(FOOD_LAYER_URL)
    )

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        foodFeatureLayer.renderer = createFoodRenderer(BASE_SYMBOL_SIZE)
        foodFeatureLayer.labelDefinitions.add(
            LabelDefinition(
                ArcadeLabelExpression("\$feature.name"),
                TextSymbol().apply {
                    color = Color.black
                    size = 12f
                    haloColor = Color.white
                    haloWidth = 2f
                }
            ).apply {
                placement = LabelingPlacement.PointAboveCenter
            }
        )
        foodFeatureLayer.labelsEnabled = true
        arcGISMap.operationalLayers.add(foodFeatureLayer)

        // Set the initial basemap based on the default state
        updateBasemap(selectedBasemap = _adaptiveUiState.value.basemapOptions)
        // Load the map and handle any errors by showing a message dialog
        viewModelScope.launch {
            arcGISMap.load()
                .onFailure { messageDialogVM.showMessageDialog(it) }
                .onSuccess {
                    foodFeatureLayer.load()
                        .onFailure { messageDialogVM.showMessageDialog(it) }
                        .onSuccess {
                            foodFeatureLayer.renderer = createFoodRenderer(_adaptiveUiState.value.symbolSize)
                            foodFeatureLayer.fullExtent?.let { extent ->
                                _restaurantViewpoint.value = Viewpoint(extent)
                            }
                        }
                }
        }
    }


    // Update the size of text and symbol when User toggled apply system text size
    fun onSelectSystemTextSize(isEnabled: Boolean) {
        // Update the UI state:
        val fontScale = _adaptiveUiState.value.fontScale
        _adaptiveUiState.update { currentState ->
            currentState.copy(
                isSystemTextScaleEnabled = isEnabled,
            )
        }
        updateFontScale(fontScale)
    }

    // Update the Font and symbol
    fun updateFontScale(fontScale: Float) {
        _adaptiveUiState.update { currentState ->
            currentState.copy(
                fontScale = fontScale,
                symbolSize = if (currentState.isSystemTextScaleEnabled) {
                    BASE_SYMBOL_SIZE * fontScale
                } else {
                    BASE_SYMBOL_SIZE
                }
            )
        }
        foodFeatureLayer.renderer = createFoodRenderer(
            if (_adaptiveUiState.value.isSystemTextScaleEnabled) {
                BASE_SYMBOL_SIZE * fontScale
            } else {
                BASE_SYMBOL_SIZE
            }
        )
    }

    fun updateBasemap(selectedBasemap: BasemapOptions) {
        // Update the UI state:
        _adaptiveUiState.update { currentState ->
            currentState.copy(basemapOptions = selectedBasemap)
        }
        // Apply the state change for the viewmodel objects:
        val basemap = Basemap(basemapStyle = selectedBasemap.getBasemapStyle())
        arcGISMap.setBasemap(basemap = basemap)
        viewModelScope.launch {
            basemap.load()
        }
    }

    companion object {
        private const val BASE_SYMBOL_SIZE = 10f
        private const val FOOD_LAYER_URL =
            "https://services2.arcgis.com/ZQgQTuoyBrtmoGdP/arcgis/rest/services/redlands_food/FeatureServer/0"
    }
}

data class AdaptiveUiState(
    val basemapOptions: BasemapOptions,
    val isSystemTextScaleEnabled: Boolean,
    val fontScale: Float,
    val symbolSize: Float
) {
    companion object {
        val defaultState = AdaptiveUiState(
            basemapOptions = BasemapOptions.Light,
            isSystemTextScaleEnabled = true,
            fontScale = 1f,
            symbolSize = 10f
        )

    }
}

// Symbol needs to be scaled separately
private fun createFoodRenderer(symbolSize: Float): SimpleRenderer {
    return SimpleRenderer(
        SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color(0xFF1769AA.toInt()), symbolSize).apply {
            outline = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.white, 2f)
        }
    )
}

enum class BasemapOptions {
    Light, Dark
}

fun BasemapOptions.getBasemapStyle(): BasemapStyle {
    return when (this) {
        BasemapOptions.Light -> BasemapStyle.ArcGISLightGray
        BasemapOptions.Dark -> BasemapStyle.ArcGISDarkGray
    }
}
