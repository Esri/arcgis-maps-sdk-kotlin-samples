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

package com.esri.arcgismaps.sample.applystretchrenderer.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.esri.arcgismaps.sample.applystretchrenderer.components.ApplyStretchRendererViewModel
import com.esri.arcgismaps.sample.applystretchrenderer.components.StretchType
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface

/**
 * Main screen layout for the sample app.
 */
@Composable
fun ApplyStretchRendererScreen(sampleName: String) {
    val mapViewModel: ApplyStretchRendererViewModel = viewModel()

    // UI state
    val selectedStretchType by mapViewModel.selectedStretchType.collectAsStateWithLifecycle()
    val minValue by mapViewModel.minValue.collectAsStateWithLifecycle()
    val maxValue by mapViewModel.maxValue.collectAsStateWithLifecycle()
    val percentMin by mapViewModel.percentMin.collectAsStateWithLifecycle()
    val percentMax by mapViewModel.percentMax.collectAsStateWithLifecycle()
    val stdDevFactor by mapViewModel.stdDeviationFactor.collectAsStateWithLifecycle()

    var isBottomSheetVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize(),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    onDown = { isBottomSheetVisible = false },
//                    onLayerViewStateChanged = {
//                        // Set initial viewpoint based on raster full extent once MapView enters composition
//                        mapViewModel.setInitialViewpointIfNeeded()
//                    }
                )

                BottomSheet(
                    isVisible = isBottomSheetVisible,
                    sheetTitle = "Stretch Renderer Settings",
                    onDismissRequest = { isBottomSheetVisible = false }
                ) { columnScope ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Choose stretch type and configure parameters.",
                            style = MaterialTheme.typography.labelLarge
                        )

                        // Stretch type dropdown
                        DropDownMenuBox(
                            textFieldValue = when (selectedStretchType) {
                                StretchType.MinMax -> mapViewModel.stretchTypeOptions[0]
                                StretchType.PercentClip -> mapViewModel.stretchTypeOptions[1]
                                StretchType.StandardDeviation -> mapViewModel.stretchTypeOptions[2]
                            },
                            textFieldLabel = "Stretch Type",
                            dropDownItemList = mapViewModel.stretchTypeOptions,
                            onIndexSelected = mapViewModel::updateStretchTypeByIndex
                        )

                        when (selectedStretchType) {
                            StretchType.MinMax -> {
                                Text("Min-Max Parameters", style = MaterialTheme.typography.titleMedium)

                                // Min value slider (0 .. max-1)
                                Text(text = "Min Value: ${minValue.toInt()}")
                                Slider(
                                    value = minValue.toFloat(),
                                    onValueChange = { value -> mapViewModel.updateMinValue(value.toDouble()) },
                                    valueRange = 0f..(maxValue.toFloat() - 1f)
                                )

                                // Max value slider ((min+1) .. 255)
                                Text(text = "Max Value: ${maxValue.toInt()}")
                                Slider(
                                    value = maxValue.toFloat(),
                                    onValueChange = { value -> mapViewModel.updateMaxValue(value.toDouble()) },
                                    valueRange = (minValue.toFloat() + 1f)..255f
                                )
                            }

                            StretchType.PercentClip -> {
                                Text("Percent Clip Parameters", style = MaterialTheme.typography.titleMedium)

                                // Percent min slider (0 .. percentMax)
                                Text(text = "Min (%): ${percentMin.toInt()}")
                                Slider(
                                    value = percentMin.toFloat(),
                                    onValueChange = { value -> mapViewModel.updatePercentMin(value.toDouble()) },
                                    valueRange = 0f..percentMax.toFloat()
                                )

                                // Percent max slider (percentMin .. 100)
                                Text(text = "Max (%): ${percentMax.toInt()}")
                                Slider(
                                    value = percentMax.toFloat(),
                                    onValueChange = { value -> mapViewModel.updatePercentMax(value.toDouble()) },
                                    valueRange = percentMin.toFloat()..100f
                                )
                            }

                            StretchType.StandardDeviation -> {
                                Text("Standard Deviation Parameters", style = MaterialTheme.typography.titleMedium)

                                // Factor slider (0.25 .. 4.0)
                                Text(text = "Factor: ${String.format("%.2f", stdDevFactor)}")
                                Slider(
                                    value = stdDevFactor.toFloat(),
                                    onValueChange = { value -> mapViewModel.updateStdDeviationFactor(value.toDouble()) },
                                    valueRange = 0.25f..4.0f
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(onClick = { isBottomSheetVisible = false }) {
                                Text("Dismiss")
                            }
                            Button(onClick = {
                                mapViewModel.updateRenderer()
                                isBottomSheetVisible = false
                            }
                            ) {
                                Text("Update Renderer")
                            }
                        }
                    }
                }

                // Error dialog
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
        },
        floatingActionButton = {
            if (!isBottomSheetVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { isBottomSheetVisible = true }
                ) { androidx.compose.material3.Icon(Icons.Filled.Settings, contentDescription = "Show Renderer Settings") }
            }
        }
    )
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewApplyStretchRendererScreen() {
    SamplePreviewSurface {
        Surface { Text("Apply stretch renderer preview") }
    }
}
