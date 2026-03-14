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

package com.esri.arcgismaps.sample.querydynamicentities.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.Color
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.realtime.DynamicEntity
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.querydynamicentities.components.QueryDynamicEntitiesViewModel
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import java.util.Locale

private enum class SheetMode { Options, Results }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueryDynamicEntitiesScreen(sampleName: String) {
    val viewModel: QueryDynamicEntitiesViewModel = viewModel()

    // Bottom sheet state and visibility
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded)
    )
    var sheetMode by remember { mutableStateOf(SheetMode.Options) }

    // Flight number dialog
    var isFlightNumberDialogVisible by remember { mutableStateOf(false) }
    var flightNumber by remember { mutableStateOf("") }

    // Observe ViewModel states
    val isQueryRunning by viewModel.isQueryRunning.collectAsStateWithLifecycle(false)
    val queryEntities by viewModel.queryResultEntities.collectAsStateWithLifecycle(emptyList())
    val resultLabel by viewModel.resultLabel.collectAsStateWithLifecycle("")

    BottomSheetScaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        scaffoldState = scaffoldState,
        sheetContent = {
            QueryBottomSheet(
                sheetMode = sheetMode,
                resultLabel = resultLabel.ifEmpty { "Query Results" },
                entities = queryEntities,
                isQueryRunning = isQueryRunning,
                onBackFromResults = {
                    sheetMode = SheetMode.Options
                    viewModel.resetDisplay()
                },
                onWithinPhoenixSelected = {
                    sheetMode = SheetMode.Results
                    viewModel.queryFlightsWithinPhoenixBuffer()
                },
                onArrivingInPhoenixSelected = {
                    sheetMode = SheetMode.Results
                    viewModel.queryFlightsArrivingInPHX()
                },
                onFlightNumberSelected = {
                    isFlightNumberDialogVisible = true
                }
            )
        }
    ) { innerPadding ->
        MapView(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            arcGISMap = viewModel.arcGISMap,
            graphicsOverlays = listOf(viewModel.graphicsOverlay),
            selectionProperties = SelectionProperties(color = Color.yellow)
        )

        // Error dialog
        viewModel.messageDialogVM.apply {
            if (dialogStatus) {
                MessageDialog(
                    title = messageTitle,
                    description = messageDescription,
                    onDismissRequest = ::dismissDialog
                )
            }
        }

        // Dialog to input a flight number
        if (isFlightNumberDialogVisible) {
            AlertDialog(
                onDismissRequest = { isFlightNumberDialogVisible = false },
                title = { Text("Enter a Flight Number to Query") },
                text = {
                    OutlinedTextField(
                        value = flightNumber,
                        onValueChange = { flightNumber = it },
                        singleLine = true,
                        label = { Text("Flight Number") }
                    )
                },
                confirmButton = {
                    Button(
                        enabled = flightNumber.isNotBlank(),
                        onClick = {
                            isFlightNumberDialogVisible = false
                            sheetMode = SheetMode.Results
                            viewModel.queryFlightsWithNumber(flightNumber)
                        }
                    ) { Text("Done") }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { isFlightNumberDialogVisible = false }
                    ) { Text("Cancel") }
                }
            )
        }

        // Loading dialog while querying
        if (isQueryRunning) {
            LoadingDialog(loadingMessage = "Querying dynamic entities…")
        }
    }
}

@Composable
private fun QueryBottomSheet(
    sheetMode: SheetMode,
    resultLabel: String,
    entities: List<DynamicEntity>,
    isQueryRunning: Boolean,
    onBackFromResults: () -> Unit,
    onWithinPhoenixSelected: () -> Unit,
    onArrivingInPhoenixSelected: () -> Unit,
    onFlightNumberSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHeader(
            title = when (sheetMode) {
                SheetMode.Options -> "Query Flights"
                SheetMode.Results -> resultLabel
            },
            showBack = sheetMode == SheetMode.Results,
            onBack = onBackFromResults
        )

        when (sheetMode) {
            SheetMode.Options -> QueryFlightsMenu(
                onWithinPhoenixSelected = onWithinPhoenixSelected,
                onArrivingInPhoenixSelected = onArrivingInPhoenixSelected,
                onFlightNumberSelected = onFlightNumberSelected
            )

            SheetMode.Results -> QueryResultsList(
                entities = entities,
                isQueryRunning = isQueryRunning
            )
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun QueryFlightsMenu(
    onWithinPhoenixSelected: () -> Unit,
    onArrivingInPhoenixSelected: () -> Unit,
    onFlightNumberSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onWithinPhoenixSelected) { Text("Within 15 Miles of PHX") }
        Button(onClick = onArrivingInPhoenixSelected) { Text("Arriving in PHX") }
        Button(onClick = onFlightNumberSelected) { Text("With Flight Number") }
    }
}

@Composable
private fun QueryResultsList(
    entities: List<DynamicEntity>,
    isQueryRunning: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (entities.isEmpty() && !isQueryRunning) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.AirplanemodeActive, contentDescription = null)
                Text("No Results", style = MaterialTheme.typography.titleMedium)
                Text(
                    "There are no flights to display for this query.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entities, key = { it.hashCode() }) { entity ->
                    DynamicEntityObservationItem(entity = entity)
                }
            }
        }
    }
}

@Composable
private fun DynamicEntityObservationItem(entity: DynamicEntity) {
    var attributes by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }

    LaunchedEffect(entity) {
        attributes = entity.latestObservation?.attributes ?: emptyMap()
        entity.dynamicEntityChangedEvent.collect { info ->
            attributes = info.receivedObservation?.attributes ?: emptyMap()
        }
    }

    val flightNumber = (attributes["flight_number"] as? String) ?: "N/A"

    Surface(tonalElevation = 1.dp) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = flightNumber,
                style = MaterialTheme.typography.titleSmall
            )
            val pretty = attributes.entries
                .sortedBy { it.key }
                .filter { it.value != null }
                .map { labelForKey(it.key) to valueToString(it.value) }
            pretty.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall)
                    Text(value, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun labelForKey(key: String): String {
    return when (key) {
        "aircraft" -> "Aircraft"
        "altitude_feet" -> "Altitude (ft)"
        "arrival_airport" -> "Arrival Airport"
        "flight_number" -> "Flight Number"
        "heading" -> "Heading"
        "speed" -> "Speed"
        "status" -> "Status"
        else -> key
    }
}

private fun valueToString(value: Any?): String {
    return when (value) {
        is Double -> String.format(Locale.getDefault(), "%.2f", value)
        is Float -> String.format(Locale.getDefault(), "%.2f", value)
        is Number -> value.toString()
        is String -> value
        else -> value?.toString() ?: ""
    }
}
