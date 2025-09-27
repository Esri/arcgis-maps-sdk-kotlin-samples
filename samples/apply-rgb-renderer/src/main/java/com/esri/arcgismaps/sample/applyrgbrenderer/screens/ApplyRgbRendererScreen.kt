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

package com.esri.arcgismaps.sample.applyrgbrenderer.screens

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.applyrgbrenderer.components.ApplyRgbRendererViewModel
import com.esri.arcgismaps.sample.applyrgbrenderer.components.ApplyRgbRendererViewModel.StretchType
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.launch

/**
 * Main screen layout for the Apply RGB renderer sample.
 *
 * The UI presents a MapView and a bottom sheet with options to choose the stretch
 * parameter type and adjust parameters. Pressing the "Update Renderer" button
 * applies the RGB renderer to the raster layer (if available).
 */
@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyRgbRendererScreen(sampleName: String) {
    val viewModel: ApplyRgbRendererViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    // UI states for dropdowns
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    var stretchDropdownExpanded by remember { mutableStateOf(false) }
    var minColorDropdownExpanded by remember { mutableStateOf(false) }
    var maxColorDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {

                // Map view — all ArcGIS map objects are supplied by the ViewModel
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = viewModel.arcGISMap,
                    onVisibleAreaChanged = { isBottomSheetVisible = false }
                )

                // Bottom sheet toggle and options are provided below via the sample BottomSheet
            }

            BottomSheet(
                isVisible = isBottomSheetVisible,
                sheetTitle = "RGB Renderer Options",
                onDismissRequest = { isBottomSheetVisible = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Stretch type selector
                    ExposedDropdownMenuBox(
                        expanded = stretchDropdownExpanded,
                        onExpandedChange = { stretchDropdownExpanded = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        TextField(
                            value = viewModel.selectedStretchType.name,
                            onValueChange = {},
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            label = { Text("Stretch Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stretchDropdownExpanded) }
                        )
                        println("expanded = $stretchDropdownExpanded")
                        ExposedDropdownMenu(
                            expanded = stretchDropdownExpanded,
                            onDismissRequest = { stretchDropdownExpanded = false }) {
                            StretchType.entries.forEachIndexed { index, type ->
                                println("FOO type = ${type.name}")
                                DropdownMenuItem(text = { Text(type.name) }, onClick = {
                                    viewModel.updateStretchType(type)
                                    stretchDropdownExpanded = false
                                })
                                // show a divider between dropdown menu options
                                if (index < StretchType.entries.lastIndex) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
//
//                    @Suppress("DEPRECATION")
//                    Divider()
//
//                    // Conditional UI for selected stretch
//                    when (viewModel.selectedStretchType) {
//                        StretchType.HistogramEqualization -> {
//                            Text(
//                                "Histogram Equalization has no parameters.",
//                                modifier = Modifier.padding(8.dp)
//                            )
//                        }
//
//                        StretchType.MinMax -> {
//                            Text(
//                                "Min-Max Stretch Parameters",
//                                style = MaterialTheme.typography.titleMedium
//                            )
//
//                            // Min color selector
//                            ExposedDropdownMenuBox(
//                                expanded = minColorDropdownExpanded,
//                                onExpandedChange = {
//                                    minColorDropdownExpanded = !minColorDropdownExpanded
//                                },
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(horizontal = 8.dp)
//                            ) {
//                                val minLabel = viewModel.presetColors[viewModel.minColorIndex].first
//                                TextField(
//                                    value = minLabel,
//                                    onValueChange = {},
//                                    readOnly = true,
//                                    label = { Text("Min Color") },
//                                    trailingIcon = {
//                                        ExposedDropdownMenuDefaults.TrailingIcon(
//                                            expanded = minColorDropdownExpanded
//                                        )
//                                    }
//                                )
//                                ExposedDropdownMenu(
//                                    expanded = minColorDropdownExpanded,
//                                    onDismissRequest = { minColorDropdownExpanded = false }) {
//                                    viewModel.presetColors.forEachIndexed { index, pair ->
//                                        DropdownMenuItem(text = { Text(pair.first) }, onClick = {
//                                            viewModel.updateMinColorIndex(index)
//                                            minColorDropdownExpanded = false
//                                        })
//                                    }
//                                }
//                            }
//
//                            // Max color selector
//                            ExposedDropdownMenuBox(
//                                expanded = maxColorDropdownExpanded,
//                                onExpandedChange = {
//                                    maxColorDropdownExpanded = !maxColorDropdownExpanded
//                                },
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(horizontal = 8.dp)
//                            ) {
//                                val maxLabel = viewModel.presetColors[viewModel.maxColorIndex].first
//                                TextField(
//                                    value = maxLabel,
//                                    onValueChange = {},
//                                    readOnly = true,
//                                    label = { Text("Max Color") },
//                                    trailingIcon = {
//                                        ExposedDropdownMenuDefaults.TrailingIcon(
//                                            expanded = maxColorDropdownExpanded
//                                        )
//                                    }
//                                )
//                                ExposedDropdownMenu(
//                                    expanded = maxColorDropdownExpanded,
//                                    onDismissRequest = { maxColorDropdownExpanded = false }) {
//                                    viewModel.presetColors.forEachIndexed { index, pair ->
//                                        DropdownMenuItem(text = { Text(pair.first) }, onClick = {
//                                            viewModel.updateMaxColorIndex(index)
//                                            maxColorDropdownExpanded = false
//                                        })
//                                    }
//                                }
//                            }
//                        }
//
//                        StretchType.PercentClip -> {
//                            Text(
//                                "Percent Clip Stretch",
//                                style = MaterialTheme.typography.titleMedium
//                            )
//
//                            Text(
//                                "Min: ${viewModel.percentClipMin.toInt()}%",
//                                modifier = Modifier.padding(start = 8.dp)
//                            )
//                            Slider(
//                                value = viewModel.percentClipMin.toFloat(),
//                                onValueChange = { newVal ->
//                                    viewModel.updatePercentClip(
//                                        newVal.toDouble(),
//                                        viewModel.percentClipMax
//                                    )
//                                },
//                                valueRange = 0f..100f
//                            )
//
//                            Text(
//                                "Max: ${viewModel.percentClipMax.toInt()}%",
//                                modifier = Modifier.padding(start = 8.dp)
//                            )
//                            Slider(
//                                value = viewModel.percentClipMax.toFloat(),
//                                onValueChange = { newVal ->
//                                    viewModel.updatePercentClip(
//                                        viewModel.percentClipMin,
//                                        newVal.toDouble()
//                                    )
//                                },
//                                valueRange = 0f..100f
//                            )
//                        }
//
//                        StretchType.StandardDeviation -> {
//                            Text(
//                                "Standard Deviation Stretch",
//                                style = MaterialTheme.typography.titleMedium
//                            )
//                            Text(
//                                "Factor: ${
//                                    String.format(
//                                        "%.2f",
//                                        viewModel.standardDeviationFactor
//                                    )
//                                }", modifier = Modifier.padding(start = 8.dp)
//                            )
//                            Slider(
//                                value = viewModel.standardDeviationFactor.toFloat(),
//                                onValueChange = { newVal ->
//                                    viewModel.updateStandardDeviationFactor(newVal.toDouble())
//                                },
//                                valueRange = 0f..16f
//                            )
//                        }
//                    }
//
//                    Divider()
//
//                    Row(
//                        horizontalArrangement = Arrangement.spacedBy(8.dp),
//                        verticalAlignment = Alignment.CenterVertically,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(8.dp)
//                    ) {
//                        OutlinedButton(
//                            onClick = { isBottomSheetVisible = false },
//                            modifier = Modifier.weight(1f)
//                        ) {
//                            Text("Dismiss")
//                        }
//
//                        Button(onClick = {
//                            coroutineScope.launch { viewModel.applyRgbRenderer() }
//                        }, modifier = Modifier.weight(1f)) {
//                            Text("Update Renderer")
//                        }
//                    }
                }
                }

            // Show message dialogs produced by the ViewModel
            viewModel.messageDialogVM.apply {
                if (dialogStatus) {
                    MessageDialog(
                        title = messageTitle,
                        description = messageDescription,
                        onDismissRequest = ::dismissDialog
                    )
                }
            }
        },
        floatingActionButton = {
            // Floating action toggles the bottom sheet
            FloatingActionButton(onClick = { isBottomSheetVisible = true }) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Options")
            }
        }
    )
}

@Composable
fun PreviewApplyRgbRendererScreen() {
    SamplePreviewSurface {
        ApplyRgbRendererScreen(sampleName = "Apply RGB Renderer (Preview)")
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@androidx.compose.ui.tooling.preview.Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
fun Preview() {
    PreviewApplyRgbRendererScreen()
}
