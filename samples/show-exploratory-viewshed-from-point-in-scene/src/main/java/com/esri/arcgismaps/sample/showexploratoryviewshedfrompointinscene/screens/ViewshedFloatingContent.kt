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

package com.esri.arcgismaps.sample.showexploratoryviewshedfrompointinscene.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.showexploratoryviewshedfrompointinscene.components.ViewshedUiState


@Composable
fun ViewshedFloatingContent(
    viewshedUiState: ViewshedUiState,
    onFrustumVisibilityChanged: (Boolean) -> Unit = {},
    onAnalysisVisibilityChanged: (Boolean) -> Unit = {},
    onSetViewpointToAnalysisExtent: () -> Unit = {},
    onResetViewshedOptions: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FrustumCheckbox(viewshedUiState.isFrustumVisible, onFrustumVisibilityChanged)
        AnalysisCheckbox(viewshedUiState.isAnalysisVisible, onAnalysisVisibilityChanged)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedButton(onClick = onSetViewpointToAnalysisExtent) {
                Text("Align camera with viewshed")
            }
            OutlinedButton(onClick = onResetViewshedOptions) {
                Text("Reset viewshed options")
            }
        }
    }
}


@Composable
private fun FrustumCheckbox(
    isChecked: Boolean,
    onFrustumVisibilityChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = isChecked,
                role = Role.Checkbox,
                onValueChange = onFrustumVisibilityChanged,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = null,
        )
        Text(text = "Frustum Outline")
    }
}

@Composable
private fun AnalysisCheckbox(
    isChecked: Boolean,
    onAnalysisVisibilityChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = isChecked,
                role = Role.Checkbox,
                onValueChange = onAnalysisVisibilityChanged,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = null,
        )
        Text(text = "Analysis Overlay")
    }
}
