/* Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.displaycomposablemapview.screens

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
import com.esri.arcgismaps.sample.displaycomposablemapview.components.AdaptiveUiState
import com.esri.arcgismaps.sample.displaycomposablemapview.components.BasemapOptions

/**
 * Supporting pane content for the sample.
 */
@Composable
internal fun DisplayComposableMapViewSupportingPane(
    adaptiveUiState: AdaptiveUiState,
    onSelectionChange: (BasemapOptions) -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    // TODO:
    //  This content is in a scrollable ColumnScope, so Composables can be added without the Column wrapper.
    //  Update below composables to reflect sample design UI state and invoke lambda callbacks for screen.

    ToggleRow(
        title = "Show reference layers",
        description =
            if (adaptiveUiState.isLayersEnabled) "Reference layers are visible on the map."
            else "Reference layers are hidden on the map.",
        isToggleChecked = adaptiveUiState.isLayersEnabled,
        onCheckedChange = onCheckedChange
    )

    HorizontalDivider()

    Text(
        text = "Select basemap style",
        style = MaterialTheme.typography.titleMedium
    )

    BasemapOptions.entries.forEach { mode ->
        SelectionRow(
            title = mode.name,
            description = when (mode) {
                BasemapOptions.Light -> {
                    "A vector basemap with light background style."
                }

                BasemapOptions.Dark -> {
                    "A vector basemap with dark background style."
                }
            },
            selected = adaptiveUiState.basemapOptions == mode,
            onClick = { onSelectionChange(mode) }
        )
    }
}

/**
 * TODO: Reusable composable for a row with a title, description, and a toggle switch.
 */
@Composable
private fun ToggleRow(
    title: String,
    description: String,
    isToggleChecked: Boolean,
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
            checked = isToggleChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * TODO: Reusable composable for a row with a title, description, and a radio button to indicate selection.
 */
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
