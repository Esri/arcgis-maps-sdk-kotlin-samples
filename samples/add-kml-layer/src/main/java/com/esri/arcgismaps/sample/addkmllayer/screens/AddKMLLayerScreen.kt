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

package com.esri.arcgismaps.sample.addkmllayer.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.addkmllayer.components.AddKmlLayerViewModel
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.ProgressDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the Add KML Layer sample.
 */
@Composable
fun AddKMLLayerScreen(sampleName: String) {
    val mapViewModel: AddKmlLayerViewModel = viewModel()
    // Observe the current selected KML option for the DropDown text field.
    val selectedKmlOption by mapViewModel.selectedKmlOption.collectAsStateWithLifecycle()
    // Observe loading state from the viewmodel to show loading dialog.
    val isLoading by mapViewModel.isLoading.collectAsStateWithLifecycle(false)

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MapView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy
                )
                // Drop down menu to switch between KML sources.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    DropDownMenuBox(
                        textFieldValue = selectedKmlOption.label,
                        textFieldLabel = "KML Source",
                        dropDownItemList = mapViewModel.kmlOptions.map { it.label },
                        onIndexSelected = mapViewModel::setKmlLayer
                    )
                }
            }

            // Display dialog while loading a KML layer.
            if (isLoading) {
                ProgressDialog(message = "Loading KmlLayer...")
            }

            // Display errors in a dialog.
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
