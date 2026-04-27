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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.sampleslib.components.AdaptiveThreePane
import com.esri.arcgismaps.sample.sampleslib.components.SampleDeviceLightDarkPreview
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showexploratoryviewshedfrompointinscene.R
import com.esri.arcgismaps.sample.showexploratoryviewshedfrompointinscene.components.SceneViewModel
import com.esri.arcgismaps.sample.showexploratoryviewshedfrompointinscene.components.ViewshedUiState

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen() {
    // create a ViewModel to handle SceneView interactions
    val sceneViewModel: SceneViewModel = viewModel()
    val viewshedUiState by sceneViewModel.viewshedUiState.collectAsStateWithLifecycle()

    MainScreenScaffold(
        viewshedUiState = viewshedUiState,
        onHeadingChanged = sceneViewModel::setHeading,
        onPitchChanged = sceneViewModel::setPitch,
        onHorizontalAngleChanged = sceneViewModel::setHorizontalAngleSlider,
        onVerticalAngleChanged = sceneViewModel::setVerticalAngleSlider,
        onMinDistanceChanged = sceneViewModel::setMinimumDistanceSlider,
        onMaxDistanceChanged = sceneViewModel::setMaximumDistanceSlider,
        isFrustumVisible = sceneViewModel::frustumVisibility,
        isAnalysisVisible = sceneViewModel::analysisVisibility,
        onSetViewpointToAnalysisExtent = sceneViewModel::setViewpointToAnalysisExtent,
        onResetViewshedOptions = sceneViewModel::resetViewshedOptions,
        mainPaneContent = {
            SceneView(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(),
                arcGISScene = sceneViewModel.scene,
                sceneViewProxy = sceneViewModel.sceneViewProxy,
                analysisOverlays = listOf(sceneViewModel.analysisOverlay)
            )
        }
    )
}

@Composable
private fun MainScreenScaffold(
    viewshedUiState: ViewshedUiState,
    onHeadingChanged: (Float) -> Unit = {},
    onPitchChanged: (Float) -> Unit = {},
    onHorizontalAngleChanged: (Float) -> Unit = {},
    onVerticalAngleChanged: (Float) -> Unit = {},
    onMinDistanceChanged: (Float) -> Unit = {},
    onMaxDistanceChanged: (Float) -> Unit = {},
    isFrustumVisible: (Boolean) -> Unit = {},
    isAnalysisVisible: (Boolean) -> Unit = {},
    onSetViewpointToAnalysisExtent: () -> Unit = {},
    onResetViewshedOptions: () -> Unit = {},
    mainPaneContent: @Composable BoxScope.() -> Unit,
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.show_exploratory_viewshed_from_point_in_scene_app_name)) },
        content = { paddingValues ->
            AdaptiveThreePane(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                supportingPaneTitle = "Viewshed Options",
                floatingPaneTitle = "Scene Options",
                mainPane = { _, _ -> mainPaneContent() },
                supportingPane = { isFloatingPaneVisible, toggleFloatingPane ->
                    ViewshedSlidersContent(
                        viewshedUiState = viewshedUiState,
                        isFloatingPaneVisible = isFloatingPaneVisible,
                        onHeadingChanged = onHeadingChanged,
                        onPitchChanged = onPitchChanged,
                        onHorizontalAngleChanged = onHorizontalAngleChanged,
                        onVerticalAngleChanged = onVerticalAngleChanged,
                        onMinDistanceChanged = onMinDistanceChanged,
                        onMaxDistanceChanged = onMaxDistanceChanged,
                        onToggleFloatingPane = toggleFloatingPane
                    )
                },
                floatingPane = {
                    ViewshedSceneOptionsContent(
                        viewshedUiState = viewshedUiState,
                        isFrustumVisible = isFrustumVisible,
                        isAnalysisVisible = isAnalysisVisible,
                        onSetViewpointToAnalysisExtent = onSetViewpointToAnalysisExtent,
                        onResetViewshedOptions = onResetViewshedOptions,
                    )
                }
            )
        }
    )
}

@SampleDeviceLightDarkPreview
@Composable
fun MainScreenPreview() {
    SamplePreviewSurface {
        MainScreenScaffold(
            viewshedUiState = ViewshedUiState(
                heading = 82f,
                pitch = 60f,
                horizontalAngle = 75f,
                verticalAngle = 90f,
                minDistance = 0f,
                maxDistance = 1500f,
                isFrustumVisible = true,
                isAnalysisVisible = true,
            ),
            mainPaneContent = {},
        )
    }
}


