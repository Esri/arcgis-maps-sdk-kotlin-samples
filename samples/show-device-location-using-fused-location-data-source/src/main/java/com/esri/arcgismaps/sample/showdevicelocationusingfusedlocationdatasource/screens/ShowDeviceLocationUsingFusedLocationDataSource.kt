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

package com.esri.arcgismaps.sample.showdevicelocationusingfusedlocationdatasource.screens

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showdevicelocationusingfusedlocationdatasource.components.ShowDeviceLocationUsingFusedLocationDataSourceViewModel
import com.google.android.gms.location.Priority
import kotlin.math.roundToInt

/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDeviceLocationUsingFusedLocationDataSource(sampleName: String, locationPermissionGranted: Boolean) {

    val mapViewModel: ShowDeviceLocationUsingFusedLocationDataSourceViewModel = viewModel()
    // On first composition, initialize the sample.
    var isViewmodelInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(isViewmodelInitialized) {
        if (!isViewmodelInitialized && locationPermissionGranted) {
            mapViewModel.initialize(locationDisplay)
            isViewmodelInitialized = true
        }
    }
    // Set up the bottom sheet controls
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    // list of configurable location priority modes
    val priorityModes = mapOf(
        Priority.PRIORITY_HIGH_ACCURACY to "High accuracy",
        Priority.PRIORITY_BALANCED_POWER_ACCURACY to "Balanced power accuracy",
        Priority.PRIORITY_LOW_POWER to "Low power",
        Priority.PRIORITY_PASSIVE to "Passive"
    )
    var currentPriority by remember { mutableIntStateOf(priorityModes.keys.first()) }
    var currentInterval by remember { mutableFloatStateOf(1F) }

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
                    DropDownMenuBox(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp),
                        textFieldLabel = "Priority",
                        textFieldValue = priorityModes[currentPriority].toString(),
                        dropDownItemList = priorityModes.values.toList(),
                        onIndexSelected = { index ->
                            currentPriority = priorityModes.keys.toList()[index]
                            mapViewModel.onPriorityChanged(currentPriority)
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = CenterVertically
                    ) {
                        Text(text = "Set desired interval for location updates:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 12.dp))
                        Text(text = currentInterval.roundToInt().toString() + " sec", modifier = Modifier.padding(end = 12.dp))
                    }
                    Slider(
                        value = currentInterval,
                        onValueChange = { valueChanged ->
                            currentInterval = valueChanged
                            mapViewModel.onIntervalChanged(valueChanged.toLong())
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
                        imageVector = Icons.Filled.Settings, contentDescription = "Show fused location options"
                    )
                }
            }
        })
}
