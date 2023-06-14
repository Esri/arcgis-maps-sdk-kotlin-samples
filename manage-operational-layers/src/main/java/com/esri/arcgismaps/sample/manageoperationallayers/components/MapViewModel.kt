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

package com.esri.arcgismaps.sample.manageoperationallayers.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class MapViewModel(application: Application) : AndroidViewModel(application) {
    // set the MapView mutable stateflow
    val mapViewState = MutableStateFlow(MapViewState())

    /**
     * Switch between two basemaps
     */
    fun changeBasemap() {
        val newArcGISMap: ArcGISMap =
            if (mapViewState.value.arcGISMap.basemap.value?.name.equals("ArcGIS:NavigationNight")) {
                ArcGISMap(BasemapStyle.ArcGISStreets)
            } else {
                ArcGISMap(BasemapStyle.ArcGISNavigationNight)
            }
        mapViewState.update { it.copy(arcGISMap = newArcGISMap) }
    }
}


/**
 * Data class that represents the MapView state
 */
data class MapViewState( // This would change based on each sample implementation
    var arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight),
    var viewpoint: Viewpoint = Viewpoint(39.8, -98.6, 10e7)
)
