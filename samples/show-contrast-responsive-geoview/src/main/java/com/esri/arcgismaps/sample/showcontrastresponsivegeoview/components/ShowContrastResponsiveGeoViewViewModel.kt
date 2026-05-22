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
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.portal.Portal
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShowContrastResponsiveGeoViewViewModel(app: Application) : AndroidViewModel(app) {
    private val portal = Portal.arcGISOnline(Portal.Connection.Anonymous)
    private var appearanceUpdateToken = 0

    var arcGISMap by mutableStateOf(createWebMap(contrastProfileFor(ContrastAppearance.Light)))
        private set

    var arcGISScene by mutableStateOf(createScene())
        private set

    private val _contrastUiState = MutableStateFlow(
        ContrastUiState()
    )
    val contrastUiState = _contrastUiState.asStateFlow()

    val messageDialogVM = MessageDialogViewModel()
    private var appliedAppearance: ContrastAppearance? = null

    init {
        updateGeoViewAppearance(ContrastAppearance.Light)
    }

    fun updateContrastMode(mode: ContrastMode) {
        _contrastUiState.update { currentState ->
            currentState.copy(contrastMode = mode)
        }
    }

    fun updateManualAppearance(appearance: ContrastAppearance) {
        _contrastUiState.update { currentState ->
            currentState.copy(manualAppearance = appearance)
        }
    }

    fun updateGeoViewType(geoViewType: GeoViewType) {
        _contrastUiState.update { currentState ->
            currentState.copy(geoViewType = geoViewType)
        }
    }

    fun updateReferenceLayerVisibility(isVisible: Boolean) {
        _contrastUiState.update { currentState ->
            currentState.copy(referenceLayersVisible = isVisible)
        }

        applyReferenceLayerVisibility(arcGISMap, isVisible)
        applyReferenceLayerVisibility(arcGISScene, isVisible)
        refreshAvailableLayers()
    }

    fun updateGeoViewAppearance(appearance: ContrastAppearance) {
        if (appliedAppearance == appearance) return

        val sourceProfile = contrastProfileFor(appearance)
        val updateToken = ++appearanceUpdateToken
        val referenceLayersVisible = _contrastUiState.value.referenceLayersVisible

        appliedAppearance = appearance
        arcGISMap = createWebMap(sourceProfile)
        arcGISScene = createScene()

        viewModelScope.launch {
            arcGISMap.load().onSuccess {
                if (updateToken != appearanceUpdateToken) return@launch

                applyReferenceLayerVisibility(arcGISMap, referenceLayersVisible)

                arcGISMap.basemap.value?.clone()?.let { basemap ->
                    basemap.referenceLayers.forEach { layer ->
                        layer.isVisible = referenceLayersVisible
                    }
                    arcGISScene.setBasemap(basemap)
                }

                refreshAvailableLayers()

                arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
            }.onFailure { error ->
                if (updateToken == appearanceUpdateToken) {
                    messageDialogVM.showMessageDialog(error)
                }
            }
        }
    }

    private fun createWebMap(profile: ContrastProfile): ArcGISMap {
        return ArcGISMap(
            item = PortalItem(
                portal = portal,
                itemId = profile.webMapItemId
            )
        ).apply {
            initialViewpoint = sampleViewpoint
        }
    }

    private fun createScene(): ArcGISScene {
        return ArcGISScene().apply {
            initialViewpoint = sampleViewpoint
        }
    }

    private fun applyReferenceLayerVisibility(geoModel: Any, isVisible: Boolean) {
        val referenceLayers = when (geoModel) {
            is ArcGISMap -> geoModel.basemap.value?.referenceLayers
            is ArcGISScene -> geoModel.basemap.value?.referenceLayers
            else -> null
        }

        referenceLayers?.forEach { layer ->
            layer.isVisible = isVisible
        }
    }

    private fun refreshAvailableLayers() {
        val basemap = arcGISMap.basemap.value ?: return
        _contrastUiState.update { currentState ->
            currentState.copy(
                availableLayers = buildList {
                    basemap.baseLayers.forEach { layer ->
                        add(layer.toBasemapLayerInfo(BasemapLayerRole.Base))
                    }
                    basemap.referenceLayers.forEach { layer ->
                        add(layer.toBasemapLayerInfo(BasemapLayerRole.Reference))
                    }
                }
            )
        }
    }
}

data class ContrastUiState(
    val contrastMode: ContrastMode = ContrastMode.Automatic,
    val manualAppearance: ContrastAppearance = ContrastAppearance.HighContrastLight,
    val geoViewType: GeoViewType = GeoViewType.Map,
    val referenceLayersVisible: Boolean = true,
    val availableLayers: List<BasemapLayerInfo> = emptyList()
)

data class BasemapLayerInfo(
    val name: String,
    val role: BasemapLayerRole,
    val isVisible: Boolean
)

enum class BasemapLayerRole {
    Base,
    Reference
}

enum class ContrastMode {
    Automatic,
    Manual
}

enum class GeoViewType {
    Map,
    Scene
}

enum class ContrastAppearance {
    Light,
    Dark,
    HighContrastLight,
    HighContrastDark
}

private val sampleViewpoint = Viewpoint(34.056295, -117.195800, 2_000_000.0)
data class ContrastProfile(
    val title: String,
    val webMapItemId: String
)

private val regularLightProfile = ContrastProfile(
    title = "Regular light",
    webMapItemId = "979c6cc89af9449cbeb5342a439c6a76"
)

private val regularDarkProfile = ContrastProfile(
    title = "Regular dark",
    webMapItemId = "1970c1995b8f44749f4b9b6e81b5ba45"
)

private val enhancedContrastLightProfile = ContrastProfile(
    title = "High contrast light",
    webMapItemId = "084291b0ecad4588b8c8853898d72445"
)

private val enhancedContrastDarkProfile = ContrastProfile(
    title = "High contrast dark",
    webMapItemId = "3e23478909194c54992eaaee78b5f754"
)

private fun contrastProfileFor(appearance: ContrastAppearance): ContrastProfile {
    return when (appearance) {
        ContrastAppearance.Light -> regularLightProfile
        ContrastAppearance.Dark -> regularDarkProfile
        ContrastAppearance.HighContrastLight -> enhancedContrastLightProfile
        ContrastAppearance.HighContrastDark -> enhancedContrastDarkProfile
    }
}

private fun Layer.toBasemapLayerInfo(role: BasemapLayerRole): BasemapLayerInfo {
    return BasemapLayerInfo(
        name = name.ifBlank { id.ifBlank { "Unnamed layer" } },
        role = role,
        isVisible = isVisible
    )
}