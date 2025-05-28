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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
            SampleTopAppBar(title = sampleName)
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
                LightingOptionsPanel(
                    lightingModes = lightingModes,
                    selectedLightingMode = lightingOptionsState.sunLighting.value,
                    onLightingModeSelected = { lightingOptionsState.sunLighting.value = it },
                    timeOfDay = timeOfDay,
                    onTimeOfDayChanged = { timeOfDay = it },
                    showDatePicker = showDatePicker,
                    onShowDatePickerChanged = { showDatePicker = it },
                    dateTime = dateTime,
                    onDateTimeChanged = { dateTime = it },
                    datePickerState = datePickerState
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
fun LightingOptionsPanel(
    lightingModes: List<LightingMode>,
    selectedLightingMode: LightingMode,
    onLightingModeSelected: (LightingMode) -> Unit,
    timeOfDay: Float,
    onTimeOfDayChanged: (Float) -> Unit,
    showDatePicker: Boolean,
    onShowDatePickerChanged: (Boolean) -> Unit,
    dateTime: ZonedDateTime,
    onDateTimeChanged: (ZonedDateTime) -> Unit,
    datePickerState: DatePickerState
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Lighting mode segmented button row
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            lightingModes.forEachIndexed { index, mode ->
                val text = when (mode) {
                    LightingMode.Light -> "Light"
                    LightingMode.LightAndShadows -> "Light & shadows"
                    LightingMode.NoLight -> "No light"
                }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index, lightingModes.size),
                    selected = (mode == selectedLightingMode),
                    onClick = { onLightingModeSelected(mode) },
                ) {
                    Text(
                        text = text,
                        fontWeight = if (mode == selectedLightingMode) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        // Create a slider with AM and PM labels to control the sun time
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                text = "AM",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                modifier = Modifier.weight(1f),
                value = timeOfDay,
                onValueChange = { onTimeOfDayChanged(it) },
                // The range is 0 (start of the day) to 86399 (end of the day in seconds)
                valueRange = 0f..86399f
            )
            Text(
                modifier = Modifier.padding(start = 8.dp, end = 12.dp),
                text = "PM",
                style = MaterialTheme.typography.titleMedium
            )
        }
        // Date picker and time display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Date:",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = { onShowDatePickerChanged(true) }) {
                Text(
                    text = dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "Time: " + dateTime.withHour(0).withMinute(0).withSecond(0)
                    .plusSeconds(timeOfDay.toLong())
                    .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { onShowDatePickerChanged(false) },
                confirmButton = {
                    TextButton(onClick = {
                        onShowDatePickerChanged(false)
                        datePickerState.selectedDateMillis?.let { millis ->
                            // Convert the selected date in milliseconds to a LocalDate
                            val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                            // Set the dateTime to the start of the day in Pacific Time
                            onDateTimeChanged(localDate.atStartOfDay(ZoneId.of("US/Pacific")))
                        }
                    }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onShowDatePickerChanged(false) }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
