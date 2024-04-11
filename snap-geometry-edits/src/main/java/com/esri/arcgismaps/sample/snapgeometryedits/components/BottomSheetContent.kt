/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.snapgeometryedits.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.geometryeditor.SnapSourceSettings
import com.esri.arcgismaps.sample.sampleslib.theme.SampleTypography

/**
 * Composable component to display the snapping configuration settings.
 */
@Composable
fun SnapSettings(
    snapSourceList: State<List<SnapSourceSettings>>,
    onSnappingChanged: (Boolean) -> Unit = { },
    onSnapSourceChanged: (Boolean, Int) -> Unit = { _: Boolean, _: Int -> },
    isSnappingEnabled: Boolean,
    isSnapSourceEnabled: List<Boolean>,
    onDismiss: () -> Unit = { }
) {
    Surface(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Column(Modifier.background(MaterialTheme.colorScheme.background)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 20.dp, 20.dp, 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    style = SampleTypography.titleMedium,
                    text = "Snapping",
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onDismiss)
                { Text(text = "Done") }
            }
            if (snapSourceList.value.isEmpty()) {
                Surface(
                    modifier = Modifier.padding(20.dp),
                    tonalElevation = 1.dp,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                style = SampleTypography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(12f),
                                text = "No valid snap sources."
                            )
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 0.dp),
                    tonalElevation = 1.dp,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                modifier = Modifier.padding(20.dp, 0.dp, 0.dp, 0.dp),
                                style = SampleTypography.bodyLarge,
                                text = "Enabled",
                            )
                            Switch(
                                checked = isSnappingEnabled,
                                onCheckedChange = {
                                    onSnappingChanged(it)
                                }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 10.dp, 20.dp, 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        style = SampleTypography.titleMedium,
                        text = "Point Layers",
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = {
                            snapSourceList.value.forEachIndexed { index, snapSource ->
                                if ((snapSource.source as FeatureLayer).featureTable?.geometryType == GeometryType.Point) {
                                    onSnapSourceChanged(true, index)
                                }
                            }
                        }
                    ) {
                        Text(text = "Enable All Sources")
                    }
                }
                Surface(
                    modifier = Modifier.padding(20.dp, 0.dp, 20.dp, 10.dp),
                    tonalElevation = 1.dp,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Column {
                            snapSourceList.value.forEachIndexed { index, snapSource ->
                                if ((snapSource.source as FeatureLayer).featureTable?.geometryType == GeometryType.Point) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(20.dp, 0.dp, 0.dp, 0.dp),
                                            text = (snapSource.source as FeatureLayer).name
                                        )
                                        Switch(
                                            checked = isSnapSourceEnabled[index],
                                            onCheckedChange = { newValue ->
                                                onSnapSourceChanged(newValue, index)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 0.dp, 20.dp, 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        style = SampleTypography.titleMedium,
                        text = "Polyline Layers",
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = {
                            snapSourceList.value.forEachIndexed { index, snapSource ->
                                if ((snapSource.source as FeatureLayer).featureTable?.geometryType == GeometryType.Polyline) {
                                    onSnapSourceChanged(true, index)
                                }
                            }
                        }
                    ) {
                        Text(text = "Enable All Sources")
                    }
                }
                Surface(
                    modifier = Modifier.padding(20.dp, 0.dp, 20.dp, 10.dp),
                    tonalElevation = 1.dp,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Column {
                            snapSourceList.value.forEachIndexed { index, snapSource ->
                                if ((snapSource.source as FeatureLayer).featureTable?.geometryType == GeometryType.Polyline) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(20.dp, 0.dp, 0.dp, 0.dp),
                                            text = (snapSource.source as FeatureLayer).name
                                        )
                                        Switch(
                                            checked = isSnapSourceEnabled[index],
                                            onCheckedChange = { newValue ->
                                                onSnapSourceChanged(newValue, index)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
