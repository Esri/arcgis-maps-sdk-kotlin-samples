package com.esri.arcgismaps.sample.showexploratoryviewshedfrompointinscene.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Custom slider implementation to be used by various viewshed slider options
 */
@Composable
fun ViewshedSlider(
    title: String,
    initialSliderValue: Float,
    sliderRangeValue: ClosedFloatingPointRange<Float>,
    functionChanged: (Float) -> Unit
) {
    var sliderValue by remember {
        mutableFloatStateOf(initialSliderValue)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title)
            Text(text = sliderValue.toInt().toString())
        }
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                // update view model viewshed value
                functionChanged(sliderValue)
            },
            valueRange = sliderRangeValue
        )
    }
}
