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

package com.esri.arcgismaps.sample.addfeaturelayers.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.addfeaturelayers.R
import com.esri.arcgismaps.sample.addfeaturelayers.components.AddFeatureLayersViewModel
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun AddFeatureLayersScreen(
    mapViewModel: AddFeatureLayersViewModel = viewModel()
) {
    // Track the visibility of the bottom sheet
    var isBottomSheetVisible by remember { mutableStateOf(true) }

    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.add_feature_layers_app_name)) },
        floatingActionButton = {
            //Only show FAB when the bottom sheet is closed
            if (!isBottomSheetVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { isBottomSheetVisible = true }
                ) { Icon(Icons.Filled.Add, contentDescription = "Show options") }
            }
        },
        content = { innerPadding ->
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                arcGISMap = mapViewModel.arcGISMap,
                mapViewProxy = mapViewModel.mapViewProxy,
                onDown = { isBottomSheetVisible = false }
            )

            BottomSheet(
                isVisible = isBottomSheetVisible,
                sheetTitle = "Feature layer source",
                onDismissRequest = { isBottomSheetVisible = false }
            ) {
                FeatureLayerSourceMenuSelector(
                    selectedFeatureLayerSource = mapViewModel.selectedFeatureLayerSource,
                    // Forward the tapped dropdown index to the ViewModel
                    onFeatureLayerSourceSelected = mapViewModel::onFeatureLayerSourceSelected
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
fun FeatureLayerSourceMenuSelector(
    selectedFeatureLayerSource: AddFeatureLayersViewModel.FeatureLayerSource,
    onFeatureLayerSourceSelected: (AddFeatureLayersViewModel.FeatureLayerSource) -> Unit = {}
) {
    DropDownMenuBox(
        textFieldValue = selectedFeatureLayerSource.label,
        textFieldLabel = "Select a feature layer source",
        // Populate the selector to map a list of each entry from the view model enum.
        dropDownItemList = AddFeatureLayersViewModel.FeatureLayerSource.entries.map { it.label },
        onIndexSelected = { index -> onFeatureLayerSourceSelected(AddFeatureLayersViewModel.FeatureLayerSource.entries[index]) }
    )

}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun BottomSheetPreview() {
    SamplePreviewSurface {
        BottomSheet(
            isVisible = true,
            sheetTitle = "Feature layer source",
        ) {
            FeatureLayerSourceMenuSelector(AddFeatureLayersViewModel.FeatureLayerSource.GEOPACKAGE)
        }
    }
}
