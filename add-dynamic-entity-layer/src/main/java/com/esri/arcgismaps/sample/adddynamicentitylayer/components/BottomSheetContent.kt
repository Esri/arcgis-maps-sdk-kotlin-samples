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

package com.esri.arcgismaps.sample.adddynamicentitylayer.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.sampleslib.theme.SampleTypography
/**
 * Composable component to display Dynamic Entity Layer Settings
 */
@Composable
fun DynamicEntityLayerProperties(
    onTrackLineVisibilityChanged: (Boolean) -> Unit = { },
    onPrevObservationsVisibilityChanged: (Boolean) -> Unit = { },
    onObservationsChanged: (Float) -> Unit = { },
    onPurgeAllObservations: () -> Unit = { },
    isTrackLineVisible: Boolean,
    isPrevObservationsVisible: Boolean,
    observationsPerTrack: Float,
    onDismiss: () -> Unit = { }
) {
    var sliderValue by remember { mutableStateOf(observationsPerTrack) }

    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(5f),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                text = "Dynamic Entity Settings",
            )
            TextButton(
                modifier = Modifier.weight(1f),
                onClick = onDismiss
            ) {
                Text(text = "Done")
            }
        }

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
                        text = "Track Lines",
                        style = SampleTypography.bodyLarge
                    )
                    Switch(
                        checked = isTrackLineVisible,
                        onCheckedChange = {
                            onTrackLineVisibilityChanged(it)
                        }
                    )
                }
                Divider(thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "Previous Observations",
                        style = SampleTypography.bodyLarge
                    )
                    Switch(
                        checked = isPrevObservationsVisible,
                        onCheckedChange = {
                            onPrevObservationsVisibilityChanged(it)
                        }
                    )
                }
            }
        }

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
                            text = "Observations per track",
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = sliderValue.toInt().toString()
                        )
                    }
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            sliderValue = it
                            onObservationsChanged(sliderValue)
                        },
                        valueRange = 1f..16f
                    )
                }
                Divider(thickness = 0.5.dp)
                TextButton(
                    modifier = Modifier.align(CenterHorizontally),
                    onClick = onPurgeAllObservations
                )
                {
                    Text(text = "Purge All Observations")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun DynamicEntityLayerPropertiesPreview() {
    SampleAppTheme {
        DynamicEntityLayerProperties(
            isTrackLineVisible = true,
            isPrevObservationsVisible = true,
            observationsPerTrack = 5f
        )
    }
}
