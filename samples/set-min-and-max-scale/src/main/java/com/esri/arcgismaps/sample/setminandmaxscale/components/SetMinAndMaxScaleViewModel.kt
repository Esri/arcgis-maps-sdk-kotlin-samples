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

package com.esri.arcgismaps.sample.setminandmaxscale.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

/**
 * ViewModel for the SetMinAndMaxScale sample.
 */
class SetMinAndMaxScaleViewModel(application: Application) : AndroidViewModel(application) {

    // Expose the map as a mutable state so the Compose UI can observe changes.
    var arcGISMap = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
            // Set an initial viewpoint.
            initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
            // Set sample min and max scales to demonstrate enforcing zoom limits.
            // Note: minScale represents the most zoomed-out denominator and
            // maxScale the most zoomed-in denominator.
            minScale = 8000.0
            maxScale = 2000.0
     }


    // Message dialog VM to show any errors during load.
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the map and surface any load failures via the message dialog VM.
        viewModelScope.launch {
            arcGISMap.load().onFailure { throwable ->
                messageDialogVM.showMessageDialog(throwable)
            }
        }
    }
}
