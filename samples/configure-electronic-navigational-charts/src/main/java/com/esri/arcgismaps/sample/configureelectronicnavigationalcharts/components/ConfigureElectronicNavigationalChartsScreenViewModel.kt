/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.configureelectronicnavigationalcharts.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.hydrography.EncAreaSymbolizationType
import com.arcgismaps.hydrography.EncCell
import com.arcgismaps.hydrography.EncColorScheme
import com.arcgismaps.hydrography.EncEnvironmentSettings
import com.arcgismaps.hydrography.EncExchangeSet
import com.arcgismaps.hydrography.EncFeature
import com.arcgismaps.hydrography.EncPointSymbolizationType
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.EncLayer
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.esri.arcgismaps.sample.configureelectronicnavigationalcharts.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ConfigureElectronicNavigationalChartsScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() +
                File.separator +
                application.getString(R.string.configure_electronic_navigational_charts_app_name)
    }

    // Paths to ENC data and hydrology resources
    private val encResourcesPath = provisionPath + application.getString(R.string.enc_res_dir)
    private val encDataPath = provisionPath + application.getString(R.string.enc_data_dir)

    // Create an ENC exchange set from the local ENC data
    private val encExchangeSet = EncExchangeSet(listOf(encDataPath))
    private val encEnvironmentSettings: EncEnvironmentSettings = EncEnvironmentSettings
    private val encMarinerSettings = encEnvironmentSettings.displaySettings.marinerSettings

    // Create an empty map, to be updated once ENC data is loaded
    var arcGISMap by mutableStateOf(ArcGISMap())

    // Passed to the composable MapView to support identify operations.
    val mapViewProxy = MapViewProxy()

    private val _selectedEncFeature = MutableStateFlow<EncFeature?>(null)
    val selectedEncFeature = _selectedEncFeature.asStateFlow()

    var currentColorScheme by mutableStateOf(encMarinerSettings.colorScheme)
        private set

    var currentAreaSymbolizationType by mutableStateOf(encMarinerSettings.areaSymbolizationType)
        private set

    var currentPointSymbolizationType by mutableStateOf(encMarinerSettings.pointSymbolizationType)
        private set

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Provide ENC environment with location of ENC resources and configure SENC caching location
        encEnvironmentSettings.resourcePath = encResourcesPath
        encEnvironmentSettings.sencDataPath = application.externalCacheDir?.path
        configureEncDisplaySettings()

        viewModelScope.launch {
            encExchangeSet.load().onSuccess {

                // Set the map to the oceans basemap style, and initial viewpoint
                arcGISMap = ArcGISMap(BasemapStyle.ArcGISOceans).apply {
                    initialViewpoint = Viewpoint( -32.5,60.95,67e3)
                }

                encExchangeSet.datasets.forEach { encDataset ->
                    // Create a layer for each ENC dataset and add it to the map
                    val encCell = EncCell(encDataset)
                    val encLayer = EncLayer(encCell)
                    arcGISMap.operationalLayers.add(encLayer)

                    encLayer.load().onFailure { error -> messageDialogVM.showMessageDialog(error) }
                }
            }.onFailure { error -> messageDialogVM.showMessageDialog(error) }
        }
    }

    fun updateColorScheme(colorScheme: EncColorScheme) {
        encMarinerSettings.colorScheme = colorScheme
        currentColorScheme = colorScheme
    }

    fun updateAreaSymbolizationType(areaSymbolizationType: EncAreaSymbolizationType) {
        encMarinerSettings.areaSymbolizationType = areaSymbolizationType
        currentAreaSymbolizationType = areaSymbolizationType
    }

    fun updatePointSymbolizationType(pointSymbolizationType: EncPointSymbolizationType) {
        encMarinerSettings.pointSymbolizationType = pointSymbolizationType
        currentPointSymbolizationType = pointSymbolizationType
    }

    /**
     * Identifies the ENC feature at the tapped screen coordinate and selects it for display.
     */
    fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        viewModelScope.launch {
            arcGISMap.operationalLayers.filterIsInstance<EncLayer>().forEach { encLayer ->
                encLayer.clearSelection()
            }
            _selectedEncFeature.value = null

            mapViewProxy.identifyLayers(singleTapConfirmedEvent.screenCoordinate, 10.dp)
                .onSuccess { identifyResults ->
                    val encIdentifyResult = identifyResults.firstOrNull { identifyResult ->
                        identifyResult.layerContent is EncLayer &&
                                identifyResult.geoElements.any { geoElement -> geoElement is EncFeature }
                    }
                    val encLayer = encIdentifyResult?.layerContent as? EncLayer
                    val encFeature = encIdentifyResult?.geoElements
                        ?.filterIsInstance<EncFeature>()
                        ?.firstOrNull()

                    if (encLayer != null && encFeature != null) {
                        encLayer.selectFeature(encFeature)
                        _selectedEncFeature.value = encFeature
                    }
                }.onFailure { error -> messageDialogVM.showMessageDialog(error) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        encEnvironmentSettings.resourcePath = null
        encEnvironmentSettings.sencDataPath = null
        encEnvironmentSettings.displaySettings.marinerSettings.resetToDefaults()
        encEnvironmentSettings.displaySettings.textGroupVisibilitySettings.resetToDefaults()
        encEnvironmentSettings.displaySettings.viewingGroupSettings.resetToDefaults()
    }

    /**
     * Disables a subset of text and viewing groups so the charts start less cluttered.
     */
    private fun configureEncDisplaySettings() {
        encEnvironmentSettings.displaySettings.textGroupVisibilitySettings.apply {
            includeGeographicNames = false
            includeNatureOfSeabed = false
        }

        encEnvironmentSettings.displaySettings.viewingGroupSettings.apply {
            includeDepthContours = false
            includeLights = false
            includeSpotSoundings = false
        }

        currentColorScheme = encMarinerSettings.colorScheme
        currentAreaSymbolizationType = encMarinerSettings.areaSymbolizationType
        currentPointSymbolizationType = encMarinerSettings.pointSymbolizationType
    }
}

val colorSchemes: List<EncColorScheme> = listOf(
    EncColorScheme.Day, EncColorScheme.Dusk, EncColorScheme.Night
)
val areaSymbolizationTypes: List<EncAreaSymbolizationType> = listOf(
    EncAreaSymbolizationType.Plain, EncAreaSymbolizationType.Symbolized
)
val pointSymbolizationTypes: List<EncPointSymbolizationType> = listOf(
    EncPointSymbolizationType.PaperChart, EncPointSymbolizationType.Simplified
)
