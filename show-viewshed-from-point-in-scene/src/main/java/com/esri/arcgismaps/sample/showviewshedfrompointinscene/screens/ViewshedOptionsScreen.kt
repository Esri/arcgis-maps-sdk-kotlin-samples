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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.showviewshedfrompointinscene.components.SceneViewModel

/**
 * Viewshed options screen for sliders and checkbox's
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

    Column() {
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

    var sliderValue by remember {
        mutableStateOf(82f)
    }
    Row {
        Text(modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp).width(150.dp),
            text = "Heading")
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                // update view model viewshed value
                onHeadingChanged(sliderValue)
            },
            valueRange = 0f..360f
        )
        Text(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp).size(40.dp),
            text = sliderValue.toInt().toString()
        )
    }
}

@Composable
private fun PitchSlider(sceneViewModel: SceneViewModel) {

    var sliderValue by remember {
        mutableStateOf(60f)
    }
    Row {
        Text(modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp).width(150.dp),
            text = "Pitch")
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                // this is called when the user completed selecting the value
                sceneViewModel.setPitch(sliderValue)
            },
            valueRange = 0f..180f
        )
        Text(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp).size(40.dp),
            text = sliderValue.toInt().toString()
        )
    }
}

@Composable
private fun HorizontalAngleSlider(sceneViewModel: SceneViewModel) {

    var sliderValue by remember {
        mutableStateOf(75f)
    }
    Row {
        Text(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp).width(150.dp),
            text = "Horizontal Angle"
        )
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                // this is called when the user completed selecting the value
                sceneViewModel.setHorizontalAngleSlider(sliderValue)
            },
            valueRange = 1f..120f
        )
        Text(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp).size(40.dp),
            text = sliderValue.toInt().toString()
        )
    }
}

@Composable
private fun VerticalAngleSlider(sceneViewModel: SceneViewModel) {

    var sliderValue by remember {
        mutableStateOf(90f)
    }
    Row {
        Text(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp).width(150.dp),
            text = "Vertical Angle"
        )
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                // this is called when the user completed selecting the value
                sceneViewModel.setVerticalAngleSlider(sliderValue)
            },
            valueRange = 1f..120f
        )
        Text(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp).size(40.dp),
            text = sliderValue.toInt().toString()
        )
    }
}

@Composable
private fun MinimumDistanceSlider(sceneViewModel: SceneViewModel) {

    var sliderValue by remember {
        mutableStateOf(0f)
    }
    Row {
        Text(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp).width(150.dp),
            text = "Minimum Distance"
        )
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                // this is called when the user completed selecting the value
                sceneViewModel.setMinimumDistanceSlider(sliderValue)
            },
            valueRange = 0f..8999f
        )
        Text(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp).size(40.dp),
            text = sliderValue.toInt().toString()
        )
    }
}

@Composable
private fun MaximumDistanceSlider(sceneViewModel: SceneViewModel) {

    var sliderValue by remember {
        mutableStateOf(1500f)
    }
    Row {
        Text(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp).width(150.dp),
            text = "Maximum Distance"
        )
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                // this is called when the user completed selecting the value
                sceneViewModel.setMaximumDistanceSlider(sliderValue)
            },
            valueRange = 0f..9999f
        )
        Text(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp).size(40.dp),
            text = sliderValue.toInt().toString()
        )
    }
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
fun AnalysisCheckBox(sceneViewModel: SceneViewModel) {
    // set the state of the checkbox
    val checkedState = remember { mutableStateOf(true) }
    // display a row and create a checkbox and text in a row
    Row {
        Checkbox(
            checked = checkedState.value,
            onCheckedChange = {
                checkedState.value = it
                sceneViewModel.analysisVisibility(checkedState.value)
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

