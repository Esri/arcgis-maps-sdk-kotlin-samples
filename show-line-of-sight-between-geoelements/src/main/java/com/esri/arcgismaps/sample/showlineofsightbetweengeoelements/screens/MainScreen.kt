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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.PointBuilder
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showlineofsightbetweengeoelements.components.SceneViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    val sceneViewModel: SceneViewModel = viewModel()
    val sliderMinHeightOffset = 150

    // Positions slider to 200 on launch
    var sliderPosition by remember {
        mutableFloatStateOf(sceneViewModel.observationPoint.z!!.minus(sliderMinHeightOffset).toFloat())
    }

  //  var sliderPosition by remember { mutableFloatStateOf(0f) }

    Scaffold(topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {


                    if (!sceneViewModel.sceneLoading) {
                        LoadingDialog(loadingMessage = "Loading Scene...")
                    } else {
                    // composable function that wraps the SceneView
                        SceneView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(it),
                            arcGISScene = sceneViewModel.scene,
                            analysisOverlays = listOf(sceneViewModel.analysisOverlay),
                            graphicsOverlays = listOf(sceneViewModel.graphicsOverlay),
                        )
                }
                    Row(
                        modifier = Modifier.Companion
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.White),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        // at lowest value in the range, it will be 150 because height = 0 + 150
                        // at highest value, it will be 300 because height = (300-150) + 150 = 300
                         val height = sliderPosition + sliderMinHeightOffset
                        Slider(
                            value = sliderPosition,
                            onValueChange = {
                                sliderPosition = it
                                    val pointBuilder = PointBuilder(sceneViewModel.observer.geometry as Point).apply {
                                        z = height.toDouble()
                                    }
                                    sceneViewModel.observer.geometry = pointBuilder.toGeometry()

                                //println("THE Z VALUE: ${(sceneViewModel.observer.geometry as Point).z}")
                            },

                               valueRange = 0f..(300f-sliderMinHeightOffset),
                            modifier = Modifier
                                .width(300.dp)
                                .padding(8.dp),
                        )
                        Text(
                            text = height.toInt().toString(),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }


            // display a dialog if the sample encounters an error
            sceneViewModel.messageDialogVM.apply {
                if (dialogStatus) {
                    MessageDialog(
                        title = messageTitle,
                        description = messageDescription,
                        onDismissRequest = ::dismissDialog
                    )
                }
            }
        })
}
