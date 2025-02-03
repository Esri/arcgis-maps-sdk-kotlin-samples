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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.Color
import com.arcgismaps.data.CodedValue
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.validateutilitynetworktopology.components.ValidateUtilityNetworkTopologyViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun ValidateUtilityNetworkTopologyScreen(sampleName: String) {
    val mapViewModel: ValidateUtilityNetworkTopologyViewModel = viewModel()

    // On first composition, initialize the sample.
    var isViewmodelInitialized by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!isViewmodelInitialized) {
            mapViewModel.initialize()
            isViewmodelInitialized = true
        }
    }
    // Display loading dialog on initialization
    if (!isViewmodelInitialized) {
        LoadingDialog("Loading utility network")
    }

    // Collect UI states from the ViewModel
    val statusMessage by mapViewModel.statusMessage.collectAsStateWithLifecycle("")
    val canGetState by mapViewModel.canGetState.collectAsStateWithLifecycle(false)
    val canValidate by mapViewModel.canValidateNetworkTopology.collectAsStateWithLifecycle(false)
    val canTrace by mapViewModel.canTrace.collectAsStateWithLifecycle(false)
    val canClearSelection by mapViewModel.canClearSelection.collectAsStateWithLifecycle(false)

    // For editing a feature's coded-value domain
    var fieldValueOptions by remember { mutableStateOf<List<CodedValue>>(listOf()) }
    val selectedFieldValue by mapViewModel.selectedFieldValue.collectAsStateWithLifecycle(null)
    val selectedFeature by mapViewModel.selectedFeature.collectAsStateWithLifecycle(null)
    var selectedFieldAlias by remember { mutableStateOf("") }

    // If we are currently editing an attribute, display bottom sheet.
    var isEditingFeature by remember { mutableStateOf(false) }

    // Track when the selected feature changes to display bottom sheet
    LaunchedEffect(selectedFeature) {
        isEditingFeature = selectedFeature != null
        fieldValueOptions = mapViewModel.fieldValueOptions.value
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    selectionProperties = SelectionProperties(color = Color.cyan),
                    onVisibleAreaChanged = mapViewModel::updateVisibleArea,
                    onSingleTapConfirmed = { tapEvent ->
                        // Identify feature at the tapped location
                        mapViewModel.identifyFeatureAt(
                            screenCoordinate = tapEvent.screenCoordinate,
                            selectedFieldAlias = { selectedFieldAlias = it }
                        )
                    }
                )
                // Display collapsable status message on top of MapView
                NetworkStatusMessage(Modifier.align(Alignment.TopCenter), statusMessage)
                // Display bottom sheet if editing a feature attribute
                if (isEditingFeature && fieldValueOptions.isNotEmpty()) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            // Clear selection and close sheet
                            mapViewModel.clearFeatureEditSelection()
                            isEditingFeature = false
                        }
                    ) {
                        // Edit feature attribute layout
                        EditFeatureFieldOptions(
                            selectedFieldValueName = selectedFieldValue?.name ?: "(None)",
                            fieldAliasName = selectedFieldAlias,
                            fieldValueOptions = fieldValueOptions,
                            onNewFieldValueSelected = mapViewModel::updateSelectedValue,
                            onCancelButtonSelected = {
                                // Clear selection and close sheet
                                mapViewModel.clearFeatureEditSelection()
                                isEditingFeature = false
                            },
                            onApplyEditsSelected = {
                                // Apply edits and close sheet
                                mapViewModel.applyEdits()
                                isEditingFeature = false
                            }
                        )
                    }
                }
            }
            // Handle error or info dialogs from the ViewModel
            mapViewModel.messageDialogVM.apply {
                if (dialogStatus) {
                    MessageDialog(
                        title = messageTitle,
                        description = messageDescription,
                        onDismissRequest = ::dismissDialog
                    )
                }
            }
        },
        bottomBar = {
            // Options to get state, validate, trace or clear features.
            BottomRowOptions(
                isGetStateEnabled = canGetState,
                isValidateEnabled = canValidate,
                isTraceEnabled = canTrace,
                isClearSelectionEnabled = canClearSelection,
                onGetStateSelected = mapViewModel::getState,
                onValidateSelected = mapViewModel::validateNetworkTopology,
                onTraceSelected = mapViewModel::trace,
                onClearSelected = mapViewModel::clearFeatureEditSelection
            )
        }
    )
}

@Composable
fun NetworkStatusMessage(modifier: Modifier, statusMessage: String) {
    var isCollapsed by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            .padding(8.dp)
            .animateContentSize()
    ) {
        Text(
            text = statusMessage,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            style = MaterialTheme.typography.labelLarge,
            maxLines = if (isCollapsed) 1 else 100,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        OutlinedIconButton(
            modifier = Modifier.size(20.dp),
            onClick = { isCollapsed = !isCollapsed }
        ) {
            Icon(
                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = "Expand/Collapse"
            )
        }
    }

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
            Button(onClick = onApplyEditsSelected) { Text("Apply Edits") }
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
            .padding(vertical = 4.dp, horizontal = 24.dp),
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Button(
                onClick = onClearSelected,
                enabled = isClearSelectionEnabled
            ) { Text("Clear") }
        }
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
