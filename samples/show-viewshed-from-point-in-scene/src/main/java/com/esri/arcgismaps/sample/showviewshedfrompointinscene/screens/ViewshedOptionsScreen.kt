/* Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.showviewshedfrompointinscene.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

/**
 * Viewshed options screen for sliders and checkboxes
 */
@Composable
fun ViewshedOptionsScreen(
    onHeadingChanged: (Float) -> Unit = {},
    onPitchChanged: (Float) -> Unit = {},
    onHorizontalAngleChanged: (Float) -> Unit = {},
    onVerticalAngleChanged: (Float) -> Unit = {},
    onMinDistanceChanged: (Float) -> Unit = {},
    onMaxDistanceChanged: (Float) -> Unit = {},
    isFrustumVisible: (Boolean) -> Unit = {},
    isAnalysisVisible: (Boolean) -> Unit = {}
) {
    Column {
        // sliders
        HeadingSlider(onHeadingChanged)
        PitchSlider(onPitchChanged)
        HorizontalAngleSlider(onHorizontalAngleChanged)
        VerticalAngleSlider(onVerticalAngleChanged)
        MinimumDistanceSlider(onMinDistanceChanged)
        MaximumDistanceSlider(onMaxDistanceChanged)
        // checkbox
        Row {
            FrustumCheckBox(isFrustumVisible)
            AnalysisCheckBox(isAnalysisVisible)
        }
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

@Composable
private fun PitchSlider(onPitchChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Pitch",
        initialSliderValue = 60f,
        sliderRangeValue = 0f..180f,
        functionChanged = onPitchChanged
    )
}

@Composable
private fun HorizontalAngleSlider(onHorizontalAngleChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Horizontal Angle",
        initialSliderValue = 75f,
        sliderRangeValue = 1f..120f,
        functionChanged = onHorizontalAngleChanged
    )
}

@Composable
private fun VerticalAngleSlider(onVerticalAngleChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Vertical Angle",
        initialSliderValue = 90f,
        sliderRangeValue = 1f..120f,
        functionChanged = onVerticalAngleChanged
    )
}

@Composable
private fun MinimumDistanceSlider(onMinDistanceChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Minimum Distance",
        initialSliderValue = 0f,
        sliderRangeValue = 0f..8999f,
        functionChanged = onMinDistanceChanged
    )
}

@Composable
private fun MaximumDistanceSlider(onMaxDistanceChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Maximum Distance",
        initialSliderValue = 1500f,
        sliderRangeValue = 0f..9999f,
        functionChanged = onMaxDistanceChanged
    )
}

@Composable
fun FrustumCheckBox(isFrustumVisible: (Boolean) -> Unit) {
    // set the state of the checkbox
    val checkedState = remember { mutableStateOf(true) }
    // display a row and create a checkbox and text in a row
    Row {
        Checkbox(
            checked = checkedState.value,
            onCheckedChange = {
                checkedState.value = it
                isFrustumVisible(checkedState.value)
            },
        )
        Text(modifier = Modifier.padding(top = 10.dp), text = "Frustum Outline")
    }
}

@Composable
fun AnalysisCheckBox(isAnalysisVisible: (Boolean) -> Unit) {
    // set the state of the checkbox
    val checkedState = remember { mutableStateOf(true) }
    // display a row and create a checkbox and text in a row
    Row {
        Checkbox(
            checked = checkedState.value,
            onCheckedChange = {
                checkedState.value = it
                isAnalysisVisible(checkedState.value)
            },
        )
        Text(modifier = Modifier.padding(top = 10.dp), text = "Analysis Overlay")
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewViewshedOptions() {
    SampleAppTheme {
        Surface {
            ViewshedOptionsScreen()
        }
    }
}

