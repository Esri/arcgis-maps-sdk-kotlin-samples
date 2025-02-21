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

package com.esri.arcgismaps.sample.displaydevicelocationwithfusedlocationdatasource.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.rememberLocationDisplay
import com.esri.arcgismaps.sample.displaydevicelocationwithfusedlocationdatasource.components.DisplayDeviceLocationWithFusedLocationDataSourceViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.google.android.gms.location.Priority
import kotlin.math.roundToInt

/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayDeviceLocationWithFusedLocationDataSourceScreen(sampleName: String) {
    val locationDisplay = rememberLocationDisplay()
    val mapViewModel: DisplayDeviceLocationWithFusedLocationDataSourceViewModel = viewModel()
    // On first composition, initialize the sample.
    var isViewmodelInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(isViewmodelInitialized) {
        if (!isViewmodelInitialized) {
            mapViewModel.initialize(locationDisplay)
            isViewmodelInitialized = true
        }
    }
    // Set up the bottom sheet controls
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf("High accuracy") }
    var interval by remember { mutableFloatStateOf(1F) }
    Scaffold(topBar = { SampleTopAppBar(title = sampleName) }, content = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
        ) {
            Box {
                MapView(
                    modifier = Modifier
                        .fillMaxSize(),
                    arcGISMap = mapViewModel.arcGISMap,
                    locationDisplay = locationDisplay
                )
            }
            // Show bottom sheet with override parameter options
            if (showBottomSheet) {
                ModalBottomSheet(
                    modifier = Modifier.wrapContentSize(),
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState
                ) {

                    ExposedDropdownMenuBox(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp),
                        expanded = isExpanded,
                        onExpandedChange = { isExpanded = !isExpanded }
                    ) {
                        TextField(
                            label = { Text("Priority") },
                            value = priority,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                            modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false }
                        ) {

                            DropdownMenuItem(text = { Text("High accuracy") }, onClick = {
                                isExpanded = false
                                priority = "High accuracy"
                                mapViewModel.onPriorityChanged(Priority.PRIORITY_HIGH_ACCURACY)
                            })

                            HorizontalDivider()

                            DropdownMenuItem(text = { Text("Balanced power accuracy") }, onClick = {
                                isExpanded = false
                                priority = "Balanced power accuracy"
                                mapViewModel.onPriorityChanged(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                            })

                            HorizontalDivider()

                            DropdownMenuItem(text = { Text("Low power") }, onClick = {
                                isExpanded = false
                                priority = "Low power"
                                mapViewModel.onPriorityChanged(Priority.PRIORITY_LOW_POWER)
                            })

                            HorizontalDivider()

                            DropdownMenuItem(text = { Text("Passive") }, onClick = {
                                isExpanded = false
                                priority = "Passive"
                                mapViewModel.onPriorityChanged(Priority.PRIORITY_PASSIVE)
                            })
                        }

                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = CenterVertically
                    ) {
                        Text(text = "Set desired interval for location updates:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 12.dp))
                        Text(text = interval.roundToInt().toString() + " sec", modifier = Modifier.padding(end = 12.dp))
                    }
                    Slider(
                        value = interval,
                        onValueChange = {
                            interval = it
                            mapViewModel.onIntervalChanged(it.toLong())
                        }, valueRange = 0f..30f,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp),
                        steps = 29
                    )
                }
            }
        }


        mapViewModel.messageDialogVM.apply {
            if (dialogStatus) {
                MessageDialog(
                    title = messageTitle, description = messageDescription, onDismissRequest = ::dismissDialog
                )
            }
        }
    },
        // Floating action button to show the parameter overrides bottom sheet
        floatingActionButton = {
            if (!showBottomSheet) {
                FloatingActionButton(modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { showBottomSheet = true }) {
                    Icon(
                        imageVector = Icons.Filled.Settings, contentDescription = "Show parameter overrides menu"
                    )
                }
            }
        })
}
