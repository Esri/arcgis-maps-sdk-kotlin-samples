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

package com.esri.arcgismaps.sample.snaptofeatures.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.geometryeditor.SnapSourceSettings
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.sampleslib.theme.SampleTypography

/**
 * Composable component to display the snapping configuration settings.
 */
@Composable
fun SnapSettings(
    onSnappingChanged: (Boolean) -> Unit = { },
    isSnappingEnabled: Boolean,
    snapSourceList: List<SnapSourceSettings>,
    isSnapSourceEnabled: MutableList<Boolean>,
    onSnapSourceChanged: (Boolean, Int) -> Unit = { _: Boolean, _: Int -> },
    onDismiss: () -> Unit = { }
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
                text = "Snap Settings",
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = "Done")
            }
        }

        // configure snapping settings
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Snapping",
                        style = SampleTypography.bodyLarge
                    )
                }
                Divider(color = MaterialTheme.colorScheme.primary, thickness = 0.5.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "\t\tEnabled",
                        style = SampleTypography.bodyLarge,
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

        // configure snap source collection settings
        Surface(
            modifier = Modifier.padding(20.dp),
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Column {
                    Row {
                        Text(
                            modifier = Modifier.weight(12f),
                            text = "Snap Sources",
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.primary, thickness = 0.5.dp)
                    // add a row for each snap source in the collection
                    for (x in snapSourceList.indices) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = "\t\t${(snapSourceList[x].source as FeatureLayer).name}"
                            )
                            Switch(
                                checked = isSnapSourceEnabled[x],
                                onCheckedChange = { newValue ->
                                    onSnapSourceChanged(newValue, x)
                                }
                            )
                        }
                        Divider(color = MaterialTheme.colorScheme.primary, thickness = 0.5.dp)
                    }
                }
                Divider(thickness = 0.5.dp)
                TextButton(
                    modifier = Modifier.align(CenterHorizontally),
                    onClick = {
                        for (x in snapSourceList.indices) {
                            onSnapSourceChanged(true, x)
                        }
                    }
                )
                {
                    Text(text = "Enable All Sources")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun SnapSettingsPreview() {
    SampleAppTheme {
        SnapSettings(
            isSnappingEnabled = false,
            snapSourceList = listOf(),
            isSnapSourceEnabled = mutableListOf(),
        )
    }
}
