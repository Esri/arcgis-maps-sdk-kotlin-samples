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

package com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.portal.Portal
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the ShowContrastResponsiveGeoView sample.
 *
 * Owns the selected effective contrast appearance, to keep the map and scene synchronized to the contrast web-map type.
 */
class ShowContrastResponsiveGeoViewViewModel(app: Application) : AndroidViewModel(app) {

    var arcGISMap by mutableStateOf(ArcGISMap())
        private set

    var arcGISScene by mutableStateOf(ArcGISScene())
        private set

    private val portal = Portal.arcGISOnline(connection = Portal.Connection.Anonymous)

    private val _contrastUiState = MutableStateFlow(ContrastUiState.defaultState)
    val contrastUiState = _contrastUiState.asStateFlow()

    val messageDialogVM = MessageDialogViewModel()

    init {
        applyGeoViewContrastBasemap(contrast = _contrastUiState.value.contrastAppearance)
    }

    /**
     * Ensures the selected [contrast] is in sync with both GeoViews.
     */
    fun syncGeoViewContrast(contrast: ContrastAppearance) {
        if (_contrastUiState.value.contrastAppearance == contrast) return
        applyGeoViewContrastBasemap(contrast)
    }

    /**
     * Applies the contrast-specific web map to both GeoViews.
     */
    private fun applyGeoViewContrastBasemap(contrast: ContrastAppearance) {
        updateContrastAppearance(contrast = contrast)
        val isVisible = _contrastUiState.value.isReferenceLayersEnabled
        val webMapItemId = contrastWebMapFor(contrast)
        arcGISMap = createArcGISMap(webMapItemId)

        viewModelScope.launch {
            arcGISMap.load().getOrElse { messageDialogVM.showMessageDialog(it) }
            arcGISMap.basemap.value?.clone()?.let { basemap ->
                arcGISScene = createArcGISScene(basemap)
                arcGISScene.load().getOrElse { messageDialogVM.showMessageDialog(it) }
                applyReferenceLayersVisibility(geoModel = arcGISMap, isVisible = isVisible)
                applyReferenceLayersVisibility(geoModel = arcGISScene, isVisible = isVisible)
            }
        }
    }

    /**
     * Returns the map for the MapView using a contrast [webMapItemId].
     */
    private fun createArcGISMap(webMapItemId: String): ArcGISMap {
        return ArcGISMap(
            item = PortalItem(
                portal = portal,
                itemId = webMapItemId
            )
        ).apply {
            initialViewpoint = sampleViewpoint
        }
    }

    /**
     * Returns the scene that adopts the loaded web map's basemap after the map finishes loading.
     */
    private fun createArcGISScene(basemap: Basemap): ArcGISScene {
        return ArcGISScene(basemap = basemap).apply {
            initialViewpoint = sampleViewpoint
        }
    }

    /**
     * Applies the current reference-layers [isVisible] flag to either [geoModel].
     */
    private fun applyReferenceLayersVisibility(geoModel: Any, isVisible: Boolean) {
        val referenceLayers = when (geoModel) {
            is ArcGISMap -> geoModel.basemap.value?.referenceLayers
            is ArcGISScene -> geoModel.basemap.value?.referenceLayers
            else -> null
        }

        referenceLayers?.forEach { layer ->
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
     * Update to switch the [geoViewType] between the 2D map and 3D scene implementations.
     */
    fun updateGeoViewType(geoViewType: GeoViewType) {
        _contrastUiState.update { currentState ->
            currentState.copy(geoViewType = geoViewType)
        }
    }

    /**
     * Update reference layers using [isVisible] flag for both GeoViews.
     */
    fun updateReferenceLayerVisibility(isVisible: Boolean) {
        _contrastUiState.update { currentState ->
            currentState.copy(isReferenceLayersEnabled = isVisible)
        }
        applyReferenceLayersVisibility(geoModel = arcGISMap, isVisible = isVisible)
        applyReferenceLayersVisibility(geoModel = arcGISScene, isVisible = isVisible)
    }
}

/**
 * UI states for the controls in the supporting pane to configure the GeoViews.
 */
data class ContrastUiState(
    val contrastMode: ContrastMode,
    val contrastAppearance: ContrastAppearance,
    val geoViewType: GeoViewType,
    val isReferenceLayersEnabled: Boolean
) {
    companion object {
        val defaultState = ContrastUiState(
            contrastMode = ContrastMode.Automatic,
            contrastAppearance = ContrastAppearance.HighContrastLight,
            geoViewType = GeoViewType.MapView,
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
 * State to track the GeoView implementation currently displayed.
 */
enum class GeoViewType {
    MapView,
    SceneView
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
 * Default viewpoint used for both the map and scene.
 */
private val sampleViewpoint = Viewpoint(34.05, -117.19, 2e6)

/**
 * Maps the selected appearance to the contrast accessibility web-maps used by the sample.
 */
private fun contrastWebMapFor(contrast: ContrastAppearance): String {
    return when (contrast) {
        ContrastAppearance.Light -> "979c6cc89af9449cbeb5342a439c6a76"
        ContrastAppearance.Dark -> "358ec1e175ea41c3bf5c68f0da11ae2b"
        ContrastAppearance.HighContrastLight -> "084291b0ecad4588b8c8853898d72445"
        ContrastAppearance.HighContrastDark -> "3e23478909194c54992eaaee78b5f754"
    }
}
