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

package com.esri.arcgismaps.sample.showservicearea.screens

import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showservicearea.components.ShowServiceAreaViewModel
import com.esri.arcgismaps.sample.showservicearea.components.ShowServiceAreaViewModel.GraphicType

/**
 * Main screen layout for the Show Service Area sample app.
 * Displays a MapView and a bottom sheet with controls for adding facilities/barriers, setting time breaks, and solving the service area.
 */
@Composable
fun ShowServiceAreaScreen(sampleName: String) {
    val viewModel: ShowServiceAreaViewModel = viewModel()

    // Collect state flows from the ViewModel for Compose UI
    val selectedGraphicType by viewModel.selectedGraphicType.collectAsStateWithLifecycle()
    val firstTimeBreak by viewModel.firstTimeBreak.collectAsStateWithLifecycle()
    val secondTimeBreak by viewModel.secondTimeBreak.collectAsStateWithLifecycle()
    val isSolvingServiceArea by viewModel.isSolvingServiceArea.collectAsStateWithLifecycle()
    val isBottomSheetVisible by viewModel.isBottomSheetVisible.collectAsStateWithLifecycle()

    // Used to show/hide the bottom sheet
    var showBottomSheet by remember { mutableStateOf(false) }

    // Ensure bottom sheet state is synced with ViewModel
    LaunchedEffect(isBottomSheetVisible) {
        showBottomSheet = isBottomSheetVisible
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            // Show FAB to open the bottom sheet if not visible
            if (!showBottomSheet) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { viewModel.setBottomSheetVisible(true) }
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Show Options")
                }
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // MapView fills most of the screen, responds to taps for adding graphics
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = viewModel.arcGISMap,
                    graphicsOverlays = viewModel.graphicsOverlays,
                    mapViewProxy = viewModel.mapViewProxy,
                    onSingleTapConfirmed = { tapEvent ->
                        tapEvent.mapPoint?.let { mapPoint ->
                            // Add facility/barrier depending on selected mode
                            viewModel.handleMapTap(mapPoint)
                        }
                        // Dismiss bottom sheet on map interaction
                        viewModel.setBottomSheetVisible(false)
                    }
                )
                // The controls are in a bottom sheet, not in the column directly
            }

            // Show the bottom sheet for options
            BottomSheet(
                isVisible = showBottomSheet,
                sheetTitle = "Service Area Options",
                onDismissRequest = { viewModel.setBottomSheetVisible(false) }
            ) { columnScope ->
                ServiceAreaOptions(
                    selectedGraphicType = selectedGraphicType,
                    firstTimeBreak = firstTimeBreak,
                    secondTimeBreak = secondTimeBreak,
                    onGraphicTypeSelected = viewModel::setSelectedGraphicType,
                    onTimeBreaksChanged = viewModel::updateTimeBreaks,
                    onSolveServiceArea = {
                        viewModel.showServiceArea()
                        viewModel.setBottomSheetVisible(false)
                    },
                    onClearAll = {
                        viewModel.removeAllGraphics()
                        viewModel.setBottomSheetVisible(false)
                    }
                )
            }

            // Show a loading dialog while the service area is being solved
            if (isSolvingServiceArea) {
                LoadingDialog(loadingMessage = "Solving service area...")
            }

            // Show error dialog if needed
            viewModel.messageDialogVM.apply {
                if (dialogStatus) {
                    MessageDialog(
                        title = messageTitle,
                        description = messageDescription,
                        onDismissRequest = ::dismissDialog
                    )
                }
            }
        }
    )
}

/**
 * UI controls for service area options: select mode, set time breaks, solve, and clear.
 * This is displayed inside the bottom sheet.
 */
@Composable
fun ServiceAreaOptions(
    selectedGraphicType: GraphicType,
    firstTimeBreak: Int,
    secondTimeBreak: Int,
    onGraphicTypeSelected: (GraphicType) -> Unit,
    onTimeBreaksChanged: (Int, Int) -> Unit,
    onSolveServiceArea: () -> Unit,
    onClearAll: () -> Unit
) {
    // Local state for time break steppers
    var firstBreak by remember { mutableStateOf(firstTimeBreak) }
    var secondBreak by remember { mutableStateOf(secondTimeBreak) }

    Column(
        modifier = Modifier
            .wrapContentSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Segmented control for Facility/Barrier mode
        Text("Add graphic type:", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            GraphicType.values().forEachIndexed { index, type ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index, GraphicType.values().size),
                    onClick = { onGraphicTypeSelected(type) },
                    selected = selectedGraphicType == type
                ) {
                    Text(type.label)
                }
            }
        }
        // Time break steppers (as OutlinedButtons)
        Text("Time breaks (minutes):", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = {
                if (firstBreak > 1) {
                    firstBreak--
                    onTimeBreaksChanged(firstBreak, secondBreak)
                }
            }) { Text("-") }
            Text("First: $firstBreak", modifier = Modifier.padding(horizontal = 8.dp))
            OutlinedButton(onClick = {
                if (firstBreak < 15) {
                    firstBreak++
                    onTimeBreaksChanged(firstBreak, secondBreak)
                }
            }) { Text("+") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = {
                if (secondBreak > 1) {
                    secondBreak--
                    onTimeBreaksChanged(firstBreak, secondBreak)
                }
            }) { Text("-") }
            Text("Second: $secondBreak", modifier = Modifier.padding(horizontal = 8.dp))
            OutlinedButton(onClick = {
                if (secondBreak < 15) {
                    secondBreak++
                    onTimeBreaksChanged(firstBreak, secondBreak)
                }
            }) { Text("+") }
        }
        // Row of action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilledTonalButton(onClick = onSolveServiceArea) {
                Text("Service Area")
            }
            Button(onClick = onClearAll) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear")
                Text("Clear")
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewServiceAreaOptions() {
    SamplePreviewSurface {
        Surface {
            ServiceAreaOptions(
                selectedGraphicType = GraphicType.Facility,
                firstTimeBreak = 3,
                secondTimeBreak = 8,
                onGraphicTypeSelected = {},
                onTimeBreaksChanged = { _, _ -> },
                onSolveServiceArea = {},
                onClearAll = {}
            )
        }
    }
}
