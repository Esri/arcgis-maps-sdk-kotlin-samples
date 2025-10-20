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

package com.esri.arcgismaps.sample.displayalternatesymbolsatdifferentscales.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.displayalternatesymbolsatdifferentscales.components.DisplayAlternateSymbolsAtDifferentScalesViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample.
 */
@Composable
fun DisplayAlternateSymbolsAtDifferentScalesScreen(sampleName: String) {
    val mapViewModel: DisplayAlternateSymbolsAtDifferentScalesViewModel = viewModel()

    // Collect the current scale
    val currentScale by mapViewModel.currentScale.collectAsStateWithLifecycle()

    Scaffold(topBar = { SampleTopAppBar(title = sampleName) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MapView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                arcGISMap = mapViewModel.arcGISMap,
                mapViewProxy = mapViewModel.mapViewProxy,
                onViewpointChangedForCenterAndScale = { newViewpoint: Viewpoint? ->
                    // Update only the scale in the view model when viewpoint changes
                    mapViewModel.updateScale(newViewpoint?.targetScale)
                }
            )

            // Controls displayed below the MapView
            RendererControls(
                scale = currentScale,
                onReset = mapViewModel::resetViewpoint
            )
        }

        // Show message dialogs for errors
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
}

@Composable
private fun RendererControls(scale: Double?, onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Display the current scale; format if available
        val scaleText = scale?.let {
            String.format(null, "Scale: 1:%,d", it.toInt())
        } ?: "Scale: N/A"

        Text(text = scaleText, style = MaterialTheme.typography.titleMedium)
        // Reset viewpoint button
        OutlinedButton(onClick = onReset) {
            Text(text = "Reset Viewpoint")
        }
    }
}
