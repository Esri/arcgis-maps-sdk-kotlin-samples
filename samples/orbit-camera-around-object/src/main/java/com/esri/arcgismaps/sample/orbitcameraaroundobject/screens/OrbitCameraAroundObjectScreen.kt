/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.orbitcameraaroundobject.screens

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.orbitcameraaroundobject.R
import com.esri.arcgismaps.sample.orbitcameraaroundobject.components.AdaptiveUiState
import com.esri.arcgismaps.sample.orbitcameraaroundobject.components.OrbitCameraAroundObjectViewModel
import com.esri.arcgismaps.sample.orbitcameraaroundobject.components.ViewMode
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDeviceLightDarkPreview
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.components.adaptive.AdaptiveThreePane

/**
 * Main composable screen for the sample.
 * It owns the ViewModel and hoists UI state for the scaffold and the MapView.
 */
@Composable
fun OrbitCameraAroundObjectScreen(
    viewModel: OrbitCameraAroundObjectViewModel = viewModel()
) {
    val adaptiveUiState = viewModel.adaptiveUiState.collectAsStateWithLifecycle().value

    MainScreenScaffold(
        adaptiveUiState = adaptiveUiState,
        setCameraHeading =  viewModel::setCameraHeading,
        setCameraPitch = viewModel::setPlanePitch,
        switchViewMode = viewModel::switchViewMode,
        setInteraction = viewModel::setInteraction,
        mainPaneContent = {
            SceneView(
                arcGISScene = viewModel.arcGISScene,
                sceneViewProxy = viewModel.sceneViewProxy,
                cameraController = viewModel.orbitCameraController,
                graphicsOverlays = viewModel.graphicsOverlays,
            )
        }
    )

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

@Composable
private fun MainScreenScaffold(
    adaptiveUiState: AdaptiveUiState,
    setCameraHeading: (Float) -> Unit = {},
    setCameraPitch: (Float) -> Unit = {},
    switchViewMode: (ViewMode) -> Unit = {},
    setInteraction: (Boolean) -> Unit = {},
    mainPaneContent: @Composable BoxScope.() -> Unit
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.orbit_camera_around_object_app_name)) },
        content = { paddingValues ->
            AdaptiveThreePane(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                supportingPaneTitle = "options",
                mainPane = { _, _ -> mainPaneContent() },
                supportingPane = { _, _ ->
                    OrbitCameraAroundObjectSupportingPane(
                        adaptiveUiState = adaptiveUiState,
                        setCameraHeading =  setCameraHeading,
                        setCameraPitch = setCameraPitch,
                        switchViewMode = switchViewMode,
                        setInteraction = setInteraction,
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
            adaptiveUiState = AdaptiveUiState.defaultState,
            mainPaneContent = {}
        )
    }
}
