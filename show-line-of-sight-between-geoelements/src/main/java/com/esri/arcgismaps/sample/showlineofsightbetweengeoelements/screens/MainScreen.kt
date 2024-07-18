/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.showlineofsightbetweengeoelements.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showlineofsightbetweengeoelements.components.SceneViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {

    // Define the viewmodel of this sample
    val sceneViewModel: SceneViewModel = viewModel()

    // Retrieve any changes to the z value from SceneViewModel
    val observerHeight = sceneViewModel.observerHeight.collectAsState().value

    // Defined in order to keep the z value in the positive range
    val offset = 100

    Scaffold(topBar = { SampleTopAppBar(title = sampleName) },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Show the current target visibility status.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "Target visibility status: ")
                    Text(
                        text = sceneViewModel.targetVisibilityString,
                        color = if (sceneViewModel.targetVisibilityString.contains("Visible"))
                            Color.Green else Color.Red
                    )
                }
                // Composable function that wraps the SceneView
                SceneView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    arcGISScene = sceneViewModel.scene,
                    analysisOverlays = listOf(sceneViewModel.analysisOverlay),
                    graphicsOverlays = listOf(sceneViewModel.graphicsOverlay),
                )

                // Composable function that holds the slider and the text position value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Observer height: ${(observerHeight.toInt() + offset)}",
                    )
                    Slider(
                        value = (observerHeight.toFloat() + offset),
                        onValueChange = { newHeight ->
                            sceneViewModel.updateHeight(newHeight.toDouble() - offset)
                        },
                        valueRange = 0f..300f,
                    )

                }
            }
        })
}
