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

package com.esri.arcgismaps.sample.displaywebscenefromportalitem.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class DisplayWebSceneFromPortalItemViewModel(application: Application) : AndroidViewModel(application) {

    // Create a portal
    private val portal = Portal("https://www.arcgis.com/")

    // Create a portal item
    private val portalItem = PortalItem(portal, "31874da8a16d45bfbc1273422f772270")

    // Create a scene from the portal item
    val arcGISScene = ArcGISScene(portalItem)

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISScene.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load scene", error.message.toString()
                )
            }
        }
    }
}
