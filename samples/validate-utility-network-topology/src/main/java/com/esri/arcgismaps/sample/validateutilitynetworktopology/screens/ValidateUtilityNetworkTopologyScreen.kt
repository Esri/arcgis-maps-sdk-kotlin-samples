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
@file:OptIn(ExperimentalMaterial3Api::class)

package com.esri.arcgismaps.sample.validateutilitynetworktopology.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.Color
import com.arcgismaps.data.CodedValue
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.validateutilitynetworktopology.components.ValidateUtilityNetworkTopologyViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun ValidateUtilityNetworkTopologyScreen(sampleName: String) {
    val viewModel: ValidateUtilityNetworkTopologyViewModel = viewModel()

    // On first composition, initialize the sample.
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    // Collect UI states from the ViewModel
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle("")
    val canGetState by viewModel.canGetState.collectAsStateWithLifecycle(false)
    val canValidate by viewModel.canValidateNetworkTopology.collectAsStateWithLifecycle(false)
    val canTrace by viewModel.canTrace.collectAsStateWithLifecycle(false)
    val canClearSelection by viewModel.canClearSelection.collectAsStateWithLifecycle(false)

    // For editing a feature's coded-value domain
    var fieldValueOptions by remember { mutableStateOf<List<CodedValue>>(listOf()) }
    val selectedFieldValue by viewModel.selectedFieldValue.collectAsStateWithLifecycle(null)
    val selectedFeature by viewModel.selectedFeature.collectAsStateWithLifecycle(null)
    var selectedFieldAlias by remember { mutableStateOf("") }

    // If we are currently editing an attribute, display "Edit Feature" dialog.
    var isEditingFeature by remember { mutableStateOf(false) }

    // Whenever the selectedFeature changes from null to non-null, we show the "edit" UI.
    LaunchedEffect(selectedFeature) {
        isEditingFeature = selectedFeature != null
        fieldValueOptions = viewModel.fieldValueOptions.value
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Column(modifier = Modifier.padding(padding)) {
                Box(modifier = Modifier.weight(1f)) {
                    MapView(
                        modifier = Modifier.fillMaxSize(),
                        arcGISMap = viewModel.arcGISMap,
                        mapViewProxy = viewModel.mapViewProxy,
                        graphicsOverlays = listOf(viewModel.graphicsOverlay),
                        selectionProperties = SelectionProperties(color = Color.yellow),
                        onVisibleAreaChanged = viewModel::updateVisibleArea,
                        onSingleTapConfirmed = { tapEvent ->
                            // Identify feature at the tapped location
                            viewModel.identifyFeatureAt(
                                screenCoordinate = tapEvent.screenCoordinate,
                                selectedFieldAlias = { selectedFieldAlias = it }
                            )
                        }
                    )

                    Log.e("RECOMPOSING", "MapView recomposed.")

                    // Status/Message text pinned near the top, with marquee if needed.
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                            .padding(8.dp)
                    )
                }

                // A row of sample operations: get state, validate, trace, clear
                // along with editing which is triggered by a selected feature.
                BottomRowOptions(
                    isGetStateEnabled = canGetState,
                    isValidateEnabled = canValidate,
                    isTraceEnabled = canTrace,
                    isClearSelectionEnabled = canClearSelection,
                    onGetStateSelected = viewModel::getState,
                    onValidateSelected = viewModel::validateNetworkTopology,
                    onTraceSelected = viewModel::trace,
                    onClearSelected = viewModel::clearSelection
                )
            }

            // If editing a feature, show a dialog with coded value choices and an "Apply" button.
            if (isEditingFeature && fieldValueOptions.isNotEmpty()) {
                ModalBottomSheet(
                    onDismissRequest = {
                        viewModel.clearSelection()
                        isEditingFeature = false
                    }
                ) {
                    EditFeatureFieldOptions(
                        selectedFieldValueName = selectedFieldValue?.name ?: "(None)",
                        fieldAliasName = selectedFieldAlias,
                        fieldValueOptions = fieldValueOptions,
                        onNewFieldValueSelected = viewModel::updateSelectedValue,
                        onCancelButtonSelected = {
                            viewModel.clearSelection()
                            isEditingFeature = false
                        },
                        onApplyEditsSelected = {
                            viewModel.applyEdits()
                            isEditingFeature = false
                        }
                    )
                }
            }

            // Handle error or info dialogs from the ViewModel
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
    )
}

@Composable
fun EditFeatureFieldOptions(
    selectedFieldValueName: String,
    fieldAliasName: String,
    fieldValueOptions: List<CodedValue>,
    onNewFieldValueSelected: (CodedValue) -> Unit,
    onCancelButtonSelected: () -> Unit,
    onApplyEditsSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .wrapContentSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onCancelButtonSelected) { Text("Cancel") }
            Text(
                text = "Edit Feature",
                style = MaterialTheme.typography.titleMedium
            )
            Button(onClick = onApplyEditsSelected) { Text("Apply") }
        }
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = fieldAliasName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Card(modifier = Modifier.padding(horizontal = 24.dp)) {
            LazyColumn {
                fieldValueOptions.forEachIndexed { index, codedValue ->
                    item {
                        Column(modifier = Modifier.clickable { onNewFieldValueSelected(codedValue) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = codedValue.name,
                                    fontWeight =
                                    if (codedValue.name == selectedFieldValueName)
                                        FontWeight.Bold
                                    else FontWeight.Normal
                                )
                                if (codedValue.name == selectedFieldValueName)
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected field value"
                                    )
                            }
                            if (index <= fieldValueOptions.lastIndex)
                                HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomRowOptions(
    isGetStateEnabled: Boolean,
    isValidateEnabled: Boolean,
    isTraceEnabled: Boolean,
    isClearSelectionEnabled: Boolean,
    onGetStateSelected: () -> Unit,
    onValidateSelected: () -> Unit,
    onTraceSelected: () -> Unit,
    onClearSelected: () -> Unit
) {
    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Button(
                onClick = onGetStateSelected,
                enabled = isGetStateEnabled
            ) { Text("Get State") }
        }

        item {
            Button(
                onClick = onValidateSelected,
                enabled = isValidateEnabled
            ) { Text("Validate") }
        }

        item {
            Button(
                onClick = onClearSelected,
                enabled = isClearSelectionEnabled
            ) { Text("Clear") }
        }

        item {
            Button(
                onClick = onTraceSelected,
                enabled = isTraceEnabled
            ) { Text("Trace") }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewValidateUtilityNetworkEditDialog() {
    SampleAppTheme {
        Surface {
            EditFeatureFieldOptions(
                selectedFieldValueName = "14.4 KV",
                fieldValueOptions = listOf(),
                onNewFieldValueSelected = { },
                onCancelButtonSelected = { },
                onApplyEditsSelected = { },
                fieldAliasName = "Nominal Voltage"
            )
        }
    }
}


@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewValidateUtilityNetworkOptions() {
    SampleAppTheme {
        Surface {
            BottomRowOptions(
                isGetStateEnabled = true,
                isValidateEnabled = true,
                isTraceEnabled = true,
                isClearSelectionEnabled = true,
                onGetStateSelected = { },
                onValidateSelected = { },
                onTraceSelected = { },
                onClearSelected = { }
            )
        }
    }
}
