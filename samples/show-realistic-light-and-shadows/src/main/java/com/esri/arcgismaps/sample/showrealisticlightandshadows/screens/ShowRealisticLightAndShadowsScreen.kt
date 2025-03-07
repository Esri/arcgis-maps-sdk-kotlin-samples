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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.view.LightingMode
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showrealisticlightandshadows.components.ShowRealisticLightAndShadowsViewModel

/**
 * Main screen layout for the sample app.
 */
@Composable
fun ShowRealisticLightAndShadowsScreen(sampleName: String) {
    val mapViewModel: ShowRealisticLightAndShadowsViewModel = viewModel()
    val lightingOptionsState = mapViewModel.lightingOptionsState
    val defaultTime = 15f * 60f * 60f // 15:00 in seconds since midnight
    var sliderPosition by remember { mutableFloatStateOf(defaultTime) }
    mapViewModel.setSunTime(defaultTime.toInt())
    val lightingModes = listOf(
        LightingMode.LightAndShadows,
        LightingMode.Light,
        LightingMode.NoLight
    )
    Scaffold(
        topBar = {
            SampleTopAppBar(title = sampleName, actions =
            {
                val expanded = remember { mutableStateOf(false) }
                IconButton(
                    onClick = { expanded.value = !expanded.value }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Lighting Settings"
                    )

                    DropdownMenu(
                        expanded = expanded.value,
                        onDismissRequest = { expanded.value = false }
                    ) {
                        lightingModes.forEach {

                            val text = when (it) {
                                LightingMode.Light -> "Light"
                                LightingMode.LightAndShadows -> "Light and Shadows"
                                LightingMode.NoLight -> "No Light"
                            }

                            // make the currently selected lighting option bold
                            val fontWeight = when (it) {
                                lightingOptionsState.sunLighting.value -> FontWeight.Bold
                                else -> FontWeight.Normal
                            }

                            DropdownMenuItem(
                                text = { Text(text = text, fontWeight = fontWeight) },
                                onClick = {
                                    lightingOptionsState.sunLighting.value = it
                                    expanded.value = false
                                }
                            )
                        }
                    }
                }
            }
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(it)
            ) {
                SceneView(
                    mapViewModel.arcGISScene,
                    modifier = Modifier.weight(1f),
                    sunTime = lightingOptionsState.sunTime.value,
                    sunLighting = lightingOptionsState.sunLighting.value,
                    ambientLightColor = lightingOptionsState.ambientLightColor.value,
                    atmosphereEffect = lightingOptionsState.atmosphereEffect.value,
                    spaceEffect = lightingOptionsState.spaceEffect.value
                )
                // create a slider with AM and PM labels to control the sun time
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
