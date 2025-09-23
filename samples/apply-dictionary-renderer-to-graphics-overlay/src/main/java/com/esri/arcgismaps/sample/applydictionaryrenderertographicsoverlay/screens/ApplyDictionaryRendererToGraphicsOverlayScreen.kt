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

package com.esri.arcgismaps.sample.applydictionaryrenderertographicsoverlay.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.applydictionaryrenderertographicsoverlay.components.ApplyDictionaryRendererToGraphicsOverlayViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import androidx.compose.ui.Modifier

/**
 * Screen composable that displays a SceneView and binds to the [ApplyDictionaryRendererToGraphicsOverlayViewModel].
 * The SceneView shows graphics styled with a DictionaryRenderer loaded from a web style and uses a Camera
 * provided by the ViewModel to set the initial viewpoint.
 */
@Composable
fun ApplyDictionaryRendererToGraphicsOverlayScreen(sampleName: String) {
    val viewModel: ApplyDictionaryRendererToGraphicsOverlayViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            // Composable SceneView that renders graphics styled with DictionaryRenderer
            // using configured ArcGISObjects from the ViewModel (scene, overlays, proxy)
            SceneView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                arcGISScene = viewModel.arcGISScene,
                sceneViewProxy = viewModel.sceneViewProxy,
                graphicsOverlays = listOf(viewModel.graphicsOverlay)
            )

            // Show any error/messages from the view model
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
