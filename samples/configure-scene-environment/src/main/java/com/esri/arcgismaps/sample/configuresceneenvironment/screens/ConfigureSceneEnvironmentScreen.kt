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
                scene = viewModel.arcGISScene
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

/**
 * Dialog content that exposes scene environment controls and forwards user actions
 * through callback lambdas.
 */
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
    onDismissRequest: () -> Unit
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
            var isColorMenuExpanded by remember { mutableStateOf(false) }
            val selectedColorOption = backgroundColorOptions.firstOrNull { it.color == currentBackground }
                ?: backgroundColorOptions.last()

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { isColorMenuExpanded = true }
                ) {
                    ColorOptionContent(option = selectedColorOption)
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
                                onBackgroundColorChanged(option.color)
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
                Switch(checked = areShadows, onCheckedChange = onShadowsChanged)
            }

            // Time slider, only meaningful when using Sun lighting
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

private fun formatTimeFromSeconds(seconds: Float): String {
    // The slider stores seconds since midnight. Convert to a clock string for display.
    val localTime = LocalTime.ofSecondOfDay(seconds.toLong())
    val formatter = DateTimeFormatter.ofPattern("h:mm a")
    return localTime.format(formatter)
}

/** Maps ArcGIS [Color] values (0..255 channels) to Compose [ComposeColor] (0f..1f channels). */
private fun Color.toComposeColor(): ComposeColor = ComposeColor(
    red = red / 255f,
    green = green / 255f,
    blue = blue / 255f,
    alpha = alpha / 255f,
)

/** Preset option shown in the background color dropdown. */
private data class BackgroundColorOption(
    val label: String,
    val color: Color,
)

private val backgroundColorOptions = listOf(
    BackgroundColorOption("White", Color.white),
    BackgroundColorOption("Black", Color.black),
    BackgroundColorOption("Sky blue", Color.fromRgba(135, 206, 235, 255)),
    BackgroundColorOption("Transparent", Color.transparent),
)

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
            timeSecondsRange = 0f..82_800f,
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
