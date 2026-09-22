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

package com.esri.arcgismaps.sample.updatelabelsandsymbolstoscaleforvisualaccessibility.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.updatelabelsandsymbolstoscaleforvisualaccessibility.components.AdaptiveUiState

@Composable
internal fun UpdateLabelsAndSymbolsToScaleForVisualAccessibilitySupportingPane(
    adaptiveUiState: AdaptiveUiState,
    onCheckedChange: (Boolean) -> Unit,
    onOpenTextSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Scaling source", style = MaterialTheme.typography.titleMedium)
        Text("Aa  Labels: GeoView API", style = MaterialTheme.typography.bodyMedium)
        Text("●  Symbols: OS text size", style = MaterialTheme.typography.bodyMedium)

        HorizontalDivider()

        Text("Toggles", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = adaptiveUiState.isSystemTextScaleEnabled,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = adaptiveUiState.isSystemTextScaleEnabled,
                onCheckedChange = null
            )
            Text("Apply OS text size to labels")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenTextSettings
        ) {
            Text("Open OS text-size settings")
        }

        HorizontalDivider()

        Text("Current controls", style = MaterialTheme.typography.titleMedium)
        Text("OS text size: ${adaptiveUiState.fontScale * 100}%")
        Text(
            "Labels: ${if (adaptiveUiState.isSystemTextScaleEnabled) "scaled by GeoView.useSystemTextScale" else "fixed size"}"
        )
        Text(
            "Symbols: 10 DIPs × ${adaptiveUiState.fontScale} = " +
                "${adaptiveUiState.symbolSize} DIPs"
        )

        HorizontalDivider()
    }
}
