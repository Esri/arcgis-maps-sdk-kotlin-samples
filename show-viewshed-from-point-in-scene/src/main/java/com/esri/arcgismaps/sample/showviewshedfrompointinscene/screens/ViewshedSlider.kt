package com.esri.arcgismaps.sample.showviewshedfrompointinscene.screens

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
import androidx.compose.ui.unit.dp

/**
 * Custom slider implementation to be used by various viewshed slider options
 */
@Composable
fun ViewshedSlider(
    title: String,
    intialSliderValue: Float,
    sliderRangeValue: ClosedFloatingPointRange<Float>,
    functionChanged: (Float) -> Unit
) {
    var sliderValue by remember {
        mutableFloatStateOf(intialSliderValue)
    }
    Row {
        Text(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp).width(150.dp),
            text = title
        )
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                // update view model viewshed value
                functionChanged(sliderValue)
            },
            valueRange = sliderRangeValue
        )
        Text(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp).size(40.dp),
            text = sliderValue.toInt().toString()
        )
    }
}
