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

package com.esri.arcgismaps.sample.displaymapfromportalitem.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

/**
 * ViewModel for displaying a map from a portal item.
 */
class DisplayMapFromPortalItemViewModel(app: Application) : AndroidViewModel(app) {

    // List of available portal item map options
    val mapOptions = listOf(
        PortalItemMap(
            title = "Terrestrial Ecosystems of the World",
            itemId = "5be0bc3ee36c4e058f7b3cebc21c74e6"
        ),
        PortalItemMap(
            title = "Recent Hurricanes, Cyclones and Typhoons",
            itemId = "064f2e898b094a17b84e4a4cd5e5f549"
        ),
        PortalItemMap(
            title = "Geology of United States",
            itemId = "92ad152b9da94dee89b9e387dfe21acd"
        )
    )

    // The currently selected portal item map option
    var currentMapOption by mutableStateOf(mapOptions.first())
        private set

    // The ArcGISMap to display
    var arcGISMap by mutableStateOf(
        ArcGISMap(item = mapOptions.first().portalItem)
    )
        private set

    // Message dialog for error handling
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Updates the map to display the selected portal item map option.
     */
    fun onMapOptionSelected(portalItemOption: PortalItemMap) {
        if (portalItemOption != currentMapOption) {
            currentMapOption = portalItemOption
            arcGISMap = ArcGISMap(item = portalItemOption.portalItem)
            viewModelScope.launch {
                arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
            }
        }
    }


    /**
     * Data class representing a portal item map option.
     */
    data class PortalItemMap(
        val title: String,
        val itemId: String
    ) {
        val portalItem: PortalItem by lazy {
            PortalItem(
                portal = Portal.arcGISOnline(Portal.Connection.Anonymous),
                itemId = itemId
            )
        }
    }
}
