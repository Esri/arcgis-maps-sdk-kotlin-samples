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
import com.esri.arcgismaps.sample.createandsavemap.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel

class MapViewModel(application: Application) : AndroidViewModel(application) {

    // view model to handle popup dialogs
    val messageDialogVM = MessageDialogViewModel()

    // set up authenticator state to handle authentication challenges
    val authenticatorState = AuthenticatorState().apply {
        oAuthUserConfiguration = OAuthUserConfiguration(
            portalUrl = application.getString(R.string.portal_url),
            clientId = application.getString(R.string.client_id),
            redirectUrl = application.getString(R.string.redirect_url)
        )
    }

    // require use of user credential to load portal
    private val portal = Portal("https://www.arcgis.com", Portal.Connection.Authenticated)

    // update displayed map once user is authenticated
    var arcGISMap by mutableStateOf(ArcGISMap())

    // folders on portal associated with the authenticated user
    private val _portalFolders = MutableStateFlow<List<PortalFolder>>(listOf())
    val portalFolders = _portalFolders.asStateFlow()


    // load a couple of feature layers
    private val worldElevation =
        ArcGISMapImageLayer("https://sampleserver6.arcgisonline.com/arcgis/rest/services/WorldTimeZones/MapServer")
    private val usCensus =
        ArcGISMapImageLayer("https://sampleserver6.arcgisonline.com/arcgis/rest/services/Census/MapServer")
    val availableLayers: List<Layer> = listOf(worldElevation, usCensus)

    // associate basemap styles with friendly names
    val stylesMap: Map<String, BasemapStyle> = mapOf(
        Pair("Streets", BasemapStyle.ArcGISStreets),
        Pair("Imagery", BasemapStyle.ArcGISImageryStandard),
        Pair("Topographic", BasemapStyle.ArcGISTopographic),
        Pair("Oceans", BasemapStyle.ArcGISOceans)
    )

    // properties hoisted from UI bottom sheet

    var selectedBasemapStyle by mutableStateOf("Streets")
        private set

    fun updateBasemapStyle(style: String) {
        selectedBasemapStyle = style

        // update map to display selected basemap style
        arcGISMap.setBasemap(Basemap(stylesMap.getValue(selectedBasemapStyle)))
    }

    fun updateActiveLayers(layer: Layer) {
        arcGISMap.operationalLayers.apply {
            if (contains(layer)) {
                remove(layer)
            } else {
                add(layer)
            }
        }
    }

    var mapName by mutableStateOf("My Map")
        private set

    fun updateName(name: String) {
        mapName = name
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

    var mapDescription by mutableStateOf("")
        private set

    fun updateDescription(description: String) {
        mapDescription = description
    }

    init {
        viewModelScope.launch {
            portal.load().onSuccess {
                // when the user has successfully authenticated and the portal is loaded...

                // populate the portal folders flow
                portal.portalInfo?.apply {
                    this.user?.fetchContent()?.onSuccess {
                        _portalFolders.value = it.folders
                    }
                }

                // load the streets basemap and set an initial viewpoint
                arcGISMap = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
                    initialViewpoint = Viewpoint(38.85, -90.2, 1e7)
                }

                // load operational layers
                worldElevation.load()
                usCensus.load()
            }.onFailure {
                // login was cancelled or failed to authenticate
                messageDialogVM.showMessageDialog(
                    application.getString(R.string.createAndSaveMap_failedToLoadPortal),
                    it.message.toString()
                )
            }
        }
    }

    /**
     * Saves the map to a user's account.
     */
    suspend fun save(): Result<Unit> {
        return arcGISMap.saveAs(
            portal,
            description = mapDescription,
            folder = portalFolder,
            tags = mapTags.split(",").map { str -> str.trim() },
            forceSaveToSupportedVersion = false,
            thumbnail = null,
            title = mapName
        )
    }


}