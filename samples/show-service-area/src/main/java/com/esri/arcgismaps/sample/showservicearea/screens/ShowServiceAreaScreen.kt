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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDialog
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showservicearea.components.ShowServiceAreaViewModel
import com.esri.arcgismaps.sample.showservicearea.components.ShowServiceAreaViewModel.GraphicType
import com.esri.arcgismaps.sample.showservicearea.components.ShowServiceAreaViewModel.TimeBreaks

/**
 * Main screen layout for the Show Service Area sample app.
 */
@Composable
fun ShowServiceAreaScreen(sampleName: String) {
    val viewModel: ShowServiceAreaViewModel = viewModel()

    // Collect state flows from the ViewModel for Compose UI
    val selectedGraphicType by viewModel.selectedGraphicType.collectAsStateWithLifecycle()
    val timeBreaks by viewModel.timeBreaks.collectAsStateWithLifecycle()
    val isSolvingServiceArea by viewModel.isSolvingServiceArea.collectAsStateWithLifecycle()

    // Dialog state for showing the time break dialog
    var showTimeBreakDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
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
                            viewModel.onSingleTap(mapPoint)
                        }
                    }
                )
                // Controls area at the bottom
                ServiceAreaControls(
                    selectedGraphicType = selectedGraphicType,
                    onGraphicTypeSelected = viewModel::updateSelectedGraphicType,
                    timeBreaks = timeBreaks,
                    onShowTimeBreakDialog = { showTimeBreakDialog = true },
                    onSolveServiceArea = viewModel::showServiceArea,
                    onClearAll = viewModel::removeAllGraphics
                )
            }

            // Time breaks dialog
            if (showTimeBreakDialog) {
                TimeBreakDialog(
                    initialTimeBreaks = timeBreaks,
                    onApply = { first, second ->
                        viewModel.updateTimeBreaks(first, second)
                        showTimeBreakDialog = false
                    },
                    onCancel = { showTimeBreakDialog = false }
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
 * Controls for the service area workflow, shown at the bottom of the screen.
 * Includes: segmented button for mode, row of action buttons.
 */
@Composable
fun ServiceAreaControls(
    selectedGraphicType: GraphicType,
    onGraphicTypeSelected: (GraphicType) -> Unit,
    timeBreaks: TimeBreaks,
    onShowTimeBreakDialog: () -> Unit,
    onSolveServiceArea: () -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 10.dp, horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Segmented control for Facility/Barrier mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                GraphicType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index, GraphicType.entries.size),
                        onClick = { onGraphicTypeSelected(type) },
                        selected = selectedGraphicType == type
                    ) {
                        Text(type.label)
                    }
                }
            }
        }
        // Row of action buttons: Set time breaks, Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onShowTimeBreakDialog) {
                Text("Set time breaks: ${timeBreaks.first}, ${timeBreaks.second}")
            }
            OutlinedButton(onClick = onClearAll) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear")
                Text("Clear")
            }
        }
        Button(onClick = onSolveServiceArea) { Text("Solve Service Area") }
    }
}

/**
 * Dialog for setting time break values with sliders.
 */
@Composable
fun TimeBreakDialog(
    initialTimeBreaks: TimeBreaks,
    onApply: (first: Int, second: Int) -> Unit,
    onCancel: () -> Unit
) {
    var firstBreak by remember { mutableIntStateOf(initialTimeBreaks.first) }
    var secondBreak by remember { mutableIntStateOf(initialTimeBreaks.second) }
    SampleDialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Set Time Breaks", style = MaterialTheme.typography.titleMedium)
            TimeBreakSlider(
                label = "First time break",
                value = firstBreak,
                valueRange = 1..15,
                onValueChange = { firstBreak = it }
            )
            TimeBreakSlider(
                label = "Second time break",
                value = secondBreak,
                valueRange = 1..15,
                onValueChange = { secondBreak = it }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(Modifier.padding(4.dp))
                Button(onClick = { onApply(firstBreak, secondBreak) }) {
                    Text("Apply")
                }
            }
        }
    }
}

/**
 * Slider row for setting a time break value, with label and value display.
 */
@Composable
fun TimeBreakSlider(
    label: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Text("$label: $value min", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = valueRange.last - valueRange.first - 1
        )
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewServiceAreaControls() {
    SamplePreviewSurface {
        Surface {
            ServiceAreaControls(
                selectedGraphicType = GraphicType.Facility,
                onGraphicTypeSelected = {},
                timeBreaks = TimeBreaks(3, 8),
                onShowTimeBreakDialog = {},
                onSolveServiceArea = {},
                onClearAll = {}
            )
        }
    }
}
