package com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom slider implementation to be used by various viewshed slider options
 */
@Composable
fun ViewshedSlider(
    title: String,
    initialSliderValue: Float,
    sliderRangeValue: ClosedFloatingPointRange<Float>,
    units: String,
    functionChanged: (Float) -> Unit
) {
    var sliderValue by remember {
        mutableFloatStateOf(initialSliderValue)
    }
    Row {
        Text(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp).width(140.dp),
            text = title,
            fontSize = 15.sp
        )
        Slider(
            modifier = Modifier.width(180.dp),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                // update view model viewshed value
                functionChanged(sliderValue)
            },
            valueRange = sliderRangeValue
        )
        Text(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp).width(70.dp),
            text = sliderValue.toInt().toString() + units,
            fontSize = 15.sp,
            textAlign = TextAlign.Right
        )
    }
}
