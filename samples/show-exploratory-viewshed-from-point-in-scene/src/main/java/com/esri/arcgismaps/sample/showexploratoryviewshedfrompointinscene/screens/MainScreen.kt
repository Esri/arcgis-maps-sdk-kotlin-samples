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

package com.esri.arcgismaps.sample.showexploratoryviewshedfrompointinscene.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.view.OrbitLocationCameraController
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.AdaptiveThreePane
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showexploratoryviewshedfrompointinscene.components.SceneViewModel
import kotlinx.coroutines.launch

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    // create a ViewModel to handle SceneView interactions
    val sceneViewModel: SceneViewModel = viewModel()
    val sceneViewProxy = remember { SceneViewProxy() }
    val coroutineScope = rememberCoroutineScope()
    val cameraController = remember {
        OrbitLocationCameraController(
            targetPoint = sceneViewModel.initLocation,
            distance = 5000.0
        )
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { paddingValues ->
            AdaptiveThreePane(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                supportingPaneTitle = "Viewshed Options",
                floatingPaneTitle = "Scene Options",
                mainPane = { _, _ ->
                    // composable function that wraps the SceneView
                    SceneView(
                        modifier = Modifier.fillMaxSize(),
                        arcGISScene = sceneViewModel.scene,
                        sceneViewProxy = sceneViewProxy,
                        cameraController = cameraController,
                        analysisOverlays = listOf(sceneViewModel.analysisOverlay)
                    )
                },
                supportingPane = { isFloatingPaneVisible, toggleFloatingPane ->
                    ViewshedSlidersContent(
                        onHeadingChanged = sceneViewModel::setHeading,
                        onPitchChanged = sceneViewModel::setPitch,
                        onHorizontalAngleChanged = sceneViewModel::setHorizontalAngleSlider,
                        onVerticalAngleChanged = sceneViewModel::setVerticalAngleSlider,
                        onMinDistanceChanged = sceneViewModel::setMinimumDistanceSlider,
                        onMaxDistanceChanged = sceneViewModel::setMaximumDistanceSlider,
                    )

                    OutlinedButton(onClick = toggleFloatingPane) {
                        Text(if (isFloatingPaneVisible) "Hide scene options" else "Open scene options")
                    }
                },
                floatingPane = {
                    ViewshedSceneOptionsContent(
                        isFrustumVisible = sceneViewModel::frustumVisibility,
                        isAnalysisVisible = sceneViewModel::analysisVisibility,
                        onSetViewpointToAnalysisExtent = {
                            coroutineScope.launch {
                                sceneViewModel.setViewpointToAnalysisExtent(sceneViewProxy)
                            }
                        }
                    )
                }
            )
        }
    )
}
