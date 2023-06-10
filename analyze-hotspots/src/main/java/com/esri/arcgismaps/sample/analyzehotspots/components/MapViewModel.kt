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
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.tasks.geoprocessing.GeoprocessingJob
import com.arcgismaps.tasks.geoprocessing.GeoprocessingParameters
import com.arcgismaps.tasks.geoprocessing.GeoprocessingResult
import com.arcgismaps.tasks.geoprocessing.GeoprocessingTask
import com.arcgismaps.tasks.geoprocessing.geoprocessingparameters.GeoprocessingString
import com.esri.arcgismaps.sample.analyzehotspots.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MapViewModel(
    private val application: Application,
    private val sampleCoroutineScope: CoroutineScope
) : AndroidViewModel(application) {
    // set the MapView mutable stateflow
    val mapViewState = MutableStateFlow(MapViewState())

    // determinate job progress loading dialog
    val showJobProgressDialog = mutableStateOf(false)

    // determinate job progress percentage
    val geoprocessingJobProgress = mutableStateOf(0)

    // job used to run the geoprocessing task on a service
    private var geoprocessingJob: GeoprocessingJob? = null

    /**
     * Creates a [geoprocessingJob] with the default [GeoprocessingParameters]
     * and a custom query date range between [fromDate] & [toDate]
     */
    suspend fun createGeoprocessingJob(
        fromDate: String,
        toDate: String,
    ) {
        // a map image layer might be generated, clear previous results
        mapViewState.value.arcGISMap.operationalLayers.clear()

        // create and load geoprocessing task
        val geoprocessingTask = GeoprocessingTask(application.getString(R.string.service_url))
        geoprocessingTask.load().getOrElse {
            showErrorDialog(it.message.toString(), it.cause.toString())
        }

        // create parameters for geoprocessing job
        val geoprocessingParameters = geoprocessingTask.createDefaultParameters().getOrElse {
            showErrorDialog(it.message.toString(), it.cause.toString())
        } as GeoprocessingParameters

        val queryString = StringBuilder("(\"DATE\" > date '")
            .append(fromDate)
            .append(" 00:00:00' AND \"DATE\" < date '")
            .append(toDate)
            .append(" 00:00:00')")

        //geoprocessingParameters.inputs["Query"] = geoprocessingString
        geoprocessingParameters.inputs["Query"] = GeoprocessingString(queryString.toString())

        // create and start geoprocessing job
        geoprocessingJob = geoprocessingTask.createJob(geoprocessingParameters)

        runGeoprocessingJob()
    }

    /**
     * Starts the [geoprocessingJob], shows the progress dialog and
     * displays the result hotspot map image layer to the MapView
     */
    private suspend fun runGeoprocessingJob() {
        // display the progress dialog
        showJobProgressDialog.value = true
        // start the job
        geoprocessingJob?.start()
        // collect the job progress
        sampleCoroutineScope.launch {
            geoprocessingJob?.progress?.collect { progress ->
                // updates the job progress dialog
                geoprocessingJobProgress.value = progress
            }
        }
        // get the result of the job on completion
        geoprocessingJob?.result()?.onSuccess {
            // dismiss the progress dialog
            showJobProgressDialog.value = false
            // get the job's result
            val geoprocessingResult = geoprocessingJob?.result()?.getOrElse {
                showErrorDialog(it.message.toString(), it.cause.toString())
            } as GeoprocessingResult
            // resulted hotspot map image layer
            val hotspotMapImageLayer = geoprocessingResult.mapImageLayer?.apply {
                opacity = 0.5f
            } ?: return showErrorDialog("Result map image layer is null")

            // add new layer to map
            mapViewState.value.arcGISMap.operationalLayers.add(hotspotMapImageLayer)
        }?.onFailure { throwable ->
            showErrorDialog(throwable.message.toString(), throwable.cause.toString())
            showJobProgressDialog.value = false
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

    // error dialog status
    val errorDialogStatus = mutableStateOf(false)
    var errorTitle = ""
    var errorDescription = ""
    fun showErrorDialog(title: String, description: String = "") {
        errorTitle = title
        errorDescription = description
        errorDialogStatus.value = true
    }
}

/**
 * Data class that represents the MapView state
 */
data class MapViewState(
    var arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic),
    var viewpoint: Viewpoint = Viewpoint(
        center = Point(-13671170.0, 5693633.0, SpatialReference(wkid = 3857)),
        scale = 1e5
    ),
)
