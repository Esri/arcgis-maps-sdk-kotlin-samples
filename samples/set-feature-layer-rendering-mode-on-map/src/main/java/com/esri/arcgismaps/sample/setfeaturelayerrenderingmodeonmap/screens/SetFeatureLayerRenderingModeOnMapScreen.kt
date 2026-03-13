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

package com.esri.arcgismaps.sample.setfeaturelayerrenderingmodeonmap.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.setfeaturelayerrenderingmodeonmap.components.SetFeatureLayerRenderingModeOnMapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen showing two MapViews: one with dynamic rendering and one with static rendering.
 * A single Zoom button animates both maps to the same viewpoint to compare
 * differences in rendering behavior.
 */
@Composable
fun SetFeatureLayerRenderingModeOnMapScreen(sampleName: String) {
    val viewModel: SetFeatureLayerRenderingModeOnMapViewModel = viewModel()
    val isZoomedIn by viewModel.isZoomedIn.collectAsStateWithLifecycle()


    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Dynamic rendering MapView
            Box(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize(),
                    arcGISMap = viewModel.arcGISMapDynamic,
                    mapViewProxy = viewModel.mapViewProxyDynamic
                )

                // Label overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "Dynamic",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Static rendering MapView
            Box(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize(),
                    arcGISMap = viewModel.arcGISMapStatic,
                    mapViewProxy = viewModel.mapViewProxyStatic
                )

                // Label overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "Static",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Button(modifier = Modifier.padding(12.dp), onClick = viewModel::onZoomButtonClicked) {
                Text(if (isZoomedIn) "Zoom Out" else "Zoom In")
            }


            // Display any message dialogs raised by the ViewModel
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
}
