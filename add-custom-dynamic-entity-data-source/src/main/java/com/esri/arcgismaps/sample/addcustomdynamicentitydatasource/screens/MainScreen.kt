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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.addcustomdynamicentitydatasource.R
import com.esri.arcgismaps.sample.addcustomdynamicentitydatasource.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    // Create a ViewModel to handle MapView interactions.
    val mapViewModel: MapViewModel = viewModel()
    // Keep track of the state of a connect/disconnect button.
    var isConnected by remember { mutableStateOf(true) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Show the current connection status.
                Row {
                    Text(text = "Connection status: ")
                    Text(
                        text = mapViewModel.connectionStatusString,
                        color = if (mapViewModel.connectionStatusString.contains("Connected"))
                            Color.Green else MaterialTheme.colorScheme.onBackground
                    )
                }
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    mapViewProxy = mapViewModel.mapViewProxy,
                    arcGISMap = mapViewModel.arcGISMap,
                    onSingleTapConfirmed = mapViewModel::identify,
                    content = {
                        (mapViewModel.identifiedDynamicEntity as? GeoElement)?.let { dynamicEntityAsGeoElement ->
                            Callout(
                                modifier = Modifier.wrapContentSize(),
                                geoElement = dynamicEntityAsGeoElement
                            ) {
                                Column(Modifier.padding(4.dp)) {
                                    Text(
                                        text = mapViewModel.identifiedDynamicEntityAttributeString
                                    )
                                }
                            }
                        }
                    }
                )
                Button(
                    onClick = {
                        if (isConnected) {
                            mapViewModel.dynamicEntityDataSourceDisconnect()
                        } else {
                            mapViewModel.dynamicEntityDataSourceConnect()
                        }
                        isConnected = !isConnected
                    }) {
                    Text(
                        text = stringResource(if (!isConnected) R.string.connect else R.string.disconnect)
                    )
                }
            }
        }
    )
}
