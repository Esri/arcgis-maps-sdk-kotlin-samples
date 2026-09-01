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

package com.esri.arcgismaps.sample.browsebuildingfloors.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.browsebuildingfloors.R
import com.esri.arcgismaps.sample.browsebuildingfloors.components.BrowseBuildingFloorsViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.arcgismaps.toolkit.indoors.FloorFilter

/**
 * Main screen layout for the sample app
 */
@Composable
fun BrowseBuildingFloorsScreen(
    mapViewModel: BrowseBuildingFloorsViewModel = viewModel()
) {
    val mapViewModel: BrowseBuildingFloorsViewModel = viewModel()
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.browse_building_floors_app_name)) },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                MapView(
                    arcGISMap = mapViewModel.arcGISMap,
                    modifier = Modifier.fillMaxSize(),
                )
                FloorFilter(floorFilterState = mapViewModel.floorFilterState)
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
