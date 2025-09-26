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

package com.esri.arcgismaps.sample.monitorchangestodrawstatus.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.monitorchangestodrawstatus.components.MonitorDrawStatusViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface

/**
 * Main screen for the Monitor changes to draw status sample.
 *
 * The composable observes the ViewModel's mapIsDrawing flow and updates the UI
 * when the draw status changes. The MapView's onDrawStatusChanged callback
 * is used to notify the ViewModel of draw status changes.
 */
@Composable
fun MonitorDrawStatusScreen(sampleName: String) {
    val viewModel: MonitorDrawStatusViewModel = viewModel()

    // Observe whether the map is currently drawing
    val mapIsDrawing by viewModel.mapIsDrawing.collectAsStateWithLifecycle(false)

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(padding)) {

                Box(modifier = Modifier.fillMaxSize()) {
                    // Pass in the ArcGISMap from the ViewModel and the draw status callback.
                    MapView(
                        modifier = Modifier
                            .fillMaxSize(),
                        arcGISMap = viewModel.arcGISMap,
                        // Forward draw status updates to the ViewModel
                        onDrawStatusChanged = viewModel::updateDrawStatus
                    )

                    // Top overlay text showing current draw status
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            text = if (mapIsDrawing) "Drawing…" else "Drawing completed.",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // Center overlay: show a circular progress indicator while drawing
                    if (mapIsDrawing) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .shadow(8.dp),
                            tonalElevation = 8.dp
                        ) {
                            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                        }
                    }
                }

                // Message dialog shown when ViewModel surfaces an error
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
    )
}
