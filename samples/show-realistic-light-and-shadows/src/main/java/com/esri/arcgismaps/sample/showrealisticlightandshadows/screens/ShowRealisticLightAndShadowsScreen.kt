/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.showrealisticlightandshadows.screens

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.view.LightingMode
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.showrealisticlightandshadows.components.ShowRealisticLightAndShadowsViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun ShowRealisticLightAndShadowsScreen(sampleName: String) {
    val mapViewModel: ShowRealisticLightAndShadowsViewModel = viewModel()
    val lightingOptionsState = mapViewModel.lightingOptionsState
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(it)
            ) {
                SceneView(
                    mapViewModel.arcGISScene,
                    modifier = Modifier
                        //.padding(innerPadding) // TODO: fix padding?
                        .fillMaxSize()
                        .weight(1f),
                    sunTime = lightingOptionsState.sunTime.value,
                    sunLighting = lightingOptionsState.sunLighting.value,
                    ambientLightColor = lightingOptionsState.ambientLightColor.value,
                    atmosphereEffect = lightingOptionsState.atmosphereEffect.value,
                    spaceEffect = lightingOptionsState.spaceEffect.value
                )
                Row {

                        Text(
                            text = "AM",
                            modifier = Modifier.padding(12.dp)
                        )
                        Slider(
                            modifier = Modifier.weight(1f),
                            value = sliderPosition,
                            onValueChange = {
                                sliderPosition = it
                                mapViewModel.setSunTime(it.toInt())
                            },
                            // the range is 0 to 86,340 seconds ((60 seconds * 60 minutes * 24 hours)  - 60 seconds),
                            // which means 12 am to 11:59 pm.
                            valueRange = 0f..86340f,
                        )
                        Text(
                            text = "PM",
                            modifier = Modifier.padding(12.dp)
                        )

                }
            }

            mapViewModel.messageDialogVM.apply {
                if (dialogStatus) {
                    MessageDialog(
                        title = messageTitle,
                        description = messageDescription,
                        onDismissRequest = ::dismissDialog
                    )
                }
            }
        }
    )
}
