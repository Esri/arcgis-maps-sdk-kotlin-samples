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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    onMinMaxValuesChange: (Int, Double, Double) -> Unit,
    onPercentClipMinValueChange: (Double) -> Unit,
    onPercentClipMaxValueChange: (Double) -> Unit,
    onStdDevFactorChange: (Double) -> Unit,
    onResetAllChanges: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(12.dp),
            text = "Choose stretch type and configure parameters:",
            style = MaterialTheme.typography.titleMedium
        )

        // Stretch type dropdown menu
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

        // Present the parameters that are appropriate for the selected stretch type
        when (uiState.stretchType) {
            StretchType.MinMax -> {
                MinMaxSettings(
                    minValues = uiState.minMaxMinValues,
                    maxValues = uiState.minMaxMaxValues,
                    onMinMaxValuesChange = onMinMaxValuesChange
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

        // A button to reset all parameters to their initial values
        Button(
            onClick = onResetAllChanges
        ) {
            Text("Reset all changes")
        }
    }
}

/**
 * UI for Min-Max stretch parameters.
 */
@Composable
fun MinMaxSettings(
    minValues: List<Double>,
    maxValues: List<Double>,
    onMinMaxValuesChange: (Int, Double, Double) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Range sliders for red, green and blue bands
        MinMaxSlider(
            index = 0,
            bandName = "RED",
            minValues = minValues,
            maxValues = maxValues,
            onMinMaxValuesChange = onMinMaxValuesChange
        )
        MinMaxSlider(
            index = 1,
            bandName = "GREEN",
            minValues = minValues,
            maxValues = maxValues,
            onMinMaxValuesChange = onMinMaxValuesChange
        )
        MinMaxSlider(
            index = 2,
            bandName = "BLUE",
            minValues = minValues,
            maxValues = maxValues,
            onMinMaxValuesChange = onMinMaxValuesChange
        )

        // Display start and end values underneath each end of the sliders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "0")
            Text(text = "255")
        }
    }
}

/**
 * UI for Min-Max values for one band (R, G or B).
 */
@Composable
fun MinMaxSlider(
    index: Int,
    bandName: String,
    minValues: List<Double>,
    maxValues: List<Double>,
    onMinMaxValuesChange: (Int, Double, Double) -> Unit
) {
    val sliderValueRange = 0f..255f
    val currentValueRange = minValues[index].toFloat()..maxValues[index].toFloat()

    // Current min-max values
    Text(text = "$bandName  Min: ${minValues[index].toInt()}  Max: ${maxValues[index].toInt()}")

    // Range slider allows handles to be dragged to set min and max values
    RangeSlider(
        value = currentValueRange,
        onValueChange = { newRange ->
            if (newRange.start < newRange.endInclusive) {
                onMinMaxValuesChange(index, newRange.start.toDouble(), newRange.endInclusive.toDouble())
            }
        },
        valueRange = sliderValueRange,
        steps = 254 // steps between 0 and 255
    )
}

/**
 * UI for Percent Clip stretch parameters.
 */
@Composable
fun PercentClipSettings(
    minValue: Double,
    maxValue: Double,
    onMinValueChange: (Double) -> Unit,
    onMaxValueChange: (Double) -> Unit
) {
    // The full range of the slider is 0..100 %
    val sliderValueRange = 0f..100f
    // maxValue needs to be subtracted from 100 to get its current position on the slider
    val currentValueRange = minValue.toFloat()..100 - maxValue.toFloat()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Header line and current min-max values
        Text("Percent Clip Parameters", style = MaterialTheme.typography.titleMedium)
        Text(text = " Min %: ${minValue.toInt()}  Max %: ${maxValue.toInt()}")

        // Range slider allows handles to be dragged to set min and max values
        RangeSlider(
            value = currentValueRange,
            onValueChange = { newRange ->
                // newRange.endInclusive needs to be subtracted from 100 to get the value to pass to
                // onMaxValueChange
                onMinValueChange(newRange.start.toDouble())
                onMaxValueChange(100 - newRange.endInclusive.toDouble())
            },
            valueRange = sliderValueRange,
            steps = 99 // steps between 0 and 100
        )
    }
}

/**
 * UI for Standard Deviation stretch parameters.
 */
@Composable
fun StdDevSettings(
    stdDevFactor: Double,
    onStdDevFactorChange: (Double) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val sliderValueRange = 0.25f..4.0f

        // Header line and current factor value
        Text("Standard Deviation Parameters", style = MaterialTheme.typography.titleMedium)
        Text(text = "Factor: %.2f".format(stdDevFactor))

        // Factor slider (0.25 ... 4.0)
        Slider(
            value = stdDevFactor.toFloat(),
            onValueChange = { value -> onStdDevFactorChange(value.toDouble()) },
            valueRange = sliderValueRange
        )

        // Display start and end values underneath each end of the slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "%.2f".format(sliderValueRange.start))
            Text(text = "%.2f".format(sliderValueRange.endInclusive))
        }
    }
}
