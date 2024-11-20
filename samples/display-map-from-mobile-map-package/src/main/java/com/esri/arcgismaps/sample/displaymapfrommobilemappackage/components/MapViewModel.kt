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

package com.esri.arcgismaps.sample.displaymapfrommobilemappackage.components


import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.MobileMapPackage
import com.esri.arcgismaps.sample.displaymapfrommobilemappackage.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(
            R.string.display_map_from_mobile_map_package_app_name
        )
    }

    // Get the file path of the (.mmpk) file
    private val filePath = provisionPath + application.getString(R.string.yellowstone_mmpk)

    // View model to handle popup dialogs
    val messageDialogVM = MessageDialogViewModel()

    // Create the mobile map package
    private val mapPackage = MobileMapPackage(filePath)

    // Create a mutable state for the map
    var map: ArcGISMap by mutableStateOf(ArcGISMap())

    init {
        viewModelScope.launch {
            // Load the mobile map package
            mapPackage.load().onSuccess {
                // Add the map from the mobile map package to the MapView
                map = mapPackage.maps.first()
            }.onFailure {
                messageDialogVM.showMessageDialog(
                    "Error loading mobile map package", it.message.toString()
                )
            }
        }
    }
}
