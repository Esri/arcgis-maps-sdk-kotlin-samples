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

package com.esri.arcgismaps.sample.createandsavemap.components

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISMapImageLayer
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.portal.Portal
import com.arcgismaps.portal.PortalFolder
import com.arcgismaps.toolkit.authentication.AuthenticatorState

class MapViewModel(application: Application) : AndroidViewModel(application) {

    // set up authenticator state to handle authentication challenges
    val authenticatorState = AuthenticatorState().apply {
        oAuthUserConfiguration = OAuthUserConfiguration(
            portalUrl = "https://www.arcgis.com",
            clientId = "InMihrA8yZXBALCv",
            redirectUrl = "create-save-map://auth"
        )
    }

    // require use of user credential to load portal
    val portal = Portal("https://www.arcgis.com", Portal.Connection.Authenticated)

    var arcGISMap by mutableStateOf<ArcGISMap?>(null)

    private val _portalFolders = MutableStateFlow<List<PortalFolder>>(listOf())
    val portalFolders = _portalFolders.asStateFlow()


    // load a couple of feature layers
    private val worldElevation =
        ArcGISMapImageLayer("https://sampleserver6.arcgisonline.com/arcgis/rest/services/WorldTimeZones/MapServer")
    private val usCensus =
        ArcGISMapImageLayer("https://sampleserver6.arcgisonline.com/arcgis/rest/services/Census/MapServer")

    val availableLayers: List<Layer> = listOf(worldElevation, usCensus)

    fun updateActiveLayers(layer: Layer) {
        if (arcGISMap?.operationalLayers?.contains(layer) == true) {
            arcGISMap!!.operationalLayers.remove(layer)
        } else {
            arcGISMap?.operationalLayers?.add(layer)
        }
    }

    // associate basemap styles with user-presentable names
    val stylesMap: Map<String, BasemapStyle> = mapOf(
        Pair("Streets", BasemapStyle.ArcGISStreets),
        Pair("Imagery", BasemapStyle.ArcGISImageryStandard),
        Pair("Topographic", BasemapStyle.ArcGISTopographic),
        Pair("Oceans", BasemapStyle.ArcGISOceans)
    )
    var basemapStyle by mutableStateOf("Streets")
        private set

    fun updateBasemapStyle(style: String) {
        basemapStyle = style

        // style non-null as list items populated from stylesMap
        arcGISMap?.setBasemap(Basemap(stylesMap[style]!!))
    }

    var mapName by mutableStateOf("My Map")
        private set

    fun updateName(name: String) {
        mapName = name
    }

    var mapDescription by mutableStateOf("Enter a description here.")
        private set

    fun updateDescription(description: String) {
        mapDescription = description
    }

    var mapTags by mutableStateOf("map, census, layers")
        private set

    fun updateTags(tags: String) {
        mapTags = tags
    }

    var portalFolder by mutableStateOf<PortalFolder?>(null)

    fun updateFolder(folder: PortalFolder?) {
        portalFolder = folder
    }


    /**
     * Saves the map to a user's account.
     */
    suspend fun save(): Result<Unit> {
        return arcGISMap?.saveAs(
            portal,
            description = mapDescription,
            folder = portalFolder,
            tags = mapTags.split(",").map { str -> str.trim() },
            forceSaveToSupportedVersion = false,
            thumbnail = null,
            title = mapName
        ) ?: Result.failure(NullPointerException("Can't save a null map."))
    }

    init {
        viewModelScope.launch {
            portal.load().onSuccess {
                portal.portalInfo?.apply {
                    this.user?.fetchContent()?.onSuccess {
                        _portalFolders.value = it.folders
                    }
                }

                arcGISMap = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
                    initialViewpoint = Viewpoint(38.85, -90.2, 1e7)
                }
            }
        }
    }


}