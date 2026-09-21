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

package com.esri.arcgismaps.sample.orbitcameraaroundobject.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.orbitcameraaroundobject.components.OrbitCameraUiState
import com.esri.arcgismaps.sample.orbitcameraaroundobject.components.ViewMode

/**
 * Supporting pane content for the sample.
 */
@Composable
internal fun OrbitCameraAroundObjectSupportingPane(
    orbitCameraUiState: OrbitCameraUiState,
    setCameraHeading: (Float) -> Unit,
    setCameraPitch: (Float) -> Unit,
    cameraMode: (ViewMode) -> Unit,
    setInteraction: (Boolean) -> Unit
) {
    ViewMode.entries.forEach { mode ->
        SelectionRow(
            title = mode.name,
            description = when (mode) {
                ViewMode.Center -> {
                    "Default centered view of the 3D model."
                }

                ViewMode.Cockpit -> {
                    "Cockpit view from the 3D model."
                }
            },
            selected = orbitCameraUiState.viewMode == mode,
            onClick = { cameraMode(mode) }
        )
    }

    ToggleRow(
        title = "Allow Camera distance interaction",
        description = "Enabled when in center mode, disabled when in cockpit mode.",
        isToggleChecked = orbitCameraUiState.allowCameraDistanceInteraction,
        isToggleEnabled = orbitCameraUiState.viewMode == ViewMode.Center,
        onCheckedChange = setInteraction
    )
    HeadingSlider(orbitCameraUiState.heading, setCameraHeading)
    PitchSlider(orbitCameraUiState.pitch, setCameraPitch)
}

@Composable
fun CameraSlider(
    title: String,
    sliderValue: Float,
    sliderRangeValue: ClosedFloatingPointRange<Float>,
    onSliderValueChanged: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = sliderValue.toInt().toString(),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = sliderValue,
            onValueChange = onSliderValueChanged,
            valueRange = sliderRangeValue
        )
    }
}

@Composable
private fun HeadingSlider(heading: Float, onHeadingChanged: (Float) -> Unit) {
    CameraSlider(
        title = "Camera Heading",
        sliderValue = heading,
        sliderRangeValue = -180f..180f,
        onSliderValueChanged = onHeadingChanged
    )
}

@Composable
private fun PitchSlider(pitch: Float, onPitchChanged: (Float) -> Unit) {
    CameraSlider(
        title = "Plane Pitch",
        sliderValue = pitch,
        sliderRangeValue = -90f..90f,
        onSliderValueChanged = onPitchChanged
    )
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    isToggleChecked: Boolean,
    isToggleEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            enabled = isToggleEnabled,
            checked = isToggleChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SelectionRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
