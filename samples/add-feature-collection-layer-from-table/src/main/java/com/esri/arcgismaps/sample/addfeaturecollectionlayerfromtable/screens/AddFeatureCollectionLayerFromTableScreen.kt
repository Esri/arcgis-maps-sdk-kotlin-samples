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

package com.esri.arcgismaps.sample.addfeaturecollectionlayerfromtable.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.addfeaturecollectionlayerfromtable.components.AddFeatureCollectionLayerFromTableViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample.
 * Displays a MapView and relies on the ViewModel to create the feature collection layer.
 */
@Composable
fun AddFeatureCollectionLayerFromTableScreen(sampleName: String) {
    val mapViewModel: AddFeatureCollectionLayerFromTableViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { paddingValues ->
            // MapView displays the map created/managed by the ViewModel.
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                arcGISMap = mapViewModel.arcGISMap
            )
            // Display any error messages surfaced by the ViewModel
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
