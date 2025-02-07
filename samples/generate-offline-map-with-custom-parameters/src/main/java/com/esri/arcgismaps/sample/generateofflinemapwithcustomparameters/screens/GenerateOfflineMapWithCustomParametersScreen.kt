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

package com.esri.arcgismaps.sample.generateofflinemapwithcustomparameters.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.generateofflinemapwithcustomparameters.components.GenerateOfflineMapWithCustomParametersViewModel
import com.esri.arcgismaps.sample.sampleslib.components.JobLoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateOfflineMapWithCustomParametersScreen(sampleName: String) {
    // Create a ViewModel to handle MapView interactions
    val mapViewModel: GenerateOfflineMapWithCustomParametersViewModel = viewModel()

    // Set up the bottom sheet controls
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    fun setBottomSheetVisibility(isVisible: Boolean) {
        showBottomSheet = isVisible
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = mapViewModel.snackbarHostState) },
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    // Disable taps when job progress dialog is shown
                    .clickable(enabled = !mapViewModel.showJobProgressDialog, onClick = { })
            ) {
                MapView(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // Retrieve the size of the Composable MapView
                    .onSizeChanged { size ->
                        mapViewModel.updateMapViewSize(size)
                    },
                    arcGISMap = mapViewModel.arcGISMap,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    mapViewInteractionOptions = MapViewInteractionOptions(isRotateEnabled = false),
                    mapViewProxy = mapViewModel.mapViewProxy,
                    onLayerViewStateChanged = {
                        mapViewModel.calculateDownloadOfflineArea()
                    },
                    onViewpointChangedForCenterAndScale = {
                        mapViewModel.calculateDownloadOfflineArea()
                    }
                )
                // Show bottom sheet with override parameter options
                if (showBottomSheet) {
                    ModalBottomSheet(
                        modifier = Modifier.wrapContentSize(),
                        onDismissRequest = { showBottomSheet = false },
                        sheetState = sheetState
                    ) {
                        OverrideParameters(
                            defineParameters = mapViewModel::defineGenerateOfflineMapParameters,
                            setBottomSheetVisibility = ::setBottomSheetVisibility
                        )
                    }
                }
                // Display progress dialog while generating an offline map
                if (mapViewModel.showJobProgressDialog) {
                    JobLoadingDialog(title = "Generating offline map...",
                        progress = mapViewModel.offlineMapJobProgress,
                        cancelJobRequest = { mapViewModel.cancelOfflineMapJob() })
                }

                // Display a dialog if the sample encounters an error
                mapViewModel.messageDialogVM.apply {
                    if (dialogStatus) {
                        MessageDialog(
                            title = messageTitle, description = messageDescription, onDismissRequest = ::dismissDialog
                        )
                    }
                }
                if (mapViewModel.showResetButton) {
                    Button(
                        onClick = mapViewModel::reset,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Reset to online map")
                    }
                }
            }
        },
        // Floating action button to show the parameter overrides bottom sheet
        floatingActionButton = {
            if (!showBottomSheet && !mapViewModel.showResetButton) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { showBottomSheet = true }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Parameter overrides"
                    )
                }
            }
        })
}

@Composable
fun OverrideParameters(
    defineParameters: (Int, Int, Int, Boolean, Boolean, Int, Boolean) -> Unit,
    setBottomSheetVisibility: (Boolean) -> Unit
) {
    // Collection of parameter overrides to set in this composable
    var minScale by remember { mutableFloatStateOf(15f) }
    var maxScale by remember { mutableFloatStateOf(20f) }
    var extentBufferDistance by remember { mutableFloatStateOf(150f) }
    var includeSystemValves by remember { mutableStateOf(false) }
    var includeServiceConnections by remember { mutableStateOf(false) }
    var minHydrantFlowRate by remember { mutableFloatStateOf(500f) }
    var cropToWaterPipeExtent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .wrapContentSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "Override parameters",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Text(text = "Adjust basemap", style = MaterialTheme.typography.labelLarge)
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = CenterVertically
            ) {
                Text(text = "Min scale level:", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = minScale,
                    // Don't let the min scale exceed the max scale
                    onValueChange = {
                        minScale = it
                        if (minScale >= maxScale) {
                            maxScale = minScale + 1
                        }
                    },
                    valueRange = 0f..22f, modifier = Modifier.weight(1f),
                    steps = 21
                )
                Text(text = "${minScale.toInt()}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = CenterVertically
            ) {
                Text(text = "Max scale level:", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = maxScale,
                    // Don't let the max scale exceed the min scale
                    onValueChange = {
                        maxScale = it
                        if (maxScale <= minScale) {
                            minScale = maxScale - 1
                        }
                    },
                    valueRange = 0f..23f, modifier = Modifier.weight(1f),
                    steps = 22
                )
                Text(text = "${maxScale.toInt()}")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = CenterVertically,
            ) {
                Text(text = "Extent buffer distance:", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = extentBufferDistance,
                    onValueChange = { extentBufferDistance = it },
                    valueRange = 0f..500f,
                    modifier = Modifier.weight(1f)
                )
                Text(text = "${extentBufferDistance.toInt()}m")
            }
        HorizontalDivider(modifier = Modifier.padding(8.dp))
            Text(text = "Include layers", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = CenterVertically
            ) {
                Checkbox(checked = includeSystemValves, onCheckedChange = { includeSystemValves = it })
                Text(text = "System valves")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = CenterVertically
            ) {
                Checkbox(checked = includeServiceConnections, onCheckedChange = { includeServiceConnections = it })
                Text(text = "Service connections")
            }
        HorizontalDivider(modifier = Modifier.padding(8.dp))
        Text(text = "Filter feature layer", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = CenterVertically
        ) {
            Text(text = "Min hydrant flow rate:", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = minHydrantFlowRate,
                onValueChange = { minHydrantFlowRate = it },
                valueRange = 0f..2000f,
                modifier = Modifier.weight(1f)
            )
            Text(text = "${minHydrantFlowRate.toInt()} GPM")
        }
        HorizontalDivider(modifier = Modifier.padding(8.dp))
        Text(text = "Crop layers to extent", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.wrapContentSize(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = CenterVertically
        ) {
            Checkbox(checked = cropToWaterPipeExtent, onCheckedChange = { cropToWaterPipeExtent = it })
            Text(text = "Water pipes")
        }
        Button(
            onClick = {
                // Call defineParameters in the view model with the parameter overrides
                defineParameters(
                    minScale.toInt(),
                    maxScale.toInt(),
                    extentBufferDistance.toInt(),
                    includeSystemValves,
                    includeServiceConnections,
                    minHydrantFlowRate.toInt(),
                    cropToWaterPipeExtent
                )
                setBottomSheetVisibility(false)
            }, modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Generate offline map")
        }
    }
}
