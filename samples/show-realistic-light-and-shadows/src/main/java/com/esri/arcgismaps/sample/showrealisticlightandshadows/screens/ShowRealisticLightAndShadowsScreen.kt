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

package com.esri.arcgismaps.sample.showrealisticlightandshadows.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.view.LightingMode
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showrealisticlightandshadows.components.ShowRealisticLightAndShadowsViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Main screen layout for the sample app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowRealisticLightAndShadowsScreen(sampleName: String) {

    val mapViewModel: ShowRealisticLightAndShadowsViewModel = viewModel()

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var dateTime by remember { mutableStateOf(ZonedDateTime.now(ZoneId.of("US/Pacific"))) }
    var timeOfDay by remember { mutableFloatStateOf(dateTime.toLocalTime().toSecondOfDay().toFloat()) }

    val lightingOptionsState = mapViewModel.lightingOptionsState
    val lightingModes = listOf(
        LightingMode.LightAndShadows,
        LightingMode.Light,
        LightingMode.NoLight
    )

    Scaffold(
        topBar = {
            SampleTopAppBar(
                title = sampleName, actions =
                    {
                        val expanded = remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { expanded.value = !expanded.value }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Lighting Settings"
                            )
                            DropdownMenu(
                                expanded = expanded.value,
                                onDismissRequest = { expanded.value = false }
                            ) {
                                lightingModes.forEach {
                                    val text = when (it) {
                                        LightingMode.Light -> "Light"
                                        LightingMode.LightAndShadows -> "Light and shadows"
                                        LightingMode.NoLight -> "No light"
                                    }
                                    // Make the currently selected lighting option bold
                                    val fontWeight = when (it) {
                                        lightingOptionsState.sunLighting.value -> FontWeight.Bold
                                        else -> FontWeight.Normal
                                    }
                                    DropdownMenuItem(
                                        text = { Text(text = text, fontWeight = fontWeight) },
                                        onClick = {
                                            lightingOptionsState.sunLighting.value = it
                                            expanded.value = false
                                        }
                                    )
                                }
                            }
                        }
                    }
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(it)
            ) {
                SceneView(
                    mapViewModel.arcGISScene,
                    modifier = Modifier.weight(1f),
                    // Set the sun time to the selected date, plus the seconds chosen for timeOfDay
                    sunTime = dateTime.withHour(0).withMinute(0).withSecond(0)
                        .plusSeconds(timeOfDay.toLong()).toInstant(),
                    sunLighting = lightingOptionsState.sunLighting.value,
                    ambientLightColor = lightingOptionsState.ambientLightColor.value,
                    atmosphereEffect = lightingOptionsState.atmosphereEffect.value,
                    spaceEffect = lightingOptionsState.spaceEffect.value
                )
                // Create a slider with AM and PM labels to control the sun time
                Row {
                    Text(
                        modifier = Modifier.padding(12.dp),
                        text = "AM",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        modifier = Modifier.weight(1f),
                        value = timeOfDay,
                        onValueChange = { value ->
                            timeOfDay = value
                        },
                        // The range is 0 (start of the day) to 86399 (end of the day in seconds)
                        valueRange = 0f..86399f
                    )
                    Text(
                        modifier = Modifier.padding(12.dp),
                        text = "PM",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                showDatePicker = false
                                datePickerState.selectedDateMillis?.let { millis ->
                                    // Convert the selected date in milliseconds to a LocalDate
                                    val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                                    // Set the dateTime to the start of the day in Pacific Time
                                    dateTime = localDate.atStartOfDay(ZoneId.of("US/Pacific"))
                                }
                            }) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("Cancel")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Date:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = { showDatePicker = true }) {
                        // Display the selected date in a human-readable format
                        Text(
                            text = "Date: ${dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy"))}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    // Display the time in a human-readable format
                    Text(
                        text = "Time: ${
                            dateTime.withHour(0).withMinute(0).withSecond(0)
                                .plusSeconds(timeOfDay.toLong())
                                .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))
                        }",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
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
