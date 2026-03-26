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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
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
                androidx.compose.material3.FloatingActionButton(
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
            )

            // The dialog with the scene environment controls
            if (isDialogOptionsVisible) {
                DialogOptions(
                    isAtmosphere = viewModel.isAtmosphereEnabled,
                    areStars = viewModel.areStarsEnabled,
                    currentBackground = viewModel.backgroundColor,
                    lightingType = viewModel.lightingType,
                    areShadows = viewModel.areDirectShadowsEnabled,
                    timeSeconds = viewModel.timeOfDaySeconds,
                    timeSecondsRange = viewModel.timeOfDaySecondsMin..viewModel.timeOfDaySecondsMax,
                    onAtmosphereChanged = viewModel::updateAtmosphereEnabled,
                    onStarsChanged = viewModel::updateStarsEnabled,
                    onBackgroundColorChanged = viewModel::updateBackgroundColor,
                    onLightingTypeChanged = viewModel::updateLightingType,
                    onShadowsChanged = viewModel::updateDirectShadowsEnabled,
                    onTimeSecondsChanged = viewModel::updateTimeOfDaySeconds,
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

@Composable
fun DialogOptions(
    isAtmosphere: Boolean,
    areStars: Boolean,
    currentBackground: Color,
    lightingType: LightingType,
    areShadows: Boolean,
    timeSeconds: Float,
    timeSecondsRange: ClosedFloatingPointRange<Float>,
    onAtmosphereChanged: (Boolean) -> Unit,
    onStarsChanged: (Boolean) -> Unit,
    onBackgroundColorChanged: (Color) -> Unit,
    onLightingTypeChanged: (LightingType) -> Unit,
    onShadowsChanged: (Boolean) -> Unit,
    onTimeSecondsChanged: (Float) -> Unit,
    onDismissRequest: () -> Unit,
) {
    SampleDialog(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Sky controls
            Text(text = "Sky", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Atmosphere")
                Switch(checked = isAtmosphere, onCheckedChange = onAtmosphereChanged)
            }

            // Stars controls, only meaningful when using Sun lighting
            if (lightingType == LightingType.SUN) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Stars")
                    Switch(checked = areStars, onCheckedChange = { onStarsChanged(it) })
                }
            }

            // Background color selection
            Text(text = "Background", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                // Use a few preset colors. Using SDK Color class values.
                val presetColors = listOf(
                    Color.white,
                    Color.black,
                    Color.fromRgba(135, 206, 235, 255), // sky-like
                    Color.transparent
                )
                presetColors.forEach { sdkColor ->
                    val isSelected = sdkColor == currentBackground
                    FilledTonalButton(
                        modifier = Modifier.size(48.dp),
                        onClick = { onBackgroundColorChanged(sdkColor) },
                        shape = CircleShape,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ComposeColor(
                                sdkColor.red / 255f,
                                sdkColor.green / 255f,
                                sdkColor.blue / 255f,
                                sdkColor.alpha / 255f,
                            ),
                        ),
                    ) {}
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Shadows")
                Switch(checked = areShadows, onCheckedChange = onShadowsChanged)
            }

            // Time slider only when using SunLighting
            if (lightingType == LightingType.SUN) {
                Text(text = "Time of day", style = MaterialTheme.typography.bodyLarge)
                // Show formatted time label
                val formattedTime = formatTimeFromSeconds(timeSeconds)
                Text(text = formattedTime)
                Slider(
                    value = timeSeconds,
                    onValueChange = { onTimeSecondsChanged(it) },
                    valueRange = timeSecondsRange
                )
            }

            // Dismiss button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onDismissRequest) { Text("Done") }
            }
        }
    }
}

private fun formatTimeFromSeconds(seconds: Float): String {
    val localTime = LocalTime.ofSecondOfDay(seconds.toLong())
    val formatter = DateTimeFormatter.ofPattern("h:mm a")
    return localTime.format(formatter)
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun DialogOptionsPreview() {
    SamplePreviewSurface {
        DialogOptions(
            isAtmosphere = true,
            areStars = true,
            currentBackground = Color.transparent,
            lightingType = LightingType.SUN,
            areShadows = true,
            timeSeconds = 43_200f,
            timeSecondsRange = 28_800f..79_200f,
            onAtmosphereChanged = {},
            onStarsChanged = {},
            onBackgroundColorChanged = {},
            onLightingTypeChanged = {},
            onShadowsChanged = {},
            onTimeSecondsChanged = {},
            onDismissRequest = {}
        )
    }
}
