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

package com.esri.arcgismaps.sample.snapgeometryeditswithutilitynetworkrules.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.snapgeometryeditswithutilitynetworkrules.components.SnapGeometryEditsWithUtilityNetworkRulesViewModel

@Composable
fun SnapSourcesPanel(
    snapSourcePropertyList: List<SnapGeometryEditsWithUtilityNetworkRulesViewModel.SnapSourceProperty>,
    onSnapSourcePropertyChanged: (Boolean, Int) -> Unit,
    isGeometryEditorStarted: Boolean,
    canGeometryEditorUndo: Boolean,
    onDiscardGeometryChanges: () -> Unit,
    onSaveGeometryChanges: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                enabled = isGeometryEditorStarted,
                onClick = onDiscardGeometryChanges
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Discard")
            }
            Text(
                text = "Snap source settings",
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(
                enabled = (isGeometryEditorStarted && canGeometryEditorUndo),
                onClick = onSaveGeometryChanges
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
            }
        }

        snapSourcePropertyList.forEachIndexed { index, snapSourceProperty ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    bitmap = snapSourceProperty.swatch.bitmap.asImageBitmap(),
                    contentDescription = "Symbol image"
                )
                Switch(
                    checked = snapSourcePropertyList[index].snapSourceSettings.isEnabled,
                    onCheckedChange = { newValue ->
                        onSnapSourcePropertyChanged(newValue, index)
                    }
                )
                Text(
                    style = MaterialTheme.typography.bodySmall,
                    text = "${snapSourceProperty.name} (${snapSourceProperty.snapSourceSettings.ruleBehavior})",
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun SnapSourcesPanelPreview() {
    SampleAppTheme {
        Surface {
            SnapSourcesPanel(
                snapSourcePropertyList = listOf(),
                onSnapSourcePropertyChanged = { _, _ -> },
                isGeometryEditorStarted = true,
                canGeometryEditorUndo = true,
                onDiscardGeometryChanges = {},
                onSaveGeometryChanges = {}
            )
        }
    }
}
