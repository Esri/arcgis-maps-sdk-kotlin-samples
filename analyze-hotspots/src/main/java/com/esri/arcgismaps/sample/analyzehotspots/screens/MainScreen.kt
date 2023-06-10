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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.esri.arcgismaps.sample.analyzehotspots.components.ComposeMapView
import com.esri.arcgismaps.sample.analyzehotspots.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.JobLoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.launch

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String, application: Application) {
    // coroutineScope that will be cancelled when this call leaves the composition
    val sampleCoroutineScope = rememberCoroutineScope()
    // create a ViewModel to handle MapView interactions
    val mapViewModel = remember { MapViewModel(application, sampleCoroutineScope) }

    Scaffold(
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
                            sampleCoroutineScope.launch {
                                mapViewModel.apply {
                                    // create and run a geoprocessing task using date range
                                    createGeoprocessingJob(
                                        fromDate = convertMillisToString(fromDateInMillis),
                                        toDate = convertMillisToString(toDateInMillis),
                                    )
                                }
                            }
                        } else {
                            mapViewModel.showErrorDialog(
                                title = "Error creating job",
                                description = "Invalid date range selected"
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

                // display a dialog if the sample encounters an error
                MessageDialog(
                    title = mapViewModel.errorTitle,
                    description = mapViewModel.errorDescription,
                    showDialog = mapViewModel.errorDialogStatus.value,
                    onDismissRequest = {
                        mapViewModel.errorDialogStatus.value = false
                    }
                )
            }
        })
}
