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

package com.esri.arcgismaps.sample.setsurfaceplacementmode.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.setsurfaceplacementmode.components.SetSurfacePlacementModeViewModel
import com.esri.arcgismaps.sample.setsurfaceplacementmode.components.SetSurfacePlacementModeViewModel.DrapedMode
import java.util.Locale

@Composable
fun SetSurfacePlacementModeScreen(sampleName: String) {
    val sceneViewModel: SetSurfacePlacementModeViewModel = viewModel()

    var isBottomSheetVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            if (!isBottomSheetVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { isBottomSheetVisible = true }
                ) { Icon(Icons.Filled.Settings, contentDescription = "Show options") }
            }
        },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                SceneView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISScene = sceneViewModel.arcGISScene,
                    graphicsOverlays = sceneViewModel.graphicsOverlays
                )

                BottomSheet(
                    isVisible = isBottomSheetVisible,
                    sheetTitle = "Surface Placement Options",
                    onDismissRequest = { isBottomSheetVisible = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Compact row: label on the left, segmented control on the right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Draped mode:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            SingleChoiceSegmentedButtonRow {
                                DrapedMode.entries.forEach { mode ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = mode.ordinal, count = DrapedMode.entries.size),
                                        onClick = { sceneViewModel.updateDrapedMode(mode) },
                                        selected = sceneViewModel.drapedMode == mode
                                    ) {
                                        Text(mode.name)
                                    }
                                }
                            }
                        }

                        // Compact row: label on the left, slider on the right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Z-value: ${String.format(Locale.getDefault(),"%.1f", sceneViewModel.zValue)} m",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Slider(
                                value = sceneViewModel.zValue.toFloat(),
                                onValueChange = { newValue -> sceneViewModel.updateZValue(newValue) },
                                valueRange = sceneViewModel.zMin.toFloat()..sceneViewModel.zMax.toFloat()
                            )
                        }
                    }
                }
            }

            sceneViewModel.messageDialogVM.apply {
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
