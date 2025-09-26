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

package com.esri.arcgismaps.sample.setsurfacenavigationconstraint.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.NavigationConstraint
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class SetSurfaceNavigationConstraintViewModel(app: Application) : AndroidViewModel(app) {

    // ArcGISScene created from a web scene portal item.
    // Configure the base surface to allow underground navigation and reduce opacity.
    val arcGISScene = ArcGISScene(
        item = PortalItem(
            portal = Portal.arcGISOnline(connection = Portal.Connection.Anonymous),
            itemId = "91a4fafd747a47c7bab7797066cb9272"
        )
    ).apply {
        // Allow the camera to move above and below the elevation surface.
        baseSurface.navigationConstraint = NavigationConstraint.None
        // Sets the opacity so that it is possible to see below the surface.
        baseSurface.opacity = 0.7f
    }

    // Message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            // Load the scene and display an error if loading fails
            arcGISScene.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Error loading scene",
                    description = error.message.toString()
                )
            }
        }
    }
}
