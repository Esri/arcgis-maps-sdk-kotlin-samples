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

package com.esri.arcgismaps.sample.showextrudedfeatures.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.showextrudedfeatures.components.ShowExtrudedFeaturesViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen containing a SceneView and simple UI to switch extrusion expressions.
 */
@Composable
fun ShowExtrudedFeaturesScreen(sampleName: String) {
    val viewModel: ShowExtrudedFeaturesViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { paddingValues ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)) {

                // SceneView composable from the toolkit renders the 3D scene
                Box(modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)) {
                    SceneView(
                        modifier = Modifier.fillMaxSize(),
                        arcGISScene = viewModel.arcGISScene
                    )
                }

                // Controls for switching the extrusion statistic
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val current = viewModel.currentStatistic
                    Button(
                        onClick = { viewModel.updateExtrusionStatistic(ShowExtrudedFeaturesViewModel.Statistic.TotalPopulation) },
                        enabled = current != ShowExtrudedFeaturesViewModel.Statistic.TotalPopulation
                    ) {
                        Text(text = ShowExtrudedFeaturesViewModel.Statistic.TotalPopulation.label)
                    }

                    Button(
                        onClick = { viewModel.updateExtrusionStatistic(ShowExtrudedFeaturesViewModel.Statistic.PopulationDensity) },
                        enabled = current != ShowExtrudedFeaturesViewModel.Statistic.PopulationDensity
                    ) {
                        Text(text = ShowExtrudedFeaturesViewModel.Statistic.PopulationDensity.label)
                    }
                }

                // Show a message dialog if an error occurred in the ViewModel
                viewModel.messageDialogVM.apply {
                    if (dialogStatus) {
                        MessageDialog(
                            title = messageTitle,
                            description = messageDescription,
                            icon = Icons.Default.Info,
                            onDismissRequest = ::dismissDialog
                        )
                    }
                }
            }
        }
    )
}
