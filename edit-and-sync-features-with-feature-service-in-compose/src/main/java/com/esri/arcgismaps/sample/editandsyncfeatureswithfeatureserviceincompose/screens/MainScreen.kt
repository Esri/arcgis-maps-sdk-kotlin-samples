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

package com.esri.arcgismaps.sample.editandsyncfeatureswithfeatureserviceincompose.screens

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.editandsyncfeatureswithfeatureserviceincompose.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.JobLoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    // create a ViewModel to handle MapView interactions
    val mapViewModel: MapViewModel = viewModel()

    // use viewModel SnackbarHostState to set SnackbarHost in Scaffold
    val snackbarHostState = remember { mapViewModel.snackbarHostState }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        content = {
            // check generate offline map to see layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                MapView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    arcGISMap = mapViewModel.map,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    onSingleTapConfirmed = mapViewModel::onSingleTapConfirmed,
                    onVisibleAreaChanged = { newPolygon ->
                        mapViewModel.polygon = newPolygon
                    },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            mapViewModel.onClickActionButton()
                        },
                        enabled = mapViewModel.isActionButtonEnabled.value
                    ) {
                        Text(text = mapViewModel.actionButtonText)
                    }
                }

                // Display progress dialog while generating geodatabase
                if (mapViewModel.showGenerateGeodatabaseJobProgressDialog.value) {
                    JobLoadingDialog(
                        title = "Generating geodatabase...",
                        progress = mapViewModel.generateGeodatabaseJobProgress.intValue,
                        cancelJobRequest = { mapViewModel.cancelGenerateGeodatabaseJob() }
                    )
                }

                // Display progress dialog while syncing geodatabase
                if (mapViewModel.showSyncGeodatabaseJobProgressDialog.value) {
                    JobLoadingDialog(
                        title = "Syncing geodatabase...",
                        progress = mapViewModel.syncGeodatabaseJobProgress.intValue,
                        cancelJobRequest = { mapViewModel.cancelSyncGeodatabaseJob() }
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