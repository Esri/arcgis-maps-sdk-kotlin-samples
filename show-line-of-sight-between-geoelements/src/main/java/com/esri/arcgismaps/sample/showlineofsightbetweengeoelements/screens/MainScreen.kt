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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
    val currentZValue = sceneViewModel.currentZValue.collectAsState().value

    // Defined in order to keep the z value in the positive range
    val offset = 100

    Scaffold(topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    // Composable function that wraps the SceneView
                    SceneView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it),
                        arcGISScene = sceneViewModel.scene,
                        analysisOverlays = listOf(sceneViewModel.analysisOverlay),
                        graphicsOverlays = listOf(sceneViewModel.graphicsOverlay),
                    )

                    // Composable function that holds the slider and the text position value
                    Row(
                        modifier = Modifier.Companion
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.White),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Slider(
                            value =( currentZValue.toFloat() + offset),
                            onValueChange = { newHeight ->
                                sceneViewModel.updateHeight(newHeight.toDouble() - offset)
                            },
                            valueRange = 0f..300f,
                            modifier = Modifier
                                .width(300.dp)
                                .padding(8.dp),
                        )
                        Text(
                            text = (currentZValue.toInt() + offset).toString() ,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        })
}
