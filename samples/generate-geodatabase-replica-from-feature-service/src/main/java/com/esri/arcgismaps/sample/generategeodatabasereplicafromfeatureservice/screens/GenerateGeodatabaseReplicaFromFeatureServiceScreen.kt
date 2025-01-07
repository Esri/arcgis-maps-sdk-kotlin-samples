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

package com.esri.arcgismaps.sample.generategeodatabasereplicafromfeatureservice.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.LoadStatus
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.generategeodatabasereplicafromfeatureservice.components.GenerateGeodatabaseReplicaFromFeatureServiceViewModel
import com.esri.arcgismaps.sample.generategeodatabasereplicafromfeatureservice.R
import com.esri.arcgismaps.sample.sampleslib.components.JobLoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app.
 */
@Composable
fun GenerateGeodatabaseReplicaFromFeatureServiceScreen(sampleName: String) {
    val application = LocalContext.current.applicationContext
    val mapViewModel: GenerateGeodatabaseReplicaFromFeatureServiceViewModel = viewModel()
    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        // retrieve the size of the Composable MapView
                        .onSizeChanged { size ->
                            mapViewModel.updateMapViewSize(size)
                        },
                    arcGISMap = mapViewModel.arcGISMap,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    mapViewProxy = mapViewModel.mapViewProxy,
                    onLayerViewStateChanged = {
                        // on launch, calculate the download area
                        if (mapViewModel.arcGISMap.loadStatus.value == LoadStatus.Loaded) {
                            mapViewModel.calculateDownloadArea()
                        }
                    },
                    onViewpointChangedForCenterAndScale = {
                        // recalculate the download area when viewpoint changes
                        if (mapViewModel.arcGISMap.loadStatus.value == LoadStatus.Loaded) {
                            mapViewModel.calculateDownloadArea()
                        }
                    },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            mapViewModel.resetMap()
                        },
                        enabled = mapViewModel.resetButtonEnabled
                    ) {
                        Text(text = getString(application, R.string.reset_map))
                    }

                    Button(
                        onClick = {
                            mapViewModel.generateGeodatabaseReplica()
                        },
                        enabled = mapViewModel.generateButtonEnabled
                    ) {
                        Text(text = getString(application, R.string.generate_button_text))
                    }
                }

                // display progress dialog while generating a geodatabase replica
                if (mapViewModel.showJobProgressDialog) {
                    JobLoadingDialog(
                        title = getString(application, R.string.dialog_title),
                        progress = mapViewModel.jobProgress,
                        cancelJobRequest = { mapViewModel.cancelOfflineGeodatabaseJob() }
                    )
                }

                // display a dialog if the sample encounters an error
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
