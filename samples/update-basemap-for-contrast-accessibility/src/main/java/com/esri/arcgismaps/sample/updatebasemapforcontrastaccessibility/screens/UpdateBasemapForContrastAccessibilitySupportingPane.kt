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

package com.esri.arcgismaps.sample.updatebasemapforcontrastaccessibility.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.updatebasemapforcontrastaccessibility.components.ContrastAppearance
import com.esri.arcgismaps.sample.updatebasemapforcontrastaccessibility.components.ContrastMode
import com.esri.arcgismaps.sample.updatebasemapforcontrastaccessibility.components.ContrastUiState

/**
 * Shows the sample controls and appearance that drives the displayed MapView.
 */
@Composable
internal fun UpdateBasemapForContrastAccessibilitySupportingPane(
    contrastUiState: ContrastUiState,
    onContrastModeChanged: (ContrastMode) -> Unit,
    onManualContrastChanged: (ContrastAppearance) -> Unit,
    onReferenceLayerVisibilityChanged: (Boolean) -> Unit
) {
    ReferenceLayerToggleRow(
        referenceLayersVisible = contrastUiState.isReferenceLayersEnabled,
        onReferenceLayerVisibilityChanged = onReferenceLayerVisibilityChanged
    )

    HorizontalDivider()

    SelectionSection(title = "Select visual contrast mode") {
        ContrastMode.entries.forEach { mode ->
            SelectionRow(
                title = mode.displayName,
                description = if (mode == ContrastMode.Automatic) {
                    "Use device light, dark, and high-contrast settings to auto-select basemap."
                } else {
                    "Choose one of the four basemaps manually."
                },
                selected = contrastUiState.contrastMode == mode,
                onClick = { onContrastModeChanged(mode) }
            )
        }
    }

    AnimatedVisibility(visible = contrastUiState.contrastMode == ContrastMode.Manual) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HorizontalDivider()
            SelectionSection(title = "Manual contrast") {
                ContrastAppearance.entries.forEach { appearance ->
                    SelectionRow(
                        title = appearance.displayName,
                        description = appearance.description,
                        selected = contrastUiState.contrastAppearance == appearance,
                        onClick = { onManualContrastChanged(appearance) }
                    )
                }
            }
        }
    }
}

/**
 * Shows the reference-layer visibility to toggle reference layers.
 */
@Composable
private fun ReferenceLayerToggleRow(
    referenceLayersVisible: Boolean,
    onReferenceLayerVisibilityChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Reference layers",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (referenceLayersVisible) "Labels and boundary reference layers are visible."
                else "Labels and boundary reference layers are hidden.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = referenceLayersVisible,
            onCheckedChange = onReferenceLayerVisibilityChanged
        )
    }
}

@Composable
private fun SelectionRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
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

@Composable
private fun SelectionSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium
    )
    content()
}

private val ContrastMode.displayName: String
    get() = when (this) {
        ContrastMode.Automatic -> "Automatic"
        ContrastMode.Manual -> "Manual"
    }

private val ContrastAppearance.displayName: String
    get() = when (this) {
        ContrastAppearance.Light -> "Light"
        ContrastAppearance.Dark -> "Dark"
        ContrastAppearance.HighContrastLight -> "High contrast light"
        ContrastAppearance.HighContrastDark -> "High contrast dark"
    }

private val ContrastAppearance.description: String
    get() = when (this) {
        ContrastAppearance.Light -> "Regular light basemap for regular light theme."
        ContrastAppearance.Dark -> "Regular dark basemap for regular dark theme."
        ContrastAppearance.HighContrastLight -> "High-contrast light basemap for enhanced light theme."
        ContrastAppearance.HighContrastDark -> "High-contrast dark basemap for enhanced dark theme."
    }
