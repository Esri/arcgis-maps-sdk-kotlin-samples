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
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.sampleslib.components.AdaptiveThreePane
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showexploratoryviewshedfrompointinscene.components.SceneViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    // create a ViewModel to handle SceneView interactions
    val sceneViewModel: SceneViewModel = viewModel()
    val viewshedUiState = sceneViewModel.viewshedUiState.collectAsStateWithLifecycle()

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
                        sceneViewProxy = sceneViewModel.sceneViewProxy,
                        analysisOverlays = listOf(sceneViewModel.analysisOverlay)
                    )
                },
                supportingPane = { isFloatingPaneVisible, toggleFloatingPane ->
                    ViewshedSlidersContent(
                        viewshedUiState = viewshedUiState.value,
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
                        viewshedUiState = viewshedUiState.value,
                        isFrustumVisible = sceneViewModel::frustumVisibility,
                        isAnalysisVisible = sceneViewModel::analysisVisibility,
                        onSetViewpointToAnalysisExtent = sceneViewModel::setViewpointToAnalysisExtent,
                        onResetViewshedOptions = sceneViewModel::resetViewshedOptions
                    )
                }
            )
        }
    )
}
