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
    onHeadingChanged: (Float) -> Unit = {},
) {
    Column {
        // sliders
        HeadingSlider(onHeadingChanged)
    }
}

@Composable
private fun HeadingSlider(onHeadingChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Heading",
        initialSliderValue = 82f,
        sliderRangeValue = 0f..360f,
        functionChanged = onHeadingChanged
    )
}
