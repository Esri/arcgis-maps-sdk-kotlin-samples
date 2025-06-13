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

package com.esri.arcgismaps.sample.browseogcapifeatureservice.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.browseogcapifeatureservice.components.BrowseOgcApiFeatureServiceViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

@Composable
fun BrowseOgcApiFeatureServiceScreen(sampleName: String) {
    val mapViewModel: BrowseOgcApiFeatureServiceViewModel = viewModel()
    val featureCollectionTitles by mapViewModel.featureCollectionTitles.collectAsStateWithLifecycle()
    val selectedTitle = mapViewModel.selectedTitle
    val isUrlDialogVisible = mapViewModel.isUrlDialogVisible
    val urlInputText = mapViewModel.urlInputText

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy
                )
                LayerPickerBar(
                    featureCollectionTitles = featureCollectionTitles,
                    selectedTitle = selectedTitle,
                    onTitleSelected = mapViewModel::onFeatureCollectionTitleSelected,
                    onOpenUrlDialog = mapViewModel::onOpenUrlDialog
                )
            }
            if (isUrlDialogVisible) {
                OgcUrlInputDialog(
                    urlInputText = urlInputText,
                    onUrlInputChanged = mapViewModel::onUrlInputTextChanged,
                    onConfirm = mapViewModel::onConfirmUrlDialog,
                    onCancel = mapViewModel::onCancelUrlDialog
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayerPickerBar(
    featureCollectionTitles: List<String>,
    selectedTitle: String,
    onTitleSelected: (String) -> Unit,
    onOpenUrlDialog: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onOpenUrlDialog) {
                Text("Load Service")
            }
            if (featureCollectionTitles.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = selectedTitle,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Layers") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        featureCollectionTitles.forEachIndexed { idx, title ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(title) },
                                onClick = {
                                    expanded = false
                                    onTitleSelected(title)
                                }
                            )
                            if (idx < featureCollectionTitles.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OgcUrlInputDialog(
    urlInputText: String,
    onUrlInputChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Load OGC API feature service") },
        text = {
            Column {
                Text("Please provide a URL to an OGC API feature service.")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = urlInputText,
                    onValueChange = onUrlInputChanged,
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = urlInputText.isNotBlank()
            ) { Text("Load") }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
