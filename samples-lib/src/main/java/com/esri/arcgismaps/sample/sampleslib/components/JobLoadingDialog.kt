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

package com.esri.arcgismaps.sample.sampleslib.components

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

/**
 * Composable component to display an determinate loading dialog along for an ArcGIS Job
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobLoadingDialog(
    title: String,
    progress: Int,
    isPauseJobEnabled: Boolean = false,
    cancelJobRequest: (Unit) -> Unit = {},
    pauseJobRequest: (Unit) -> Unit = {},
    resumeJobRequest: (Unit) -> Unit = {},
) {
    BasicAlertDialog(
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        onDismissRequest = { /* No dismiss allowed, instead use 'Cancel job' button */ }
    ) {
        Surface(
            tonalElevation = 12.dp,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // loading message text
                Text(
                    modifier = Modifier.padding(24.dp),
                    text = title,
                    textAlign = TextAlign.Center
                )
                // row of progress indicator and percentage text.
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // set ease animation when progress state changes
                    val progressAnimation by animateFloatAsState(
                        targetValue = progress.toFloat() / 100f,
                        animationSpec = tween(
                            durationMillis = 500,
                            easing = FastOutSlowInEasing
                        ), label = "FloatAnimation"
                    )
                    // create the linear progress indicator
                    LinearProgressIndicator(
                        progress = { progressAnimation },
                    )
                    // progress percentage text
                    Text(
                        text = "$progress%",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // display pause/resume job based on the boolean state
                var isJobPaused by remember { mutableStateOf(false) }
                Row(modifier = Modifier.align(Alignment.End)) {
                    // display pause button if enabled
                    if (isPauseJobEnabled) {
                        // pause job button
                        Button(colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.primary
                        ), onClick = {
                            if (!isJobPaused)
                                pauseJobRequest(Unit)
                            else
                                resumeJobRequest(Unit)

                            isJobPaused = !isJobPaused
                        }) {
                            if (!isJobPaused) {
                                Text(text = "Pause Job")
                            } else {
                                Text(text = "Resume Job")
                            }
                        }
                    }
                    // cancel job button
                    Button(colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                        onClick = { cancelJobRequest(Unit) }) {
                        Text(text = "Cancel Job")
                    }
                }
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewJobLoadingDialog() {
    SampleAppTheme {
        JobLoadingDialog(
            title = "Job dialog loading message here",
            isPauseJobEnabled = true,
            progress = 75
        )
    }
}