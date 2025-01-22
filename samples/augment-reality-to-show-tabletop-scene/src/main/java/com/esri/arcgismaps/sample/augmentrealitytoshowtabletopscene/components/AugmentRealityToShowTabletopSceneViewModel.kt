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

package com.esri.arcgismaps.sample.augmentrealitytoshowtabletopscene.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.MobileScenePackage
import com.arcgismaps.mapping.NavigationConstraint
import com.esri.arcgismaps.sample.augmentrealitytoshowtabletopscene.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File


class AugmentRealityToShowTabletopSceneViewModel(application: Application) : AndroidViewModel(application) {

    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(R.string.augment_reality_to_show_tabletop_scene_app_name)
    }

    // Get the folder path containing the mobile scene package (.mspk) file
    private val filePath = "$provisionPath/philadelphia.mspk"

    // Create a mobile scene package
    private val scenePackage = MobileScenePackage(filePath)

    // Create a mutable state variable to hold the scene. Later loaded from the scene package
    var scene: ArcGISScene? by mutableStateOf(null)

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            // Load the mobile scene package
            scenePackage.load().onSuccess {
                // Get the first scene from the package
                scene = scenePackage.scenes.first().apply {
                    // Set the navigation constraint to allow you to look at the scene from below
                    baseSurface.navigationConstraint = NavigationConstraint.None
                }
            }.onFailure {
                messageDialogVM.showMessageDialog(
                    "Error loading mobile scene package", it.message.toString()
                )
            }
        }
    }
}
