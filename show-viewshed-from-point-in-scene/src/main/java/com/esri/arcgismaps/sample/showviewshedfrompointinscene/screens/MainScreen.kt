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

package com.esri.arcgismaps.sample.showviewshedfrompointinscene.screens

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showviewshedfrompointinscene.components.SceneViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String, application: Application) {
    // create a ViewModel to handle SceneView interactions
    val sceneViewModel = SceneViewModel(application)

    val sceneViewProxy = SceneViewProxy().apply {
        setViewpointCamera(sceneViewModel.camera)
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it)
                ) {
                    // composable function that wraps the SceneView
                    SceneView(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        arcGISScene = sceneViewModel.scene,
                        cameraController = sceneViewModel.cameraController,
                        sceneViewProxy = sceneViewProxy,
                        analysisOverlays = listOf(sceneViewModel.analysisOverlay)

                    )
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
        }
    )
}
