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

package com.esri.arcgismaps.sample.stylepointwithdistancecompositescenesymbol.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.stylepointwithdistancecompositescenesymbol.components.StylePointWithDistanceCompositeSceneSymbolViewModel

/**
 * Main screen for the sample. Hosts a SceneView and a simple overlay with instructions and distance display.
 */
@Composable
fun StylePointWithDistanceCompositeSceneSymbolScreen(sampleName: String) {
    val viewModel: StylePointWithDistanceCompositeSceneSymbolViewModel = viewModel()

    // Collect camera distance exposed by the ViewModel.
    val cameraDistance by viewModel.cameraDistanceMeters.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Provide the scene, camera controller, overlays and the SceneViewProxy.
            SceneView(
                modifier = Modifier.fillMaxSize().weight(1f),
                arcGISScene = viewModel.arcGISScene,
                sceneViewProxy = viewModel.sceneViewProxy,
                cameraController = viewModel.orbitCameraController,
                graphicsOverlays = viewModel.graphicsOverlays,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier.padding(top = 6.dp),
                    text = "Distance from target: ${cameraDistance.toInt()} m",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Zoom in and out to see the symbol change.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        // Display message dialog if any error occurs
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
}
