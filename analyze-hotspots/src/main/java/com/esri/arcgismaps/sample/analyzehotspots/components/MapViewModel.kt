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

package com.esri.arcgismaps.sample.analyzehotspots.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.tasks.geoprocessing.GeoprocessingParameters
import com.arcgismaps.tasks.geoprocessing.GeoprocessingTask
import com.arcgismaps.tasks.geoprocessing.geoprocessingparameters.GeoprocessingString
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

    suspend fun analyzeHotspots(fromDateInMillis: Long, toDateInMillis: Long) {
        val geoprocessingTask =
            GeoprocessingTask("http://sampleserver6.arcgisonline.com/arcgis/rest/services/911CallsHotspot/GPServer/911%20Calls%20Hotspot")

        geoprocessingTask.load().getOrElse {
            // TODO
        }

        // a map image layer is generated as a result, clear previous results
        mapViewState.value.arcGISMap.operationalLayers.clear()

        // create parameters for geoprocessing job
        val geoprocessingParameters = geoprocessingTask.createDefaultParameters().getOrElse {
            // TODO
        } as GeoprocessingParameters

        val queryString = StringBuilder("(\"DATE\" > date '")
            .append(fromDateInMillis)
            .append(" 00:00:00' AND \"DATE\" < date '")
            .append(toDateInMillis)
            .append(" 00:00:00')")

        val geoprocessingString = GeoprocessingString(queryString.toString())
        geoprocessingParameters.inputs["Query"] = geoprocessingString

        // create and start geoprocessing job
        val geoprocessingJob = geoprocessingTask.createJob(geoprocessingParameters)
        geoprocessingJob.start()
    }
}


/**
 * Data class that represents the MapView state
 */
data class MapViewState(
    // This would change based on each sample implementation
    var arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic),
    var viewpoint: Viewpoint = Viewpoint(
        center = Point(-13671170.0, 5693633.0, SpatialReference(wkid = 3857)),
        scale = 57779.0
    ),
)
