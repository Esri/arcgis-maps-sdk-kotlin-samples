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

package com.esri.arcgismaps.sample.monitorchangestolayerviewstate.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.monitorchangestolayerviewstate.components.MonitorChangesToLayerViewStateViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app. Observes flows exposed by the ViewModel and
 * wires MapView callbacks back into the ViewModel.
 */
@Composable
fun MonitorChangesToLayerViewStateScreen(sampleName: String) {
    val mapViewModel: MonitorChangesToLayerViewStateViewModel = viewModel()
    // Collect flows from the view model
    val layerIsVisible by mapViewModel.layerIsVisibleFlow.collectAsStateWithLifecycle()
    val layerStatusLabels by mapViewModel.layerStatusLabelsFlow.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // MapView fills the available space
                MapView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    arcGISMap = mapViewModel.arcGISMap,
                    // Listen for layer view state changes and forward to the view model
                    onLayerViewStateChanged = { layerStateChanged ->
                        mapViewModel.onLayerViewStateChanged(
                            layer = layerStateChanged.layer,
                            layerViewStatusList = layerStateChanged.layerViewState.status.toList()
                        )
                    }
                )
                // Controls are hoisted into a composable function
                LayerControls(
                    layerIsVisible = layerIsVisible,
                    layerStatusLabels = layerStatusLabels,
                    onToggleVisibility = mapViewModel::updateLayerVisibility
                )
            }
            // Show message dialog if the view model reported an error
            mapViewModel.messageDialogVM.apply {
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

@Composable
private fun LayerControls(
    layerIsVisible: Boolean,
    layerStatusLabels: List<String>,
    onToggleVisibility: (Boolean) -> Unit
) {
    Card(modifier = Modifier.padding(12.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (layerIsVisible) "Feature layer: Visible" else "Feature layer: Hidden",
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(checked = layerIsVisible, onCheckedChange = onToggleVisibility)
            }

            // Display the current LayerViewState labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Layer view status:", style = MaterialTheme.typography.titleMedium)
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = layerStatusLabels.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Preview
@Composable
private fun LayerControlsPreview() {
    SamplePreviewSurface {
        LayerControls(
            layerIsVisible = true,
            layerStatusLabels = listOf("Active"),
            onToggleVisibility = { }
        )
    }
}
