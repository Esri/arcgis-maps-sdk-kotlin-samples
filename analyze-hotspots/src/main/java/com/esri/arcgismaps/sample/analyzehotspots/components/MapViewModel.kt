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
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.tasks.geoprocessing.GeoprocessingJob
import com.arcgismaps.tasks.geoprocessing.GeoprocessingParameters
import com.arcgismaps.tasks.geoprocessing.GeoprocessingTask
import com.arcgismaps.tasks.geoprocessing.geoprocessingparameters.GeoprocessingString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MapViewModel(application: Application) : AndroidViewModel(application) {
    // set the MapView mutable stateflow
    val mapViewState = MutableStateFlow(MapViewState())

    // determinate job progress loading dialog
    val showJobProgressDialog = mutableStateOf(false)

    // determinate job progress percentage
    val geoprocessingJobProgress = mutableStateOf(0)

    private var geoprocessingJob: GeoprocessingJob? = null

    fun analyzeHotspots(
        fromDateInMillis: String,
        toDateInMillis: String,
        jobCoroutineScope: CoroutineScope,
    ) {
        val geoprocessingTask =
            GeoprocessingTask("http://sampleserver6.arcgisonline.com/arcgis/rest/services/911CallsHotspot/GPServer/911%20Calls%20Hotspot")

        jobCoroutineScope.launch {
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

            // ("DATE" > date '1998-01-01 00:00:00' AND "DATE" < date '1998-05-31 00:00:00')
            // ("DATE" > date '1997-12-30 00:00:00' AND "DATE" < date '1998-01-13 00:00:00')
            val geoprocessingString =
                GeoprocessingString("(\"DATE\" > date '1998-01-01 00:00:00' AND \"DATE\" < date '1998-05-31 00:00:00')")
            geoprocessingParameters.inputs["Query"] = geoprocessingString

            // create and start geoprocessing job
            geoprocessingJob = geoprocessingTask.createJob(geoprocessingParameters)
            geoprocessingJob?.start()

            jobCoroutineScope.launch {
                geoprocessingJob?.progress?.collect { progress ->
                    Log.e("TAG", "PROGRESS: $progress")
                    geoprocessingJobProgress.value = progress
                }
            }

            Log.e("TAG", "JOB STARTED")

            showJobProgressDialog.value = true
            geoprocessingJob?.result()?.onSuccess {
                Log.e("TAG", "SUCCESS")
                showJobProgressDialog.value = false

                val hotspotMapImageLayer = geoprocessingJob?.result()?.getOrNull()?.mapImageLayer
                hotspotMapImageLayer?.opacity = 0.5f

                // add new layer to map
                if (hotspotMapImageLayer != null) {
                    mapViewState.value.arcGISMap.operationalLayers.add(hotspotMapImageLayer)
                }
            }?.onFailure { throwable ->
                Log.e("TAG", "FAILURE: ${throwable.message}")
                showJobProgressDialog.value = false
            }
        }

    }

    fun cancelGeoprocessingJob(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            geoprocessingJob?.cancel()
        }
    }

    fun convertMillisToString(millis: Long): String {
        val instant = Instant.ofEpochMilli(millis)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val date = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        return date.format(formatter)
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
