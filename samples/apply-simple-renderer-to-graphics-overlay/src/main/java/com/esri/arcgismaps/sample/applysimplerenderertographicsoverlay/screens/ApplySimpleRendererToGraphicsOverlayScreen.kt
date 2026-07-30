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

package com.esri.arcgismaps.sample.applysimplerenderertographicsoverlay.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.applysimplerenderertographicsoverlay.components.ApplySimpleRendererToGraphicsOverlayViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlin.math.roundToInt

/**
 * Main screen layout for the sample app.
 */
@Composable
fun ApplySimpleRendererToGraphicsOverlayScreen(sampleName: String) {
    val mapViewModel: ApplySimpleRendererToGraphicsOverlayViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    graphicsOverlays = if (mapViewModel.showOverlays) mapViewModel.graphicsOverlays else emptyList()
                )

                ReproControls(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    graphicCountOptions = mapViewModel.graphicCountOptions,
                    selectedGraphicCountIndex = mapViewModel.selectedGraphicCountIndex,
                    onGraphicCountChanged = mapViewModel::updateGraphicCount,
                    onReproduce = mapViewModel::reproduceIssue,
                    onClear = mapViewModel::clearOverlays,
                    status = mapViewModel.status,
                    isWorking = mapViewModel.isWorking
                )
            }
            // Show error dialog if needed
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
private fun ReproControls(
    modifier: Modifier = Modifier,
    graphicCountOptions: List<Int>,
    selectedGraphicCountIndex: Int,
    onGraphicCountChanged: (Int) -> Unit,
    onReproduce: () -> Unit,
    onClear: () -> Unit,
    status: String,
    isWorking: Boolean
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Graphics: ${graphicCountOptions[selectedGraphicCountIndex].formatCount()}",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = selectedGraphicCountIndex.toFloat(),
            valueRange = 0f..(graphicCountOptions.lastIndex).toFloat(),
            steps = graphicCountOptions.size - 2,
            enabled = !isWorking,
            onValueChange = { onGraphicCountChanged(it.roundToInt()) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onReproduce,
                enabled = !isWorking
            ) { Text("Prepare + register") }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onClear,
                enabled = !isWorking
            ) { Text("Clear") }
        }

        if (isWorking) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Text(text = status, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun Int.formatCount(): String = "% ,d".replace(" ", "").format(this)
