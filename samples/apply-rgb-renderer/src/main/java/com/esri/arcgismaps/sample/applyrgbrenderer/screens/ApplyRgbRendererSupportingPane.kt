/* Copyright 2026 Esri
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.esri.arcgismaps.sample.applyrgbrenderer.components.RgbRendererUiState
import com.esri.arcgismaps.sample.applyrgbrenderer.components.StretchType
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox

/**
 * Supporting pane content for the sample.
 */
@Composable
internal fun ApplyRgbRendererSupportingPane(
    uiState: RgbRendererUiState,
    onStretchTypeChange: (StretchType) -> Unit,
    onMinMaxMinValueChange: (Double) -> Unit,
    onMinMaxMaxValueChange: (Double) -> Unit,
    onPercentClipMinValueChange: (Double) -> Unit,
    onPercentClipMaxValueChange: (Double) -> Unit,
    onStdDevFactorChange: (Double) -> Unit,
    onResetAllChanges: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose stretch type and configure parameters:",
            style = MaterialTheme.typography.titleMedium
        )

        // Stretch type dropdown
        val stretchTypeOptions = listOf("MinMax", "Percent Clip", "Std Deviation")
        DropDownMenuBox(
            textFieldValue = when (uiState.stretchType) {
                StretchType.MinMax -> stretchTypeOptions[0]
                StretchType.PercentClip -> stretchTypeOptions[1]
                StretchType.StandardDeviation -> stretchTypeOptions[2]
            },
            textFieldLabel = "Stretch Type",
            dropDownItemList = stretchTypeOptions,
            onIndexSelected = { index ->
                onStretchTypeChange(
                    when (index) {
                        0 -> StretchType.MinMax
                        1 -> StretchType.PercentClip
                        else -> StretchType.StandardDeviation
                    }
                )
            }
        )

        when (uiState.stretchType) {
            StretchType.MinMax -> {
                MinMaxSettings(
                    minValue = uiState.minMaxMinValue,
                    maxValue = uiState.minMaxMaxValue,
                    onMinValueChange = onMinMaxMinValueChange,
                    onMaxValueChange = onMinMaxMaxValueChange
                )
            }

            StretchType.PercentClip -> {
                PercentClipSettings(
                    minValue = uiState.percentClipMinValue,
                    maxValue = uiState.percentClipMaxValue,
                    onMinValueChange = onPercentClipMinValueChange,
                    onMaxValueChange = onPercentClipMaxValueChange
                )
            }

            StretchType.StandardDeviation -> {
                StdDevSettings(
                    stdDevFactor = uiState.stdDevFactor,
                    onStdDevFactorChange = onStdDevFactorChange
                )
            }
        }
        Button(
            onClick = onResetAllChanges
        ) {
            Text("Reset all changes")
        }
    }
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
    var range = minValue.toFloat()..maxValue.toFloat()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Min-Max Parameters", style = MaterialTheme.typography.titleMedium)

        Text(text = "Min Value: ${range.start.toInt()}  Max Value: ${range.endInclusive.toInt()}")

        RangeSlider(
            value = range,
            onValueChange = { newRange ->
                if (newRange.start < newRange.endInclusive) {
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

// UI for Standard Deviation stretch parameters
@Composable
fun StdDevSettings(
    stdDevFactor: Double,
    onStdDevFactorChange: (Double) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Standard Deviation Parameters", style = MaterialTheme.typography.titleMedium)

        // Factor slider (0.25 ... 4.0)
        Text(text = "Factor: %.2f".format(stdDevFactor))
        Slider(
            value = stdDevFactor.toFloat(),
            onValueChange = { value -> onStdDevFactorChange(value.toDouble()) },
            valueRange = 0.25f..4.0f
        )
    }
}

// UI for Percent Clip stretch parameters
@Composable
fun PercentClipSettings(
    minValue: Double,
    maxValue: Double,
    onMinValueChange: (Double) -> Unit,
    onMaxValueChange: (Double) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Percent Clip Parameters", style = MaterialTheme.typography.titleMedium)

        PercentClipSlider(
            title = "Min",
            sliderValue = minValue,
            units = " %",
            onSliderValueChange = onMinValueChange
        )

        PercentClipSlider(
            title = "Max",
            sliderValue = maxValue,
            units = " %",
            onSliderValueChange = onMaxValueChange
        )
    }
}

/**
 * Custom slider implementation, used for clip percentages.
 */
@Composable
fun PercentClipSlider(
    title: String,
    sliderValue: Double,
    units: String,
    onSliderValueChange: (Double) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title)
            Text(text = sliderValue.toInt().toString() + units)
        }
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = sliderValue.toFloat(),
            onValueChange = { value -> onSliderValueChange(value.toDouble()) },
            valueRange = 0f..50f
        )
    }
}
