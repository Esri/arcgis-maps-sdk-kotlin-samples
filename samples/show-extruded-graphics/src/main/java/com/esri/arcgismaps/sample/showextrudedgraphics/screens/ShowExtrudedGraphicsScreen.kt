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

package com.esri.arcgismaps.sample.showextrudedgraphics.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.showextrudedgraphics.components.ShowExtrudedGraphicsViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main composable screen for the ShowExtrudedGraphics sample.
 * Displays a SceneView bound to the scene prepared in the ViewModel.
 */
@Composable
fun ShowExtrudedGraphicsScreen(sampleName: String) {
    val viewModel: ShowExtrudedGraphicsViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(it)) {

                // SceneView displays the 3D ArcGISScene prepared by the ViewModel.
                SceneView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISScene = viewModel.arcGISScene,
                    graphicsOverlays = listOf(viewModel.graphicsOverlay)
                )

            }

            // Show any message dialogs coming from the ViewModel (loading / errors)
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
    )
}
