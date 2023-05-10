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

package com.esri.arcgismaps.sample.displaycomposablemapview.components

import androidx.lifecycle.ViewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MapViewModel() : ViewModel() {
    // set the MapView mutable stateflow
    private val _mapViewState = MutableStateFlow(MapViewState())
    val mapViewState: StateFlow<MapViewState> = _mapViewState.asStateFlow()

    init {
        _mapViewState.value =
            MapViewState(arcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight))
    }

    /**
     * Switch between two basemaps
     */
    fun changeBasemap() {
        val newArcGISMap: ArcGISMap =
            if (_mapViewState.value.arcGISMap.basemap.value?.name.equals("ArcGIS:NavigationNight")) {
                ArcGISMap(BasemapStyle.ArcGISStreets)
            } else {
                ArcGISMap(BasemapStyle.ArcGISNavigationNight)
            }
        _mapViewState.update { it.copy(arcGISMap = newArcGISMap) }
    }
}


/**
 * Data class that represents the MapView state
 */
data class MapViewState( // This would change based on each sample implementation
    var arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight),
    var viewpoint: Viewpoint = Viewpoint(39.8, -98.6, 10e7)
)
