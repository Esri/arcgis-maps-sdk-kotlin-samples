/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.showviewshedfrompointonmap.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showviewshedfrompointonmap.components.ShowViewshedFromPointOnMapViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun ShowViewshedFromPointOnMapScreen(sampleName: String) {
    val mapViewModel: ShowViewshedFromPointOnMapViewModel = viewModel()

    // Observe geoprocessing state from the view model
    val isGeoprocessingInProgress by mapViewModel.isGeoprocessingInProgress.collectAsState()

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
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    onSingleTapConfirmed = mapViewModel::onMapTapped,
                    mapViewProxy = mapViewModel.mapviewProxy,
                    graphicsOverlays = listOf(
                        mapViewModel.inputGraphicsOverlay,
                        mapViewModel.bufferGraphicsOverlay,
                        mapViewModel.resultGraphicsOverlay
                    )
                )
            }

            mapViewModel.messageDialogVM.apply {
                if (dialogStatus) {
                    MessageDialog(
                        title = messageTitle,
                        description = messageDescription,
                        onDismissRequest = ::dismissDialog
                    )
                }
            }

            // If geoprocessing is running, show a loading dialog
            if (isGeoprocessingInProgress) {
                LoadingDialog(loadingMessage = "Calculating viewshed…")
            }
        }
    )
}
