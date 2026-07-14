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

package com.esri.arcgismaps.sample.analyzeterrainsuitabilityfromslopeandaspect.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.analyzeterrainsuitabilityfromslopeandaspect.components.SlopeAspectUiState
import com.esri.arcgismaps.sample.analyzeterrainsuitabilityfromslopeandaspect.components.ScenarioOption

/**
 * Supporting pane content for the sample.
 */
@Composable
internal fun AnalyzeTerrainSuitabilityFromSlopeAndAspectSupportingPane(
    slopeAspectUiState: SlopeAspectUiState,
    onSelectionChange: (ScenarioOption) -> Unit
) {
    Text(
        text = "Sheltered vs Exposed Terrain Suitability",
        style = MaterialTheme.typography.titleMedium
    )
    ScenarioOption.entries.forEach { mode ->
        SelectionRow(
            title = mode.name,
            description = when (mode) {
                ScenarioOption.Sheltered -> {
                    "Gentle, lowland south-facing slopes"
                }

                ScenarioOption.Exposed -> {
                    "Steep, upland west- through north-facing slopes"
                }
            },
            selected = slopeAspectUiState.scenarioOption == mode,
            onClick = { onSelectionChange(mode) }
        )
    }
    RasterDataCopyrightText()
}

/**
 * Reusable composable for a row with a title, description, and a radio button to indicate selection.
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
                .padding(6.dp),
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

/**
 * Display copyright text for the raster data we are using.
 */
@Composable
fun RasterDataCopyrightText() {
    Text(
        text = "Raster data copyright Scottish Government and SEPA (2014)",
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
