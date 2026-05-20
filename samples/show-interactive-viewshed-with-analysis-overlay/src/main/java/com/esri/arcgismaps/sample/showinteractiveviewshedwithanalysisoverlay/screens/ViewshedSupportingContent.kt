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
package com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.components.ViewshedUiState

/**
 * Screen containing UI controls to modify the viewshed parameters.
 */
@Composable
fun ViewshedSupportingContent(
    uiState: ViewshedUiState,
    onObserverElevationChanged: (Float) -> Unit,
    onTargetHeightChanged: (Float) -> Unit,
    onMaxRadiusChanged: (Float) -> Unit,
    onFieldOfViewChanged: (Float) -> Unit,
    onHeadingChanged: (Float) -> Unit,
    onElevationSamplingIntervalChanged: (Double) -> Unit
) {
    Column {
        ObserverElevationSlider(
            sliderValue = uiState.observerElevation.toFloat(),
            onObserverElevationChanged
        )
        TargetHeightSlider(sliderValue = uiState.targetHeight.toFloat(), onTargetHeightChanged)
        MaxRadiusSlider(sliderValue = uiState.maxRadius.toFloat(), onMaxRadiusChanged)
        FieldOfViewSlider(sliderValue = uiState.fieldOfView.toFloat(), onFieldOfViewChanged)
        HeadingSlider(sliderValue = uiState.heading.toFloat(), onHeadingChanged)
        ElevationSamplingIntervalButtons(
            initialValue = uiState.elevationSamplingInterval,
            onElevationSamplingIntervalChanged
        )
    }
}

/**
 * Custom slider implementation, used for several viewshed parameter controls.
 */
@Composable
fun ViewshedSlider(
    title: String,
    sliderValue: Float,
    sliderRangeValue: ClosedFloatingPointRange<Float>,
    units: String,
    onSliderValueChanged: (Float) -> Unit
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
            value = sliderValue,
            onValueChange = onSliderValueChanged,
            valueRange = sliderRangeValue
        )
    }
}

@Composable
private fun ObserverElevationSlider(sliderValue: Float, onObserverElevationChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Observer Elevation",
        sliderValue = sliderValue,
        sliderRangeValue = 2f..200f,
        units = " m",
        onSliderValueChanged = onObserverElevationChanged
    )
}

@Composable
private fun TargetHeightSlider(sliderValue: Float, onTargetHeightChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Target Height",
        sliderValue = sliderValue,
        sliderRangeValue = 2f..1000f,
        units = " m",
        onSliderValueChanged = onTargetHeightChanged
    )
}

@Composable
private fun MaxRadiusSlider(sliderValue: Float, onMaxRadiusChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Maximum Radius",
        sliderValue = sliderValue,
        sliderRangeValue = 2500f..20000f,
        units = " m",
        onSliderValueChanged = onMaxRadiusChanged
    )
}

@Composable
private fun FieldOfViewSlider(sliderValue: Float, onFieldOfViewChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Field of View",
        sliderValue = sliderValue,
        sliderRangeValue = 5f..360f,
        units = "°",
        onSliderValueChanged = onFieldOfViewChanged
    )
}

@Composable
private fun HeadingSlider(sliderValue: Float, onHeadingChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Heading",
        sliderValue = sliderValue,
        sliderRangeValue = 0f..360f,
        units = "°",
        onSliderValueChanged = onHeadingChanged
    )
}

/**
 * Use radio buttons to allow one of 3 values to be selected for Elevation Sampling Interval.
 */
@Composable
private fun ElevationSamplingIntervalButtons(
    initialValue: Double?,
    onElevationSamplingIntervalChanged: (Double) -> Unit
) {
    val radioOptions = listOf("0", "10", "20")
    val initialIndex = when (initialValue) {
        10.0 -> 1
        20.0 -> 2
        else -> 0
    }
    val selectedOption = radioOptions[initialIndex]
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Elevation Sampling Interval (m)")
        Row(Modifier.selectableGroup()) {
            radioOptions.forEach { text ->
                Row(
                    Modifier.selectable(
                        selected = (text == selectedOption),
                        onClick = { onElevationSamplingIntervalChanged(text.toDouble()) })
                ) {
                    RadioButton(
                        selected = (text == selectedOption),
                        onClick = {
                            onElevationSamplingIntervalChanged(text.toDouble())
                        }
                    )
                    Text(
                        modifier = Modifier.padding(top = 10.dp, end = 10.dp),
                        text = text,
                        textAlign = TextAlign.Left
                    )
                }
            }
        }
    }
}
