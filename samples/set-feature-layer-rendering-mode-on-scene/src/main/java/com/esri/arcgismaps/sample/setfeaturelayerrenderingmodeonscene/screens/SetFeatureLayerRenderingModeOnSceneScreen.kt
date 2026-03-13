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

package com.esri.arcgismaps.sample.setfeaturelayerrenderingmodeonscene.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.setfeaturelayerrenderingmodeonscene.components.SetFeatureLayerRenderingModeOnSceneViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen for the SetFeatureLayerRenderingModeOnScene sample.
 * Displays two SceneViews stacked vertically: the top scene renders layers statically,
 * the bottom scene renders layers dynamically. A FloatingActionButton toggles a zoom
 * animation on both scenes to demonstrate the rendering differences while animating.
 */
@Composable
fun SetFeatureLayerRenderingModeOnSceneScreen(sampleName: String) {
    val viewModel: SetFeatureLayerRenderingModeOnSceneViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { paddingValues ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)) {

                // Static SceneView - renders its layers using static rendering
                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()) {
                    SceneView(
                        modifier = Modifier.fillMaxSize(),
                        arcGISScene = viewModel.staticScene,
                        sceneViewProxy = viewModel.staticSceneViewProxy
                    )

                    // Small overlay label to indicate static scene
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ComposeColor.Black.copy(alpha = 0.35f))
                            .align(Alignment.TopCenter)
                    ) {
                        Text(
                            text = "Static",
                            color = ComposeColor.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(8.dp)
                        )
                    }
                }

                // Dynamic SceneView - renders its layers using dynamic rendering
                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()) {
                    SceneView(
                        modifier = Modifier.fillMaxSize(),
                        arcGISScene = viewModel.dynamicScene,
                        sceneViewProxy = viewModel.dynamicSceneViewProxy
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ComposeColor.Black.copy(alpha = 0.35f))
                            .align(Alignment.TopCenter)
                    ) {
                        Text(
                            text = "Dynamic",
                            color = ComposeColor.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(8.dp)
                        )
                    }
                }

                // Small row showing state information and a manual zoom button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dynamicStatus = viewModel.dynamicScene.loadStatus.collectAsStateWithLifecycle().value
                    val staticStatus = viewModel.staticScene.loadStatus.collectAsStateWithLifecycle().value
                    val statusText = "Dynamic: $dynamicStatus | Static: $staticStatus"

                    Text(text = statusText)

                    Button(onClick = { viewModel.toggleZoom() }, enabled = true) {
                        Text(if (viewModel.isZoomedIn) "Zoom Out" else "Zoom In")
                    }
                }

                // Display message dialog if an error occurred in the ViewModel
                viewModel.messageDialogVM.apply {
                    if (dialogStatus) {
                        MessageDialog(
                            title = messageTitle,
                            description = messageDescription,
                            onDismissRequest = ::dismissDialog
                        )
                    }
                }
            }
        }
    )
}
