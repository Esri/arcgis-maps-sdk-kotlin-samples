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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showlineofsightbetweengeoelements.components.SceneViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    val sceneViewModel: SceneViewModel = viewModel()

    val currentZValue = sceneViewModel.currentZValue.collectAsState().value

    Scaffold(topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                SceneView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISScene = sceneViewModel.scene,
                    graphicsOverlays = listOf(sceneViewModel.graphicOverlay),
                    analysisOverlays = listOf(sceneViewModel.analysisOverlay)
                )

                Slider(
                    value = currentZValue.toFloat(),
                    onValueChange = { newHeight ->
                        sceneViewModel.updateHeight(newHeight.toDouble())
                    },

                    valueRange = 150f..(300f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                )
            }

        })
}
