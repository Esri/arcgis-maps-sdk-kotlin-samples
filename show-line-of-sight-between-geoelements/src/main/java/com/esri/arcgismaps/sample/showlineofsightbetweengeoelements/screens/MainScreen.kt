/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.showlineofsightbetweengeoelements.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showlineofsightbetweengeoelements.components.SceneViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    val sceneViewModel: SceneViewModel = viewModel()

    Scaffold(topBar = { SampleTopAppBar(title = sampleName) },
        content = {

            // composable function that wraps the SceneView
            SceneView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                arcGISScene = sceneViewModel.scene,
                analysisOverlays = listOf(sceneViewModel.analysisOverlay),
                graphicsOverlays = listOf( sceneViewModel.graphicsOverlay),

            )

            // display a dialog if the sample encounters an error
            sceneViewModel.messageDialogVM.apply {
                if (dialogStatus) {
                    MessageDialog(
                        title = messageTitle,
                        description = messageDescription,
                        onDismissRequest = ::dismissDialog
                    )
                }
            }
        })
}
