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

package com.esri.arcgismaps.sample.browsebuildingfloors.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.indoors.ButtonPosition
import com.arcgismaps.toolkit.indoors.FloorFilterState
import com.arcgismaps.toolkit.indoors.UIProperties
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class BrowseBuildingFloorsViewModel(app: Application) : AndroidViewModel(app) {

    // Create a portal item using a floor-aware web map.
    val portalItem = PortalItem(
        portal = Portal("https://www.arcgis.com/"),
        itemId = "f133a698536f44c8884ad81f80b6cfc7"
    )


    // Initial Map to hold a portalItem
    val arcGISMap = ArcGISMap(portalItem)

    // FloorFilterState to hold the current filter info
    val floorFilterState = FloorFilterState(geoModel = arcGISMap)

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }
}
