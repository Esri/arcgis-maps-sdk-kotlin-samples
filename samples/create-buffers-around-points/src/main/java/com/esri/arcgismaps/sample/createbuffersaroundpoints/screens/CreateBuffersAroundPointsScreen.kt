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

package com.esri.arcgismaps.sample.createbuffersaroundpoints.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.geometry.Point
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.createbuffersaroundpoints.components.CreateBuffersAroundPointsViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app. Provides the map, controls for unioning/clearing
 * buffers, and a simple input dialog to enter buffer radius in miles when the user taps the map.
 */
@Composable
fun CreateBuffersAroundPointsScreen(sampleName: String) {
    val mapViewModel: CreateBuffersAroundPointsViewModel = viewModel()

    // Observe view model states
    val statusText by mapViewModel.statusText.collectAsStateWithLifecycle()
    val isInputVisible by mapViewModel.isInputDialogVisible.collectAsStateWithLifecycle()
    val shouldUnion by mapViewModel.shouldUnion.collectAsStateWithLifecycle()

    // Local state for the input value (string) and validation
    var radiusInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Column(modifier = Modifier.padding(padding)) {

                // Message dialog from the view model
                mapViewModel.messageDialogVM.apply {
                    if (dialogStatus) {
                        MessageDialog(
                            title = messageTitle,
                            description = messageDescription,
                            onDismissRequest = ::dismissDialog
                        )
                    }
                }

                // Overlay at the top center that shows the current status of interactions
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        text = statusText,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                // MapView: display the map and handle taps
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    graphicsOverlays = mapViewModel.graphicsOverlays,
                    onSingleTapConfirmed = { tapEvent ->
                        tapEvent.mapPoint?.let { mapPoint: Point ->
                            mapViewModel.onMapTapped(mapPoint)
                        }
                    }
                )


                // Controls row: union switch and clear button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Union", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = shouldUnion,
                            onCheckedChange = { checked -> mapViewModel.updateUnion(checked) }
                        )
                    }

                    Row {
                        OutlinedButton(onClick = { mapViewModel.clearAll() }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear buffers")
                            Text(text = " Clear", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }


            }

            // Buffer radius input dialog
            if (isInputVisible) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text(text = "Buffer radius (miles)") },
                    text = {
                        Column {
                            Text(text = "Enter a radius between 0 and 300 miles.")
                            TextField(
                                value = radiusInput,
                                onValueChange = { radiusInput = it },
                                label = { Text("Radius in miles") }
                            )
                        }
                    },
                    confirmButton = {
                        FilledTonalButton(onClick = {
                            // Parse the input and submit to the view model
                            val miles = radiusInput.toDoubleOrNull()
                            if (miles == null) {
                                // Show an error dialog via the view model
                                mapViewModel.messageDialogVM.showMessageDialog(
                                    "Invalid input",
                                    "Please enter a valid number"
                                )
                            } else {
                                mapViewModel.submitRadiusMiles(miles)
                                radiusInput = ""
                            }
                        }) {
                            Text("Done")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            // Cancel input
                            radiusInput = ""
                            mapViewModel.dismissInputDialog()
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    )
}
