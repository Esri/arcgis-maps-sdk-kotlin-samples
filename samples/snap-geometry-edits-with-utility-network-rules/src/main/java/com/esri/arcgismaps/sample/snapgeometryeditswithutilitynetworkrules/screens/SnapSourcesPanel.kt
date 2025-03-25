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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.sampleslib.theme.SampleTypography
import com.esri.arcgismaps.sample.snapgeometryeditswithutilitynetworkrules.components.SnapGeometryEditsWithUtilityNetworkRulesViewModel

@Composable
fun SnapSourcesPanel(
    snapSourcePropertyList: State<List<SnapGeometryEditsWithUtilityNetworkRulesViewModel.SnapSourceProperty>>,
    onSnapSourcePropertyChanged: (Boolean, Int) -> Unit = { _: Boolean, _: Int -> }
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .defaultMinSize(minHeight = 185.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row {
                Text(
                    text = "Snap sources and their SnapRuleBehavior",
                    fontWeight = FontWeight.ExtraBold
                )
            }
            if (snapSourcePropertyList.value.isEmpty()) {
                Text(text = "Tap a point feature to edit")
            }
            for (index in snapSourcePropertyList.value.indices) {
                val snapSourceProperty = snapSourcePropertyList.value[index]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = snapSourceProperty.swatch.bitmap.asImageBitmap(),
                        contentDescription = "Symbol image"
                    )
                    Switch(
                        modifier = Modifier.padding(start = 8.dp),
                        checked = snapSourcePropertyList.value[index].snapSourceSettings.isEnabled,
                        onCheckedChange = { newValue ->
                            onSnapSourcePropertyChanged(newValue, index)
                        }
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        style = SampleTypography.bodyMedium,
                        text = "${snapSourceProperty.name} (${snapSourceProperty.snapSourceSettings.ruleBehavior})"
                    )
                }
            }
        }
    }
}
