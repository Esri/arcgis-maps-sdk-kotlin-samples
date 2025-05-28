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

package com.esri.arcgismaps.sample.displaymapfromportalitem.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.displaymapfromportalitem.components.DisplayMapFromPortalItemViewModel
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app.
 */
@Composable
fun DisplayMapFromPortalItemScreen(sampleName: String) {
    val mapViewModel: DisplayMapFromPortalItemViewModel = viewModel()
    val mapOptions = mapViewModel.mapOptions
    val currentMapOption = mapViewModel.currentMapOption

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap
                )
                MapSelectionDropDown(
                    mapOptions = mapOptions,
                    selectedOption = currentMapOption,
                    onOptionSelected = mapViewModel::onMapOptionSelected
                )
            }

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
private fun MapSelectionDropDown(
    mapOptions: List<DisplayMapFromPortalItemViewModel.PortalItemMap>,
    selectedOption: DisplayMapFromPortalItemViewModel.PortalItemMap,
    onOptionSelected: (DisplayMapFromPortalItemViewModel.PortalItemMap) -> Unit
) {
    val titles = remember(mapOptions) { mapOptions.map { it.title } }
    val selectedIndex = mapOptions.indexOf(selectedOption).coerceAtLeast(0)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DropDownMenuBox(
            modifier = Modifier.animateContentSize(),
            textFieldValue = titles.getOrNull(selectedIndex) ?: "",
            textFieldLabel = "Maps",
            dropDownItemList = titles,
            onIndexSelected = { index ->
                mapOptions.getOrNull(index)?.let { onOptionSelected(it) }
            }
        )
    }
}
