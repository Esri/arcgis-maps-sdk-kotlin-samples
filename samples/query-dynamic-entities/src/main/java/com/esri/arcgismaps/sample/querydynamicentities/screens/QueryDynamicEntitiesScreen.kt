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

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.realtime.DynamicEntity
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.querydynamicentities.components.QueryDynamicEntitiesViewModel
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import java.util.Locale

@Composable
fun QueryDynamicEntitiesScreen(sampleName: String) {
    val viewModel: QueryDynamicEntitiesViewModel = viewModel()

    // UI states
    var isOptionsSheetVisible by remember { mutableStateOf(false) }
    var isResultsSheetVisible by remember { mutableStateOf(false) }
    var isFlightNumberDialogVisible by remember { mutableStateOf(false) }
    var flightNumber by remember { mutableStateOf("") }

    val isQueryRunning by viewModel.isQueryRunning.collectAsStateWithLifecycle(false)
    val queryEntities by viewModel.queryResultEntities.collectAsStateWithLifecycle(emptyList())
    val resultLabel by viewModel.resultLabel.collectAsStateWithLifecycle("")

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Column(modifier = Modifier.padding(padding)) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize(),
                    arcGISMap = viewModel.arcGISMap,
                    graphicsOverlays = listOf(viewModel.graphicsOverlay),
                    selectionProperties = SelectionProperties(color = Color.yellow),
                    onDown = {
                        // Dismiss any sheets when user starts interacting with the map
                        isOptionsSheetVisible = false
                    },
                    onPan = {
                        isOptionsSheetVisible = false
                    }
                )

                BottomSheet(
                    isVisible = isOptionsSheetVisible,
                    sheetTitle = "Query Flights",
                    onDismissRequest = { isOptionsSheetVisible = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = {
                            isOptionsSheetVisible = false
                            viewModel.queryFlightsWithinPhoenixBuffer()
                            isResultsSheetVisible = true
                        }) { Text("Within 15 Miles of PHX") }

                        Button(onClick = {
                            isOptionsSheetVisible = false
                            viewModel.queryFlightsArrivingInPHX()
                            isResultsSheetVisible = true
                        }) { Text("Arriving in PHX") }

                        Button(onClick = {
                            // show dialog to enter a flight number
                            isFlightNumberDialogVisible = true
                        }) { Text("With Flight Number") }
                    }
                }

                // Enter flight number dialog
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
                                    isOptionsSheetVisible = false
                                    viewModel.queryFlightsWithNumber(flightNumber)
                                    isResultsSheetVisible = true
                                }
                            ) { Text("Done") }
                        },
                        dismissButton = {
                            Button(onClick = { isFlightNumberDialogVisible = false }) { Text("Cancel") }
                        }
                    )
                }

                // Results bottom sheet
                BottomSheet(
                    isVisible = isResultsSheetVisible,
                    sheetTitle = resultLabel.ifEmpty { "Query Results" },
                    onDismissRequest = {
                        isResultsSheetVisible = false
                        viewModel.resetDisplay()
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (queryEntities.isEmpty() && !isQueryRunning) {
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
                                items(queryEntities, key = { it.hashCode() }) { entity ->
                                    DynamicEntityObservationItem(entity = entity)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                isResultsSheetVisible = false
                                viewModel.resetDisplay()
                            }) { Icon(Icons.Filled.Close, contentDescription = "Dismiss") }
                        }
                    }
                }

                // Loading dialog while querying
                if (isQueryRunning) {
                    LoadingDialog(loadingMessage = "Querying dynamic entities…")
                }

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
            }
        }
    )
}

@Composable
private fun DynamicEntityObservationItem(entity: DynamicEntity) {
    // Keep the latest attributes of this dynamic entity, update in real-time
    var attributes: Map<String, Any?> by remember { mutableStateOf(emptyMap()) }

    LaunchedEffect(entity) {
        attributes = entity.latestObservation?.attributes ?: emptyMap()
        // Collect live changes to display new observation attributes
        entity.dynamicEntityChangedEvent.collect { info ->
            attributes = info.receivedObservation?.attributes ?: emptyMap()
        }
    }

    val flightNumber = (attributes["flight_number"] as? String) ?: "N/A"

    Surface(tonalElevation = 1.dp) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = flightNumber,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            // Display sorted attributes with human-readable labels
            val pretty = attributes.entries
                .sortedBy { it.key }
                .filter { it.value != null }
                .map { labelForKey(it.key) to valueToString(it.value) }
            pretty.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
        is Double -> String.format(Locale.getDefault(),"%.2f", value)
        is Float -> String.format(Locale.getDefault(),"%.2f", value)
        is Number -> value.toString()
        is String -> value
        else -> value?.toString() ?: ""
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewQueryResultsItem() {
    SamplePreviewSurface {
        // We cannot preview live events, so show a simple surface
        Surface(tonalElevation = 1.dp) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Flight_396", style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Arrival Airport", style = MaterialTheme.typography.bodySmall)
                    Text("PHX", style = MaterialTheme.typography.bodySmall)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Status", style = MaterialTheme.typography.bodySmall)
                    Text("In flight", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
