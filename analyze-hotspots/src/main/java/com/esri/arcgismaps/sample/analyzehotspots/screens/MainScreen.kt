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

package com.esri.arcgismaps.sample.analyzehotspots.screens

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.esri.arcgismaps.sample.analyzehotspots.components.ComposeMapView
import com.esri.arcgismaps.sample.analyzehotspots.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.JobLoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleSnackbarHostState
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.components.showMessage

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String, application: Application) {
    // create a ViewModel to handle MapView interactions
    val mapViewModel = remember { MapViewModel(application) }
    // default snackbar host state for this composition
    val snackbarHostState = remember { SnackbarHostState() }
    // coroutineScope that will be cancelled when this call leaves the composition
    val sampleCoroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SampleSnackbarHostState(snackbarHostState) },
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            // sample app content layout
            Column(modifier = Modifier.fillMaxSize().padding(it)) {
                // composable function that wraps the MapView
                ComposeMapView(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    mapViewModel = mapViewModel
                )
                // bottom layout with a button to display analyze hotspot options
                BottomAppContent(
                    // date range selected to analyze
                    analyzeHotspotsRange = { fromDateInMillis, toDateInMillis ->
                        if (fromDateInMillis != null && toDateInMillis != null) {
                            mapViewModel.apply {
                                // create and run a geoprocessing task using date range
                                analyzeHotspots(
                                    fromDateInMillis = convertMillisToString(0),
                                    toDateInMillis = convertMillisToString(toDateInMillis),
                                    jobCoroutineScope = sampleCoroutineScope
                                )
                            }
                        } else {
                            showMessage(
                                scope = sampleCoroutineScope,
                                snackbarHostState = snackbarHostState,
                                message = "Invalid date range selected"
                            )
                        }
                    },
                )
                // display progress dialog while analyzing hotspots
                JobLoadingDialog(
                    title = "Analyzing hotspots...",
                    showDialog = mapViewModel.showJobProgressDialog.value,
                    progress = mapViewModel.geoprocessingJobProgress.value,
                    cancelJobRequest = { mapViewModel.cancelGeoprocessingJob(sampleCoroutineScope) }
                )
            }
        })
}
