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

@Composable
fun ViewshedParametersScreen(
    onTargetHeightChanged: (Float) -> Unit = {},
    onMaxRadiusChanged: (Float) -> Unit = {},
    onFieldOfViewChanged: (Float) -> Unit = {},
    onHeadingChanged: (Float) -> Unit = {},
) {
    Column {
        // sliders
        TargetHeightSlider(onTargetHeightChanged)
        MaxRadiusSlider(onMaxRadiusChanged)
        FieldOfViewSlider(onFieldOfViewChanged)
        HeadingSlider(onHeadingChanged)
    }
}

@Composable
private fun TargetHeightSlider(onTargetHeightChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Target Height:",
        initialSliderValue = 20f,
        sliderRangeValue = 2f..1000f,
        units = " m",
        functionChanged = onTargetHeightChanged
    )
}

@Composable
private fun MaxRadiusSlider(onMaxRadiusChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Maximum Radius:",
        initialSliderValue = 8000f,
        sliderRangeValue = 2500f..20000f,
        units = " m",
        functionChanged = onMaxRadiusChanged
    )
}

@Composable
private fun FieldOfViewSlider(onFieldOfViewChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Field of View:",
        initialSliderValue = 150f,
        sliderRangeValue = 5f..360f,
        units = "°",
        functionChanged = onFieldOfViewChanged
    )
}

@Composable
private fun HeadingSlider(onHeadingChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Heading:",
        initialSliderValue = 10f,
        sliderRangeValue = 0f..360f,
        units = "°",
        functionChanged = onHeadingChanged
    )
}
