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

package com.esri.arcgismaps.sample.updatebasemapforcontrastaccessibility.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the UpdateBasemapForContrastAccessibility sample.
 *
 * Owns the selected effective contrast appearance, to keep the map synchronized to the contrast basemap type.
 */
class UpdateBasemapForContrastAccessibilityViewModel(app: Application) : AndroidViewModel(app) {

    val arcGISMap = ArcGISMap(spatialReference = SpatialReference.webMercator()).apply {
        initialViewpoint = Viewpoint(34.05, -117.19, 2e6)
    }

    private val _contrastUiState = MutableStateFlow(ContrastUiState.defaultState)
    val contrastUiState = _contrastUiState.asStateFlow()

    val messageDialogVM = MessageDialogViewModel()

    init {
        applyContrastBasemap(contrast = _contrastUiState.value.contrastAppearance)
    }

    /**
     * Ensures the selected [contrast] is in sync with the MapView.
     */
    fun syncContrast(contrast: ContrastAppearance) {
        if (_contrastUiState.value.contrastAppearance == contrast) return
        applyContrastBasemap(contrast)
    }

    /**
     * Applies the contrast-specific basemap to the MapView.
     */
    private fun applyContrastBasemap(contrast: ContrastAppearance) {
        updateContrastAppearance(contrast = contrast)
        val isVisible = _contrastUiState.value.isReferenceLayersEnabled
        arcGISMap.setBasemap(contrastBasemapFor(contrast))

        viewModelScope.launch {
            arcGISMap.load().getOrElse { messageDialogVM.showMessageDialog(it) }
            applyReferenceLayersVisibility(map = arcGISMap, isVisible = isVisible)
        }
    }

    /**
     * Applies the current reference-layers [isVisible] flag to the [map].
     */
    private fun applyReferenceLayersVisibility(map: ArcGISMap, isVisible: Boolean) {
        map.basemap.value?.referenceLayers?.forEach { layer ->
            layer.isVisible = isVisible
        }
    }

    /**
     * Updates whether the sample resolves the [mode] automatically or uses the manual picker.
     */
    fun updateContrastMode(mode: ContrastMode) {
        _contrastUiState.update { currentState ->
            currentState.copy(contrastMode = mode)
        }
    }

    /**
     * Updates the [contrast] appearance while the sample is in manual mode.
     */
    fun updateContrastAppearance(contrast: ContrastAppearance) {
        _contrastUiState.update { currentState ->
            currentState.copy(contrastAppearance = contrast)
        }
    }

    /**
     * Update reference layers using [isVisible].
     */
    fun updateReferenceLayerVisibility(isVisible: Boolean) {
        _contrastUiState.update { currentState ->
            currentState.copy(isReferenceLayersEnabled = isVisible)
        }
        applyReferenceLayersVisibility(map = arcGISMap, isVisible = isVisible)
    }
}

/**
 * UI states for the controls in the supporting pane to configure the displayed MapView.
 */
data class ContrastUiState(
    val contrastMode: ContrastMode,
    val contrastAppearance: ContrastAppearance,
    val isReferenceLayersEnabled: Boolean
) {
    companion object {
        val defaultState = ContrastUiState(
            contrastMode = ContrastMode.Automatic,
            contrastAppearance = ContrastAppearance.HighContrastLight,
            isReferenceLayersEnabled = true
        )
    }
}

/**
 * State to track whether appearance comes from device settings or the manual picker.
 */
enum class ContrastMode {
    Automatic,
    Manual
}

/**
 * State to track the four contrast appearance variants.
 */
enum class ContrastAppearance {
    Light,
    HighContrastLight,
    Dark,
    HighContrastDark
}

/**
 * Maps the selected appearance to the contrast accessibility basemaps used by the sample.
 */
private fun contrastBasemapFor(contrast: ContrastAppearance): Basemap {
    return when (contrast) {
        ContrastAppearance.Light -> Basemap(BasemapStyle.ArcGISLightGray)
        ContrastAppearance.Dark -> Basemap(BasemapStyle.ArcGISDarkGray)
        ContrastAppearance.HighContrastLight -> Basemap("https://www.arcgis.com/home/item.html?id=084291b0ecad4588b8c8853898d72445")
        ContrastAppearance.HighContrastDark -> Basemap("https://www.arcgis.com/home/item.html?id=3e23478909194c54992eaaee78b5f754")
    }
}
