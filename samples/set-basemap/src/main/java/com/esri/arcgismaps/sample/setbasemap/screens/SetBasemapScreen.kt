/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.setbasemap.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.basemapgallery.BasemapGallery
import com.arcgismaps.toolkit.basemapgallery.BasemapGalleryItem
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.setbasemap.components.SetBasemapViewModel

/**
 * Main screen layout for the sample app.
 * A FloatingActionButton opens a BottomSheet with a Basemap Gallery. The sheet
 * is dismissed when the user interacts with the map.
 */
@Composable
fun SetBasemapScreen(sampleName: String) {
    val mapViewModel: SetBasemapViewModel = viewModel()

    var isBottomSheetVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            if (!isBottomSheetVisible) {
                FloatingActionButton(onClick = { isBottomSheetVisible = true }) {
                    Icon(Icons.Filled.Map, contentDescription = "Show basemap gallery")
                }
            }
        },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                MapView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISMap = mapViewModel.arcGISMap,
                    onDown = { isBottomSheetVisible = false }
                )

                BottomSheet(
                    isVisible = isBottomSheetVisible,
                    sheetTitle = "Basemap Gallery",
                    onDismissRequest = { isBottomSheetVisible = false }
                ) {
                    BasemapGallery(
                        basemapGalleryItems = mapViewModel.basemapGalleryItems,
                        onItemClick = { item: BasemapGalleryItem ->
                            mapViewModel.onBasemapGalleryItemClick(item)
                            isBottomSheetVisible = false
                        }
                    )
                }
            }

            // Display a dialog if the sample encounters an error
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
