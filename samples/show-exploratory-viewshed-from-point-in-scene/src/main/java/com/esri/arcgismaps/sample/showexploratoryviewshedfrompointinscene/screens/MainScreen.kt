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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.view.OrbitLocationCameraController
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showexploratoryviewshedfrompointinscene.components.SceneViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    var isBottomSheetVisible by remember { mutableStateOf(true) }
    // create a ViewModel to handle SceneView interactions
    val sceneViewModel: SceneViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            if (!isBottomSheetVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 24.dp),
                    onClick = { isBottomSheetVisible = true }
                ) { Icon(Icons.Filled.Settings, contentDescription = "Viewshed options") }
            }
        },
        content = {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it)
                ) {

                    val cameraController = remember {
                        OrbitLocationCameraController(
                            targetPoint = sceneViewModel.initLocation,
                            distance = 5000.0
                        )
                    }
                    // composable function that wraps the SceneView
                    SceneView(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        arcGISScene = sceneViewModel.scene,
                        onDown = { isBottomSheetVisible = false },
                        cameraController = cameraController,
                        analysisOverlays = listOf(sceneViewModel.analysisOverlay)
                    )
                }
            }
            BottomSheet(
                sheetTitle = "Viewshed Options",
                isVisible = isBottomSheetVisible,
                onDismissRequest = { isBottomSheetVisible = false }
            ) {
                // display list of options to modify viewshed properties
                ViewshedOptionsScreen(
                    onHeadingChanged = sceneViewModel::setHeading,
                    onPitchChanged = sceneViewModel::setPitch,
                    onHorizontalAngleChanged = sceneViewModel::setHorizontalAngleSlider,
                    onVerticalAngleChanged = sceneViewModel::setVerticalAngleSlider,
                    onMinDistanceChanged = sceneViewModel::setMinimumDistanceSlider,
                    onMaxDistanceChanged = sceneViewModel::setMaximumDistanceSlider,
                    isFrustumVisible = sceneViewModel::frustumVisibility,
                    isAnalysisVisible = sceneViewModel::analysisVisibility
                )
            }
        }
    )
}
