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

package com.esri.arcgismaps.sample.augmentrealitytoshowtabletopscene.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.LoadStatus
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.toolkit.ar.TableTopSceneView
import com.arcgismaps.toolkit.ar.TableTopSceneViewStatus
import com.arcgismaps.toolkit.ar.rememberTableTopSceneViewStatus
import com.esri.arcgismaps.sample.augmentrealitytoshowtabletopscene.R
import com.esri.arcgismaps.sample.augmentrealitytoshowtabletopscene.components.AugmentRealityToShowTabletopSceneViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun DisplaySceneInTabletopARScreen(sampleName: String) {
    val sceneViewModel: AugmentRealityToShowTabletopSceneViewModel = viewModel()
    var initializationStatus: TableTopSceneViewStatus by rememberTableTopSceneViewStatus()
    Scaffold(topBar = { SampleTopAppBar(title = sampleName) }, content = { paddingValues ->
        sceneViewModel.scene?.let {
            // Scene view that provides an augmented reality table top experience
            TableTopSceneView(
                arcGISScene = it,
                arcGISSceneAnchor = Point(
                    x = -75.16996728256345, y = 39.95787000283599, spatialReference = SpatialReference.wgs84()
                ),
                translationFactor = 800.0,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                clippingDistance = 800.0,
                onInitializationStatusChanged = { statusChanged ->
                    initializationStatus = statusChanged
                },
            )
        }
    })

    // Show an overlay with instructions or progress indicator based on the initialization status
    when (val status = initializationStatus) {
        is TableTopSceneViewStatus.Initializing -> ShowInstruction(text = stringResource(R.string.initializing_overlay))
        is TableTopSceneViewStatus.DetectingPlanes -> ShowInstruction(text = stringResource(R.string.detect_planes_overlay))
        is TableTopSceneViewStatus.Initialized -> {
            sceneViewModel.scene?.let {
                when (val sceneLoadStatus = it.loadStatus.collectAsStateWithLifecycle().value) {
                    // Tell the user to tap the screen if the scene has not started loading
                    is LoadStatus.NotLoaded -> ShowInstruction(text = stringResource(R.string.tap_scene_overlay))
                    // The scene may take a while to load, so show a progress indicator
                    is LoadStatus.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    // Show an error message if the scene failed to load
                    is LoadStatus.FailedToLoad -> ShowInstruction(
                        text = stringResource(
                            R.string.failed_to_load_scene, sceneLoadStatus.error
                        )
                    )
                    is LoadStatus.Loaded -> {} // Do nothing
                }
            }
        }
        is TableTopSceneViewStatus.FailedToInitialize -> {
            ShowInstruction(
                text = stringResource(R.string.failed_to_initialize_overlay, status.error.message ?: status.error)
            )
        }
    }
}

/**
 * Displays the provided [text] in the middle of the screen.
 */
@Composable
fun ShowInstruction(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text)
    }
}
