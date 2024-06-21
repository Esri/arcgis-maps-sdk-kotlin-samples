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

package com.esri.arcgismaps.sample.addcustomdynamicentitydatasource.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.addcustomdynamicentitydatasource.R
import com.esri.arcgismaps.sample.addcustomdynamicentitydatasource.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    // create a ViewModel to handle MapView interactions
    val mapViewModel: MapViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Box {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    mapViewProxy = mapViewModel.mapViewProxy,
                    arcGISMap = mapViewModel.arcGISMap,
                    onSingleTapConfirmed = mapViewModel::identify
                )
                Text(
                    text = "Connection status" + mapViewModel.connectionStatusString,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(color = androidx.compose.ui.graphics.Color.Blue)
                )
                // Create a button to allow the user to connect/disconnect the data source.
                var isConnected by remember { mutableStateOf(true) }
                Button(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    onClick = {
                        if (isConnected) {
                            mapViewModel.feedProviderOnDisconnect()
                        } else {
                            mapViewModel.feedProviderOnConnect()
                        }
                        isConnected = !isConnected
                    }) {
                    Text(
                        text = if (!isConnected) {
                            stringResource(R.string.connect)
                        } else {
                            stringResource(R.string.disconnect)
                        }
                    )
                }
                // display a MessageDialog with identify information
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
