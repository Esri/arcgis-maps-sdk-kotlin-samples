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

package com.esri.arcgismaps.sample.setspatialreference.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.setspatialreference.components.SetSpatialReferenceViewModel

/**
 * Simple screen that displays a MapView using a map created with a custom spatial reference.
 * The ViewModel is responsible for constructing and loading the ArcGISMap.
 */
@Composable
fun SetSpatialReferenceScreen(sampleName: String) {
    // Obtain the ViewModel scoped to this composable
    val viewModel: SetSpatialReferenceViewModel = viewModel()

    Scaffold(topBar = { SampleTopAppBar(title = sampleName) }) { padding ->
        MapView(
            modifier = Modifier.fillMaxSize()
                .padding(padding),
            arcGISMap = viewModel.arcGISMap
        )

        // Display an error dialog if the ViewModel requests it
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

@Composable
fun PreviewSetSpatialReferenceScreen() {
    SamplePreviewSurface {
        SetSpatialReferenceScreen(sampleName = "Set spatial reference")
    }
}
