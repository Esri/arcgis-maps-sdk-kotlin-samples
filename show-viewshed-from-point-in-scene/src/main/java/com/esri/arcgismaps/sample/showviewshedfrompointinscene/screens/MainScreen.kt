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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
    // create a ViewModel to handle MapView interactions
    val sceneViewModel = SceneViewModel(application)

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                // composable function that wraps the MapView
                ComposeSceneView(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    sceneViewModel = sceneViewModel
                )
                // sliders
                HeadingSlider()
                PitchSlider()
                HorizontalAngleSlider()
                VerticalAngleSlider()
                MinimumDistanceSlider()
                MaximumDistanceSlider()

            }
        }
    )
}

@Composable
private fun HeadingSlider() {

    var sliderValue by remember {
        mutableStateOf(0f)
    }
    Row {
        Text(text = "Heading")
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
            },
            onValueChangeFinished = {
                // this is called when the user completed selecting the value
                Log.d("MainActivity", "sliderValue = ${sliderValue.toInt()}")
            },
            valueRange = 0f..360f
        )
        Text(text = sliderValue.toInt().toString())
    }
}

@Composable
private fun PitchSlider() {

    var sliderValue by remember {
        mutableStateOf(0f)
    }
    Row {
        Text(text = "Pitch")
        Slider(
            modifier = Modifier.weight(1f).padding(5.dp),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
            },
            onValueChangeFinished = {
                // this is called when the user completed selecting the value
                Log.d("MainActivity", "sliderValue = ${sliderValue.toInt()}")
            },
            valueRange = 0f..180f
        )
        Text(text = sliderValue.toInt().toString())
    }
}

@Composable
private fun HorizontalAngleSlider() {

    var sliderValue by remember {
        mutableStateOf(0f)
    }
    Row {
        Text(text = "Horizontal Angle")
        Slider(
            modifier = Modifier.weight(1f).padding(5.dp),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
            },
            onValueChangeFinished = {
                // this is called when the user completed selecting the value
                Log.d("MainActivity", "sliderValue = ${sliderValue.toInt()}")
            },
            valueRange = 0f..120f
        )
        Text(text = sliderValue.toInt().toString())
    }
}

@Composable
private fun VerticalAngleSlider() {

    var sliderValue by remember {
        mutableStateOf(0f)
    }
    Row {
        Text(text = "Vertical Angle")
        Slider(
            modifier = Modifier.weight(1f).padding(5.dp),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
            },
            onValueChangeFinished = {
                // this is called when the user completed selecting the value
                Log.d("MainActivity", "sliderValue = ${sliderValue.toInt()}")
            },
            valueRange = 0f..120f
        )
        Text(text = sliderValue.toInt().toString())
    }
}

@Composable
private fun MinimumDistanceSlider() {

    var sliderValue by remember {
        mutableStateOf(0f)
    }
    Row {
        Text(text = "Minimum Distance")
        Slider(
            modifier = Modifier.weight(1f).padding(5.dp),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
            },
            onValueChangeFinished = {
                // this is called when the user completed selecting the value
                Log.d("MainActivity", "sliderValue = ${sliderValue.toInt()}")
            },
            valueRange = 0f..8999f
        )
        Text(text = sliderValue.toInt().toString())
    }
}

@Composable
private fun MaximumDistanceSlider() {

    var sliderValue by remember {
        mutableStateOf(0f)
    }
    Row {
        Text(text = "Maximum Distance")
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
            },
            onValueChangeFinished = {
                // this is called when the user completed selecting the value
                Log.d("MainActivity", "sliderValue = ${sliderValue.toInt()}")
            },
            valueRange = 0f..9999f
        )
        Text(text = sliderValue.toInt().toString())
    }
}
