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

package com.esri.arcgismaps.sample.configuresceneenvironment.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.Color
import com.arcgismaps.toolkit.geoviewcompose.LocalSceneView
import com.esri.arcgismaps.sample.configuresceneenvironment.components.ConfigureSceneEnvironmentViewModel
import com.esri.arcgismaps.sample.configuresceneenvironment.components.LightingType
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDialog
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Main screen layout for the sample app that configures scene environment.
 */
@Composable
fun ConfigureSceneEnvironmentScreen(sampleName: String) {
    val viewModel: ConfigureSceneEnvironmentViewModel = viewModel()

    var isDialogOptionsVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            if (!isDialogOptionsVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { isDialogOptionsVisible = true }
                ) { Icon(Icons.Filled.Settings, contentDescription = "Show options") }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            // Display the SceneView with the selected environment options
            LocalSceneView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                scene = viewModel.arcGISScene,
                onCriticalErrorChanged = viewModel.messageDialogVM::showMessageDialog
            )

            // The dialog with the scene environment controls
            if (isDialogOptionsVisible) {
                DialogOptions(
                    isAtmosphereEnabled = viewModel.isAtmosphereEnabled,
                    areStarsEnabled = viewModel.areStarsEnabled,
                    selectedBackgroundColor = viewModel.backgroundColor,
                    lightingType = viewModel.lightingType,
                    areShadowsEnabled = viewModel.areDirectShadowsEnabled,
                    timeOfDaySeconds = viewModel.timeOfDaySeconds,
                    timeOfDaySecondsRange = viewModel.timeOfDaySecondsMin..viewModel.timeOfDaySecondsMax,
                    onAtmosphereEnabledChanged = viewModel::updateAtmosphereEnabled,
                    onStarsEnabledChanged = viewModel::updateStarsEnabled,
                    onBackgroundColorSelected = viewModel::updateBackgroundColor,
                    onLightingTypeChanged = viewModel::updateLightingType,
                    onShadowsEnabledChanged = viewModel::updateDirectShadowsEnabled,
                    onTimeOfDayChanged = viewModel::updateTimeOfDaySeconds,
                    onDismissRequest = { isDialogOptionsVisible = false }
                )
            }

            // Display a dialog if the sample encounters an error
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
}

/**
 * Dialog content that exposes scene environment controls and forwards user actions
 * through callback lambdas.
 */
@Composable
fun DialogOptions(
    isAtmosphereEnabled: Boolean,
    areStarsEnabled: Boolean,
    selectedBackgroundColor: Color,
    lightingType: LightingType,
    areShadowsEnabled: Boolean,
    timeOfDaySeconds: Float,
    timeOfDaySecondsRange: ClosedFloatingPointRange<Float>,
    onAtmosphereEnabledChanged: (Boolean) -> Unit,
    onStarsEnabledChanged: (Boolean) -> Unit,
    onBackgroundColorSelected: (Color) -> Unit,
    onLightingTypeChanged: (LightingType) -> Unit,
    onShadowsEnabledChanged: (Boolean) -> Unit,
    onTimeOfDayChanged: (Float) -> Unit,
    onDismissRequest: () -> Unit
) {
    SampleDialog(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Sky controls
            Text(text = "Sky", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Atmosphere")
                Switch(checked = isAtmosphereEnabled, onCheckedChange = onAtmosphereEnabledChanged)
            }

            // Stars controls, only meaningful when using Sun lighting
            if (lightingType == LightingType.SUN) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Stars")
                    Switch(checked = areStarsEnabled, onCheckedChange = { onStarsEnabledChanged(it) })
                }
            }

            // Background color selection
            Text(text = "Background", style = MaterialTheme.typography.titleMedium)
            var isColorMenuExpanded by remember { mutableStateOf(false) }
            val selectedColorOption = backgroundColorOptions.firstOrNull { it.color == selectedBackgroundColor }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { isColorMenuExpanded = true }
                ) {
                    if (selectedColorOption != null) {
                        ColorOptionContent(option = selectedColorOption)
                    } else {
                        Text("Choose background color")
                    }
                }
                DropdownMenu(
                    expanded = isColorMenuExpanded,
                    onDismissRequest = { isColorMenuExpanded = false }
                ) {
                    backgroundColorOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                ColorOptionContent(option = option)
                            },
                            onClick = {
                                onBackgroundColorSelected(option.color)
                                isColorMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Lighting section
            Text(text = "Lighting", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = LightingType.entries
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = { onLightingTypeChanged(option) },
                        selected = (option == lightingType)
                    ) {
                        Text(option.displayName)
                    }
                }
            }

            // Shadows toggle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Shadows")
                Switch(checked = areShadowsEnabled, onCheckedChange = onShadowsEnabledChanged)
            }

            // Time slider, only meaningful when using Sun lighting
            if (lightingType == LightingType.SUN) {
                Text(text = "Time of day", style = MaterialTheme.typography.bodyLarge)
                // Show formatted time label
                val formattedTime = formatTimeFromSeconds(timeOfDaySeconds)
                Text(text = formattedTime)
                Slider(
                    value = timeOfDaySeconds,
                    onValueChange = { onTimeOfDayChanged(it) },
                    valueRange = timeOfDaySecondsRange
                )
            }

            // Done button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onDismissRequest) { Text("Done") }
            }
        }
    }
}

/** Renders a small color box with a matching label for the color dropdown UI. */
@Composable
private fun ColorOptionContent(option: BackgroundColorOption) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp)
                )
                .background(
                    color = option.color.toComposeColor(),
                    shape = RoundedCornerShape(4.dp)
                )
        )
        Text(text = option.label)
    }
}

private val timeOfDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private fun formatTimeFromSeconds(seconds: Float): String {
    // The slider stores seconds since midnight. Convert to a clock string for display.
    val localTime = LocalTime.ofSecondOfDay(seconds.toLong())
    return localTime.format(timeOfDayFormatter)
}

/** Maps ArcGIS [Color] values (0..255 channels) to Compose [ComposeColor] (0f..1f channels). */
private fun Color.toComposeColor(): ComposeColor = ComposeColor(
    red = red / 255f,
    green = green / 255f,
    blue = blue / 255f,
    alpha = alpha / 255f
)

/** Preset option shown in the background color dropdown. */
private data class BackgroundColorOption(
    val label: String,
    val color: Color
)

private val backgroundColorOptions = listOf(
    BackgroundColorOption("White", Color.white),
    BackgroundColorOption("Black", Color.black),
    BackgroundColorOption("Sky blue", Color.fromRgba(135, 206, 235, 255)),
    BackgroundColorOption("Transparent", Color.transparent)
)

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun DialogOptionsPreview() {
    SamplePreviewSurface {
        DialogOptions(
            isAtmosphereEnabled = true,
            areStarsEnabled = true,
            selectedBackgroundColor = Color.transparent,
            lightingType = LightingType.SUN,
            areShadowsEnabled = true,
            timeOfDaySeconds = 43_200f,
            timeOfDaySecondsRange = 0f..82_800f,
            onAtmosphereEnabledChanged = {},
            onStarsEnabledChanged = {},
            onBackgroundColorSelected = {},
            onLightingTypeChanged = {},
            onShadowsEnabledChanged = {},
            onTimeOfDayChanged = {},
            onDismissRequest = {}
        )
    }
}
