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

package com.esri.arcgismaps.sample.monitorchangestodrawstatus.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.view.DrawStatus
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Monitor changes to draw status sample.
 *
 * Exposes an ArcGISMap and a flow indicating whether the map is currently drawing
 * so the UI can react (show a progress indicator and status text).
 */
class MonitorDrawStatusViewModel(application: Application) : AndroidViewModel(application) {

    // Create a map shown by the sample
    val arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        // Center the map on San Francisco
        initialViewpoint = Viewpoint(
            center = Point(
                x = -13623300.0,
                y = 4548100.0,
                spatialReference = SpatialReference.webMercator()
            ),
            scale = 32e4
        )
    }

    // Flow exposing whether the map is currently drawing.
    // true when DrawStatus is InProgress, false otherwise.
    private val _mapIsDrawing = MutableStateFlow(false)
    val mapIsDrawing = _mapIsDrawing.asStateFlow()

    // Message dialog view model to show errors if map load fails
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the map and surface any errors via the MessageDialogViewModel.
        viewModelScope.launch {
            arcGISMap.load().onFailure { error ->
                // Surface the error so the UI can present a dialog
                messageDialogVM.showMessageDialog(error)
            }
        }
    }

    /**
     * Update the draw status observed from the MapView composable.
     */
    fun updateDrawStatus(drawStatus: DrawStatus) {
        _mapIsDrawing.value = drawStatus == DrawStatus.InProgress
    }
}
