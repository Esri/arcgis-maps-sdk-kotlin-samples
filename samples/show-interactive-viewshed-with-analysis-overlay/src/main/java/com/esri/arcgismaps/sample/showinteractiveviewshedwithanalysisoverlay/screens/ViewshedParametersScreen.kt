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
import androidx.compose.runtime.Composable
import com.arcgismaps.analysis.visibility.ViewshedParameters

@Composable
fun ViewshedParametersScreen(
    viewshedParameters: ViewshedParameters,
    onObserverElevationChanged: (Float) -> Unit = {},
    onTargetHeightChanged: (Float) -> Unit = {},
    onMaxRadiusChanged: (Float) -> Unit = {},
    onFieldOfViewChanged: (Float) -> Unit = {},
    onHeadingChanged: (Float) -> Unit = {},
) {
    Column {
        // sliders
        ObserverElevationSlider(viewshedParameters.observerPosition!!.z!!.toFloat(), onObserverElevationChanged)
        TargetHeightSlider(viewshedParameters.targetHeight.toFloat(), onTargetHeightChanged)
        MaxRadiusSlider(viewshedParameters.maxRadius!!.toFloat(), onMaxRadiusChanged)
        FieldOfViewSlider(viewshedParameters.fieldOfView.toFloat(), onFieldOfViewChanged)
        HeadingSlider(viewshedParameters.heading.toFloat(), onHeadingChanged)
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
