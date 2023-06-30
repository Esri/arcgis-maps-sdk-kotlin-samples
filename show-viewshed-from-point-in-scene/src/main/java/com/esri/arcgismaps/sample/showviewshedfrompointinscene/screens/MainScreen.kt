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

import android.app.Application
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showviewshedfrompointinscene.components.ComposeSceneView
import com.esri.arcgismaps.sample.showviewshedfrompointinscene.components.SceneViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String, application: Application) {
    // create a ViewModel to handle SceneView interactions
    val sceneViewModel = SceneViewModel(application)

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it)
                ) {
                    // composable function that wraps the SceneView
                    ComposeSceneView(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        sceneViewModel = sceneViewModel
                    )
                    // sliders
                    HeadingSlider(sceneViewModel)
                    PitchSlider(sceneViewModel)
                    HorizontalAngleSlider(sceneViewModel)
                    VerticalAngleSlider(sceneViewModel)
                    MinimumDistanceSlider(sceneViewModel)
                    MaximumDistanceSlider(sceneViewModel)
                    Row {
                        FrustumCheckBox(sceneViewModel)
                        AnalysisCheckBox(sceneViewModel)
                    }
                }

            }
        }
    )
}

@Composable
private fun HeadingSlider(sceneViewModel: SceneViewModel) {

    var sliderValue by remember {
        mutableStateOf(82f)
    }
    Row {
        Text(modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp), text = "Heading")
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
            },
            onValueChangeFinished = {
                // this is called when the user completed selecting the value
                sceneViewModel.setHeading(sliderValue)
            },
            valueRange = 0f..360f
        )
        Text(modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp), text = sliderValue.toInt().toString())
    }
}

@Composable
private fun PitchSlider(sceneViewModel: SceneViewModel) {

    var sliderValue by remember {
        mutableStateOf(60f)
    }
    Row {
        Text(modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp), text = "Pitch")
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
            },
            onValueChangeFinished = {
                // this is called when the user completed selecting the value
                sceneViewModel.setPitch(sliderValue)
            },
            valueRange = 0f..180f
        )
        Text(modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp), text = sliderValue.toInt().toString())
    }
}

@Composable
private fun HorizontalAngleSlider(sceneViewModel: SceneViewModel) {

    var sliderValue by remember {
        mutableStateOf(75f)
    }
    Row {
        Text(modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp), text = "Horizontal Angle")
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
            },
            onValueChangeFinished = {
                // this is called when the user completed selecting the value
                sceneViewModel.setHorizontalAngleSlider(sliderValue)
            },
            valueRange = 0f..120f
        )
        Text(modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp), text = sliderValue.toInt().toString())
    }
}

@Composable
private fun VerticalAngleSlider(sceneViewModel: SceneViewModel) {

    var sliderValue by remember {
        mutableStateOf(90f)
    }
    Row {
        Text(modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp), text = "Vertical Angle")
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
            },
            onValueChangeFinished = {
                // this is called when the user completed selecting the value
                sceneViewModel.setVerticalAngleSlider(sliderValue)
            },
            valueRange = 0f..120f
        )
        Text(modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp), text = sliderValue.toInt().toString())
    }
}

@Composable
private fun MinimumDistanceSlider(sceneViewModel: SceneViewModel) {

    var sliderValue by remember {
        mutableStateOf(0f)
    }
    Row {
        Text(modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp), text = "Minimum Distance")
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
            },
            onValueChangeFinished = {
                // this is called when the user completed selecting the value
                sceneViewModel.setMinimumDistanceSlider(sliderValue)
            },
            valueRange = 0f..8999f
        )
        Text(modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp), text = sliderValue.toInt().toString())
    }
}

@Composable
private fun MaximumDistanceSlider(sceneViewModel: SceneViewModel) {

    var sliderValue by remember {
        mutableStateOf(1500f)
    }
    Row {
        Text(modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp), text = "Maximum Distance")
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
            },
            onValueChangeFinished = {
                // this is called when the user completed selecting the value
                Log.d("MainActivity", "sliderValue = ${sliderValue.toInt()}")
                sceneViewModel.setMaximumDistanceSlider(sliderValue)
            },
            valueRange = 0f..9999f
        )
        Text(modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp), text = sliderValue.toInt().toString())
    }
}

@Composable
fun FrustumCheckBox(sceneViewModel: SceneViewModel) {
    // in below line we are setting
    // the state of our checkbox.
    val checkedState = remember { mutableStateOf(true) }
    // in below line we are displaying a row
    // and we are creating a checkbox in a row.
    Row {
        Checkbox(
            // below line we are setting
            // the state of checkbox.
            checked = checkedState.value,
            // below line is use to add padding
            // to our checkbox.

            // below line is use to add on check
            // change to our checkbox.
            onCheckedChange = {
                checkedState.value = it
                sceneViewModel.frustumVisibility(checkedState.value) },
        )
        // below line is use to add text to our check box and we are
        // adding padding to our text of checkbox
        Text(modifier = Modifier.padding(top = 10.dp), text = "Frustum Outline")
    }
}

@Composable
fun AnalysisCheckBox(sceneViewModel: SceneViewModel) {
    // in below line we are setting
    // the state of our checkbox.
    val checkedState = remember { mutableStateOf(true) }
    // in below line we are displaying a row
    // and we are creating a checkbox in a row.
    Row {
        Checkbox(
            // below line we are setting
            // the state of checkbox.
            checked = checkedState.value,
            // below line is use to add padding
            // to our checkbox.

            // below line is use to add on check
            // change to our checkbox.
            onCheckedChange = {
                checkedState.value = it
                sceneViewModel.analysisVisibility(checkedState.value) },
        )
        // below line is use to add text to our check box and we are
        // adding padding to our text of checkbox
        Text(modifier = Modifier.padding(top = 10.dp), text = "Analysis Overlay")
    }
}