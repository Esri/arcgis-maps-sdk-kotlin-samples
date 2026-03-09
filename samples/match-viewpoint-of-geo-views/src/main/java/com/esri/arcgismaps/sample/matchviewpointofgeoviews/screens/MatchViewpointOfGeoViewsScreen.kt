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

package com.esri.arcgismaps.sample.matchviewpointofgeoviews.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Composable screen that displays a MapView and a SceneView stacked vertically and keeps their
 * center-and-scale viewpoints synchronized. The MapView and SceneView notify the ViewModel
 * about navigation state changes and viewpoint updates.
 */
@Composable
fun MatchViewpointOfGeoViewsScreen(sampleName: String) {
    val viewModel: MatchViewpointOfGeoViewsViewModel = viewModel()

    Column(modifier = Modifier.fillMaxSize()) {
        SampleTopAppBar(title = sampleName)

        Column(modifier = Modifier.fillMaxSize()) {
            MapView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                arcGISMap = viewModel.arcGISMap,
                mapViewProxy = viewModel.mapViewProxy,
                onNavigationChanged = { isNavigating ->
                    viewModel.updateMapIsNavigating(isNavigating)
                },
                onViewpointChangedForCenterAndScale = { newViewpoint: Viewpoint ->
                    viewModel.onMapViewpointChanged(newViewpoint)
                }
            )
            SceneView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                arcGISScene = viewModel.arcGISScene,
                sceneViewProxy = viewModel.sceneViewProxy,
                onNavigationChanged = { isNavigating ->
                    viewModel.updateSceneIsNavigating(isNavigating)
                },
                onViewpointChangedForCenterAndScale = { newViewpoint: Viewpoint ->
                    viewModel.onSceneViewpointChanged(newViewpoint)
                }
            )
        }
    }
}
