/*
 * COPYRIGHT 1995-2026 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */
package com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arcgismaps.analysis.visibility.ViewshedParameters

@Composable
fun ViewshedParametersScreen(
    viewshedParameters: ViewshedParameters,
    onObserverElevationChanged: (Float) -> Unit,
    onTargetHeightChanged: (Float) -> Unit,
    onMaxRadiusChanged: (Float) -> Unit,
    onFieldOfViewChanged: (Float) -> Unit,
    onHeadingChanged: (Float) -> Unit,
    onElevationSamplingIntervalChanged: (Double) -> Unit
) {
    Column {
        // sliders
        ObserverElevationSlider(viewshedParameters.observerPosition!!.z!!.toFloat(), onObserverElevationChanged)
        TargetHeightSlider(viewshedParameters.targetHeight.toFloat(), onTargetHeightChanged)
        MaxRadiusSlider(viewshedParameters.maxRadius!!.toFloat(), onMaxRadiusChanged)
        FieldOfViewSlider(viewshedParameters.fieldOfView.toFloat(), onFieldOfViewChanged)
        HeadingSlider(viewshedParameters.heading.toFloat(), onHeadingChanged)
        ElevationSamplingIntervalButtons(viewshedParameters.elevationSamplingInterval, onElevationSamplingIntervalChanged)
    }
}

@Composable
private fun ObserverElevationSlider(initialValue: Float, onObserverElevationChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Observer Elevation",
        initialSliderValue = initialValue,
        sliderRangeValue = 2f..200f,
        units = " m",
        functionChanged = onObserverElevationChanged
    )
}

@Composable
private fun TargetHeightSlider(initialValue: Float, onTargetHeightChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Target Height",
        initialSliderValue = initialValue,
        sliderRangeValue = 2f..1000f,
        units = " m",
        functionChanged = onTargetHeightChanged
    )
}

@Composable
private fun MaxRadiusSlider(initialValue: Float, onMaxRadiusChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Maximum Radius",
        initialSliderValue = initialValue,
        sliderRangeValue = 2500f..20000f,
        units = " m",
        functionChanged = onMaxRadiusChanged
    )
}

@Composable
private fun FieldOfViewSlider(initialValue: Float, onFieldOfViewChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Field of View",
        initialSliderValue = initialValue,
        sliderRangeValue = 5f..360f,
        units = "°",
        functionChanged = onFieldOfViewChanged
    )
}

@Composable
private fun HeadingSlider(initialValue: Float, onHeadingChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Heading",
        initialSliderValue = initialValue,
        sliderRangeValue = 0f..360f,
        units = "°",
        functionChanged = onHeadingChanged
    )
}

@Composable
private fun ElevationSamplingIntervalButtons(initialValue: Double?, onElevationSamplingIntervalChanged: (Double) -> Unit) {
    val radioOptions = listOf("0", "10", "20")
    val initialIndex = when (initialValue) {
        10.0 -> 1
        20.0 -> 2
        else -> 0
    }
    val selectedOption = remember { mutableStateOf(radioOptions[initialIndex]) }
    Row(
        Modifier.selectableGroup()
    ) {
        Text(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp).width(200.dp),
            text = "Elevation Sampling Interval(m)",
            fontSize = 15.sp
        )
        radioOptions.forEach { text ->
            RadioButton(
                selected = (text == selectedOption.value),
                onClick = {
                    selectedOption.value = text
                    onElevationSamplingIntervalChanged(text.toDouble())
                }
            )
            Text(
                modifier = Modifier.padding(top = 10.dp, end = 10.dp),
                text = text,
                fontSize = 15.sp,
                textAlign = TextAlign.Left
            )
        }
    }
}
