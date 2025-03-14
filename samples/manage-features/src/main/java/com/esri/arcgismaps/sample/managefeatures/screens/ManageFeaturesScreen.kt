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

package com.esri.arcgismaps.sample.managefeatures.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.managefeatures.components.ManageFeaturesViewModel
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.launch

/**
 * Main screen layout for the sample app
 */
@Composable
fun ManageFeaturesScreen(sampleName: String) {
    val mapViewModel: ManageFeaturesViewModel = viewModel()

    var featureManagementDropdownIndex by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        snackbarHost = {
            SnackbarHost(
                modifier = Modifier.padding(bottom = 128.dp),
                hostState = snackbarHostState
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MapView(
                    modifier = Modifier
                        .weight(1f),
                    mapViewProxy = mapViewModel.mapViewProxy,
                    arcGISMap = mapViewModel.arcGISMap,
                    onSingleTapConfirmed = mapViewModel::onTap,
                ) {
                    mapViewModel.selectedFeature?.let { selectedFeature ->
                        // Only show the delete button when on the delete feature operation and a feature is selected.
                        if (mapViewModel.currentFeatureOperation == mapViewModel.manageFeaturesList[1]) {
                            Callout(geoElement = selectedFeature) {
                                Button(onClick = mapViewModel::deleteSelectedFeature) {
                                    Text(text = "Delete")
                                }
                            }
                        }
                        // Only show the dropdown for damage type when on the update feature operation.
                        if (mapViewModel.currentFeatureOperation == mapViewModel.manageFeaturesList[2]) {
                            Callout(geoElement = selectedFeature) {
                                DropDownMenuBox(
                                    modifier = Modifier
                                        .padding(8.dp),
                                    textFieldLabel = "Select damage type",
                                    textFieldValue = mapViewModel.currentDamageType,
                                    dropDownItemList = mapViewModel.damageTypeList,
                                    onIndexSelected = { index ->
                                        if (mapViewModel.selectedFeature != null) {
                                            mapViewModel.onDamageTypeSelected(index)
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Please select a feature to update")
                                            }
                                        }
                                    })
                            }
                        }
                    }
                }
                // Start of drop down and instruction UI.
                Row(
                    modifier = Modifier
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Show the dropdown for feature management operations.
                    DropDownMenuBox(
                        modifier = Modifier.padding(end = 8.dp),
                        textFieldLabel = "Feature management operation",
                        textFieldValue = mapViewModel.currentFeatureOperation,
                        dropDownItemList = mapViewModel.manageFeaturesList,
                        onIndexSelected = { index ->
                            mapViewModel.onFeatureOperationSelected(index)
                            featureManagementDropdownIndex = index
                        })
                }
                // Show instructions for the current feature operation.
                Text(
                    text = mapViewModel.instructionsList[featureManagementDropdownIndex],
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp)
                        .animateContentSize()
                )
            }
            // Show snack bar messages with information about feature operations.
            if (mapViewModel.snackBarMessage != "") {
                LaunchedEffect(mapViewModel.snackBarMessage) {
                    snackbarHostState.showSnackbar(mapViewModel.snackBarMessage)
                }
            }
            // Show any errors in a message dialog.
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
