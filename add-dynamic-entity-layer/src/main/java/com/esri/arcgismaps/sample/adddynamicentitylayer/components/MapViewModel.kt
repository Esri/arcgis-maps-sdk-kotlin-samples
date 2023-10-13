/* Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.adddynamicentitylayer.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.DynamicEntityLayer
import com.arcgismaps.realtime.ArcGISStreamService
import com.arcgismaps.realtime.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(application: Application,
                   private val sampleCoroutineScope: CoroutineScope,) : AndroidViewModel(application) {

    // set the state of the switch
    var trackLineCheckedState =  mutableStateOf(false)
    var prevObservationCheckedState =  mutableStateOf(false)
    var trackSliderValue = mutableStateOf(5f)

    // Flag to show or dismiss the bottom sheet
    val isBottomSheetVisible = mutableStateOf(false)

    // set the MapView mutable stateflow
    val mapViewState = MutableStateFlow(MapViewState())
    var streamService =
        ArcGISStreamService("https://realtimegis2016.esri.com:6443/arcgis/rest/services/SandyVehicles/StreamServer")
    var dynamicEntityLayer = DynamicEntityLayer(streamService)

    /**
     * Create ArcGIS Stream Service
     */
    init {
        mapViewState.value.arcGISMap.operationalLayers.add(dynamicEntityLayer)
    }

    fun disconnectStreamService() {
        sampleCoroutineScope.launch {
            streamService.disconnect()
        }
    }

    fun connectStreamService() {
        sampleCoroutineScope.launch {
            streamService.connect()
        }
    }

    fun dismissBottomSheet() {
        isBottomSheetVisible.value = false
    }

    fun showBottomSheet() {
        isBottomSheetVisible.value = true
    }

    fun trackLineVisibility(checkedValue: Boolean) {
        trackLineCheckedState.value = checkedValue
        dynamicEntityLayer.trackDisplayProperties.showTrackLine = trackLineCheckedState.value
    }

    fun prevObservationsVisibility(checkedValue: Boolean) {
        prevObservationCheckedState.value = checkedValue
        dynamicEntityLayer.trackDisplayProperties.showPreviousObservations = prevObservationCheckedState.value
    }

    fun setObservations(sliderValue: Float) {
        trackSliderValue.value = sliderValue
        dynamicEntityLayer.trackDisplayProperties.maximumObservations = trackSliderValue.value.toInt()
    }

    fun purgeAllObservations() {
        sampleCoroutineScope.launch {
            streamService.purgeAll()
        }
    }
}

/**
 * Data class that represents the MapView state
 */
data class MapViewState( // This would change based on each sample implementation
    var arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISStreets),
    var viewpoint: Viewpoint = Viewpoint(40.559691, -111.869001, 150000.0)
)
