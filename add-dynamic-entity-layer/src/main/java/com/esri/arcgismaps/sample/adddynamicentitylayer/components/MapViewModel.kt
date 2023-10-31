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
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.DynamicEntityLayer
import com.arcgismaps.realtime.ArcGISStreamService
import com.arcgismaps.realtime.ArcGISStreamServiceFilter
import com.esri.arcgismaps.sample.adddynamicentitylayer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    application: Application,
    private val sampleCoroutineScope: CoroutineScope,
) : AndroidViewModel(application) {

    // set the state of the switches and slider
    val trackLineCheckedState = mutableStateOf(false)
    val prevObservationCheckedState = mutableStateOf(false)
    val trackSliderValue = mutableStateOf(5f)

    // flag to show or dismiss the bottom sheet
    val isBottomSheetVisible = mutableStateOf(false)

    // set the MapView mutable stateflow
    val mapViewState = MutableStateFlow(MapViewState())

    // create ArcGIS Stream Service
    private val streamService =
        ArcGISStreamService(application.getString(R.string.stream_service_url))

    // create ArcGISStreamServiceFilter
    private val streamServiceFilter = ArcGISStreamServiceFilter()

    // layer displaying the dynamic entities on the map
    private val dynamicEntityLayer: DynamicEntityLayer

    /**
     * set the data source for the dynamic entity layer.
     */
    init {
        // set condition on the ArcGISStreamServiceFilter to limit the amount of data coming from the server
        streamServiceFilter.whereClause = "speed > 0"
        streamService.apply {
            filter = streamServiceFilter
            // sets the maximum time (in seconds) an observation remains in the application.
            purgeOptions.maximumDuration = 300.0
        }
        dynamicEntityLayer = DynamicEntityLayer(streamService)

        // add the dynamic entity layer to the map's operational layers
        mapViewState.value.arcGISMap.operationalLayers.add(dynamicEntityLayer)
    }

    // disconnects the stream service
    fun disconnectStreamService() {
        sampleCoroutineScope.launch {
            streamService.disconnect()
        }
    }

    // connects the stream service
    fun connectStreamService() {
        sampleCoroutineScope.launch {
            streamService.connect()
        }
    }

    // to dismiss the bottom sheet
    fun dismissBottomSheet() {
        isBottomSheetVisible.value = false
    }

    // to manage bottomSheet visibility
    fun showBottomSheet() {
        isBottomSheetVisible.value = true
    }

    // to manage track lines visibility
    fun trackLineVisibility(checkedValue: Boolean) {
        trackLineCheckedState.value = checkedValue
        dynamicEntityLayer.trackDisplayProperties.showTrackLine = trackLineCheckedState.value
    }

    // to manage previous observations visibility
    fun prevObservationsVisibility(checkedValue: Boolean) {
        prevObservationCheckedState.value = checkedValue
        dynamicEntityLayer.trackDisplayProperties.showPreviousObservations =
            prevObservationCheckedState.value
    }

    // to set the maximum number of observations displayed per track
    fun setObservations(sliderValue: Float) {
        trackSliderValue.value = sliderValue
        dynamicEntityLayer.trackDisplayProperties.maximumObservations =
            trackSliderValue.value.toInt()
    }

    // remove all dynamic entity observations from the in-memory data cache as well as from the map
    fun purgeAllObservations() {
        sampleCoroutineScope.launch {
            streamService.purgeAll()
        }
    }
}

/**
 * Data class that represents the MapView state
 */
data class MapViewState(
    var arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISStreets),
    var viewpoint: Viewpoint = Viewpoint(40.559691, -111.869001, 150000.0)
)
