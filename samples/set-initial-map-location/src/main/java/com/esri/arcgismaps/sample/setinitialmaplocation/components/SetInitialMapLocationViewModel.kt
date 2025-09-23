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

package com.esri.arcgismaps.sample.setinitialmaplocation.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

/**
 * ViewModel that prepares an ArcGISMap with an initial viewpoint.
 */
class SetInitialMapLocationViewModel(application: Application) : AndroidViewModel(application) {

    // Create an ArcGISMap with an imagery basemap and set an initial viewpoint.
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISImagery).apply {
            initialViewpoint = Viewpoint(latitude = -33.867886, longitude = -63.985, scale = 10_000.0)
    }

    // Message dialog view model used to present errors to the user
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the map asynchronously and handle possible failures by showing a message dialog.
        viewModelScope.launch {
            arcGISMap.load().onFailure { throwable ->
                messageDialogVM.showMessageDialog(throwable)
            }
        }
    }
}
