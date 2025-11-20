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

package com.esri.arcgismaps.sample.setinitialviewpoint.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.setinitialviewpoint.components.SetInitialViewpointViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Screen composable that hosts the MapView and observes the ViewModel.
 * The MapView receives the ArcGISMap from the ViewModel which contains the initial viewpoint.
 */
@Composable
fun SetInitialViewpointScreen(sampleName: String) {
    val mapViewModel: SetInitialViewpointViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) }
    ) { paddingValues ->
        // MapView displays the map configured in the ViewModel
        MapView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            arcGISMap = mapViewModel.arcGISMap
        )
    }

    // Show message dialog when the viewmodel reports an error
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

