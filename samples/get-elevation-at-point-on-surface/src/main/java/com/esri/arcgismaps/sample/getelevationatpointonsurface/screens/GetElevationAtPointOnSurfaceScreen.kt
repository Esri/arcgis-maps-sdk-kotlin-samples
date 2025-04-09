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

package com.esri.arcgismaps.sample.getelevationatpointonsurface.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.getelevationatpointonsurface.components.GetElevationAtPointOnSurfaceViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun GetElevationAtPointOnSurfaceScreen(sampleName: String) {
    val sceneViewModel: GetElevationAtPointOnSurfaceViewModel = viewModel()
    Scaffold(topBar = { SampleTopAppBar(title = sampleName) }, content = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
        ) {
            SceneView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                arcGISScene = sceneViewModel.arcGISScene,
                graphicsOverlays = listOf(sceneViewModel.graphicsOverlay),
                // On single tap, call get elevation in the view model
                onSingleTapConfirmed = sceneViewModel::getElevation,
                sceneViewProxy = sceneViewModel.sceneViewProxy,
            )
        }

        sceneViewModel.messageDialogVM.apply {
            if (dialogStatus) {
                MessageDialog(
                    title = messageTitle, description = messageDescription, onDismissRequest = ::dismissDialog
                )
            }
        }
    })
}
