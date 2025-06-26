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

package com.esri.arcgismaps.sample.editfeatureswithfeaturelinkedannotation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.editfeatureswithfeaturelinkedannotation.components.EditFeaturesWithFeatureLinkedAnnotationViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

@Composable
fun EditFeaturesWithFeatureLinkedAnnotationScreen(sampleName: String) {
    val mapViewModel: EditFeaturesWithFeatureLinkedAnnotationViewModel = viewModel()

    val currentInstruction by mapViewModel.instruction.collectAsState()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MapView(
                        modifier = Modifier
                            .fillMaxSize(),
                        arcGISMap = mapViewModel.arcGISMap,
                        mapViewProxy = mapViewModel.mapViewProxy,
                        onSingleTapConfirmed = { tapEvent ->
                            tapEvent.mapPoint?.let { mapPoint ->
                                mapViewModel.onMapSingleTap(
                                    screenCoordinate = tapEvent.screenCoordinate,
                                    mapPoint = mapPoint
                                )
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentInstruction.message,
                            color = Color.White
                        )
                    }
                }
                if (mapViewModel.showEditAddressDialog) {
                    EditAddressDialog(
                        buildingNumber = mapViewModel.buildingNumber.toString(),
                        streetName = mapViewModel.streetName,
                        onBuildingNumberChange = { mapViewModel.onBuildingNumberChange(it.toInt()) },
                        onStreetNameChange = { mapViewModel.onStreetNameChange(it) },
                        onDismiss = { mapViewModel.onShowEditAddressDialogChange(false) },
                        onConfirm = {
                            mapViewModel.onEditAddressConfirmed(
                                mapViewModel.selectedFeature,
                                mapViewModel.buildingNumber,
                                mapViewModel.streetName
                            )
                        }
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
        }
    )
}

@Composable
fun EditAddressDialog(
    buildingNumber: String,
    streetName: String,
    onBuildingNumberChange: (Int) -> Unit,
    onStreetNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Address") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = buildingNumber,
                    onValueChange = onBuildingNumberChange,
                    label = { Text("Building Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = streetName,
                    onValueChange = onStreetNameChange,
                    label = { Text("Street Name") },
                    singleLine = true
                )
                Text("Edit the feature's 'AD_ADDRESS' and 'ST_STR_NAM' attributes.")
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = buildingNumber.isNotBlank() && streetName.isNotBlank()
            ) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
