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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.applystretchrenderer.components.ApplyStretchRendererViewModel
import com.esri.arcgismaps.sample.applystretchrenderer.components.StretchType
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

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
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                arcGISMap = mapViewModel.arcGISMap,
                mapViewProxy = mapViewModel.mapViewProxy,
                onDown = {
                    isBottomSheetVisible = false
                    mapViewModel.dismissChanges()
                },
            )

            BottomSheet(
                isVisible = isBottomSheetVisible,
                sheetTitle = "Stretch Renderer Settings",
                onDismissRequest = {
                    isBottomSheetVisible = false
                    mapViewModel.dismissChanges()
                }
            ) {
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
                            MinMaxSettings(
                                minValue = minValue,
                                maxValue = maxValue,
                                onMinValueChange = mapViewModel::updateMinValue,
                                onMaxValueChange = mapViewModel::updateMaxValue
                            )
                        }

                        StretchType.PercentClip -> {
                            PercentClipSettings(
                                percentMin = percentMin,
                                percentMax = percentMax,
                                onPercentMinChange = mapViewModel::updatePercentMin,
                                onPercentMaxChange = mapViewModel::updatePercentMax
                            )
                        }

                        StretchType.StandardDeviation -> {
                            StdDevSettings(
                                stdDevFactor = stdDevFactor,
                                onStdDevFactorChange = mapViewModel::updateStdDeviationFactor
                            )
                        }
                    }
                    Button(
                        onClick = {
                            mapViewModel.resetAllChanges()
                            isBottomSheetVisible = false
                        }
                    ) {
                        Text("Reset all changes")
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
        },
        floatingActionButton = {
            if (!isBottomSheetVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { isBottomSheetVisible = true }
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Show Renderer Settings"
                    )
                }
            }
        }
    )
}

// UI for Min-Max stretch parameters
@Composable
fun MinMaxSettings(
    minValue: Double,
    maxValue: Double,
    onMinValueChange: (Double) -> Unit,
    onMaxValueChange: (Double) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..255f
) {
    var range by remember { mutableStateOf(minValue.toFloat()..maxValue.toFloat()) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Min-Max Parameters", style = MaterialTheme.typography.titleMedium)

        Text(text = "Min Value: ${range.start.toInt()}  Max Value: ${range.endInclusive.toInt()}")

        RangeSlider(
            value = range,
            onValueChange = { newRange ->
                if ( newRange.start < newRange.endInclusive ) {
                    range = newRange
                    onMinValueChange(newRange.start.toDouble())
                    onMaxValueChange(newRange.endInclusive.toDouble())
                }

            },
            valueRange = valueRange,
            steps = 254 // steps between 0 and 255
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "${valueRange.start.toInt()}")
            Text(text = "${valueRange.endInclusive.toInt()}")
        }
    }
}

// UI for Percent Clip stretch parameters
@Composable
fun PercentClipSettings(
    percentMin: Double,
    percentMax: Double,
    onPercentMinChange: (Double) -> Unit,
    onPercentMaxChange: (Double) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f
) {
    var range by remember { mutableStateOf(percentMin.toFloat()..percentMax.toFloat()) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Percent Clip Parameters", style = MaterialTheme.typography.titleMedium)

        // Percent min slider (0 .. percentMax)
        Text(text = " Min %: ${range.start.toInt()}  Max %: ${range.endInclusive.toInt()}")
        RangeSlider(
            value = range,
            onValueChange = { newRange ->
                // Ensure min is always less than max
                if (newRange.start < newRange.endInclusive) {
                    range = newRange
                    onPercentMinChange(newRange.start.toDouble())
                    onPercentMaxChange(newRange.endInclusive.toDouble())
                }
            },
            valueRange = valueRange,
            steps = 99 // steps between 0 and 100
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "${valueRange.start.toInt()}")
            Text(text = "${valueRange.endInclusive.toInt()}")
        }
    }
}

// UI for Standard Deviation stretch parameters
@Composable
fun StdDevSettings(
    stdDevFactor: Double,
    onStdDevFactorChange: (Double) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Standard Deviation Parameters", style = MaterialTheme.typography.titleMedium)

        // Factor slider (0.25 .. 4.0)
        Text(text = "Factor: %.2f".format(stdDevFactor))
        Slider(
            value = stdDevFactor.toFloat(),
            onValueChange = { value -> onStdDevFactorChange(value.toDouble()) },
            valueRange = 0.25f..4.0f
        )
    }
}
