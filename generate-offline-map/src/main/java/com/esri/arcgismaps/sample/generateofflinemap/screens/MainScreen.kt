/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.generateofflinemap.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.generateofflinemap.R
import com.esri.arcgismaps.sample.generateofflinemap.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.JobLoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {

    val application = LocalContext.current.applicationContext

    // Create a ViewModel to handle MapView interactions
    val mapViewModel: MapViewModel = viewModel()


    Scaffold(
        snackbarHost = { SnackbarHost(hostState = mapViewModel.snackbarHostState) },
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            // Column composable that wraps around MapView
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                MapView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        // Retrieve the size of the Composable MapView
                        .onSizeChanged { size ->
                            mapViewModel.mapViewSize = size
                        },
                    arcGISMap = mapViewModel.map,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    mapViewProxy = mapViewModel.mapViewProxy,
                    onViewpointChangedForCenterAndScale = {
                        mapViewModel.calculateDownloadOfflineArea()
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            when (mapViewModel.takeMapOfflineButtonText) {
                                application.getString(R.string.take_map_offline) -> mapViewModel.createOfflineMapJob()
                                else -> mapViewModel.resetButtonClick()
                            }
                        },
                    ) {
                        when (mapViewModel.takeMapOfflineButtonText) {
                            application.getString(R.string.take_map_offline) -> Text(
                                text = application.getString(
                                    R.string.take_map_offline
                                )
                            )

                            else -> Text(text = application.getString(R.string.reset_map))
                        }
                    }
                }

                // Display progress dialog while generating an offline map
                if (mapViewModel.showJobProgressDialog.value) {
                    JobLoadingDialog(
                        title = "Generating offline map...",
                        progress = mapViewModel.offlineMapJobProgress.intValue,
                        cancelJobRequest = { mapViewModel.cancelOfflineMapJob() }
                    )
                }

                // Display a dialog if the sample encounters an error
                mapViewModel.messageDialogVM.apply {
                    if (dialogStatus) {
                        MessageDialog(
                            title = messageTitle,
                            description = messageDescription,
                            onDismissRequest = ::dismissDialog
                        )
                    }
                }
            }
        }
    )
}
