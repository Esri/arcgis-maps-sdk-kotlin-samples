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

package com.esri.arcgismaps.sample.applyuniquevaluerenderer.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.applyuniquevaluerenderer.components.ApplyUniqueValueRendererViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the Apply Unique Value Renderer sample.
 * The ViewModel constructs the map, adds the feature layer and applies
 * the UniqueValueRenderer. The screen simply displays the MapView and
 * presents any message dialogs emitted by the ViewModel.
 */
@Composable
fun ApplyUniqueValueRendererScreen(sampleName: String) {
    val mapViewModel: ApplyUniqueValueRendererViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // MapView expects the ArcGISMap instance produced by the ViewModel
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                arcGISMap = mapViewModel.arcGISMap
            )

            // Display any error messages from the ViewModel
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
}
