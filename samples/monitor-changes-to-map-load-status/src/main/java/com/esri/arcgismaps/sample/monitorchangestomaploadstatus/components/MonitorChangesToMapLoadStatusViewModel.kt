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

package com.esri.arcgismaps.sample.monitorchangestomaploadstatus.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel that holds an ArcGISMap and exposes its load status as a StateFlow
 */
class MonitorChangesToMapLoadStatusViewModel(app: Application) : AndroidViewModel(app) {

    // ArcGISMap created once and exposed as a compose state so UI can consume it
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISImagery).apply {
            // Provide an initial viewpoint
            initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
        }


    // Expose load status string as StateFlow to be observed by the UI
    private val _loadStatusText = MutableStateFlow(LoadStatus.NotLoaded::class.simpleName)
    val loadStatusText = _loadStatusText.asStateFlow()

    // Message dialog view model to show errors
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the map and monitor load status changes
        viewModelScope.launch {
            // Start loading the map and show any immediate failure
            arcGISMap.load().onFailure { throwable ->
                messageDialogVM.showMessageDialog(throwable)
            }

            // Collect load status updates and update the StateFlow
            arcGISMap.loadStatus.collect { loadStatus ->
                _loadStatusText.value = loadStatus::class.simpleName
            }
        }
    }
}
