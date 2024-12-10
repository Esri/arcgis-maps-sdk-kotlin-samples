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

package com.esri.arcgismaps.sample.playkmltour.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.kml.KmlTourStatus
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.playkmltour.R
import com.esri.arcgismaps.sample.playkmltour.components.PlayKMLTourViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Main screen layout for the sample
 */
@Composable
fun PlayKMLTourScreen(sampleName: String) {
    val viewModel: PlayKMLTourViewModel = viewModel()

    val state = remember { viewModel.kmlTourStatusFlow }
    val stateFlow by state.collectAsStateWithLifecycle(KmlTourStatus.NotInitialized)

    val progress = remember { viewModel.kmlTourProgressFlow }
    val progressFlow by progress.collectAsStateWithLifecycle(0.0f)

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                SceneView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    isAttributionBarVisible = false,
                    arcGISScene = viewModel.arcGISScene,
                    sceneViewProxy = viewModel.sceneViewProxy
                )

                val padding = 8.dp

                LinearProgressIndicator(progress = { progressFlow },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = padding))
                Text("${stringResource(R.string.tour_status)} ${tourStateToString(stateFlow)}",
                    modifier = Modifier
                        .padding(all = padding)
                        .align(Alignment.CenterHorizontally))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(enabled = (stateFlow == KmlTourStatus.Paused || stateFlow == KmlTourStatus.Playing),
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .padding(all = padding),
                        onClick = {
                            viewModel.reset()
                        }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_reset_24),
                            contentDescription = null
                        )
                        Text(stringResource(R.string.reset))
                    }
                    Button(modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = padding),
                        onClick = {
                            // play tour
                            viewModel.playOrPause()
                        }) {
                        if (stateFlow == KmlTourStatus.Playing) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_round_pause_24),
                                contentDescription = null
                            )
                            Text(stringResource(R.string.pause))
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_round_play_arrow_24),
                                contentDescription = null
                            )
                            Text(stringResource(R.string.play))
                        }
                    }
                }
            }

            viewModel.messageDialogVM.apply {
                if (dialogStatus) {
                    MessageDialog(
                        title = messageTitle,
                        description = messageDescription,
                        onDismissRequest = ::dismissDialog
                    )
                }
            }
        }
    )
}

private fun tourStateToString(tourStatus: KmlTourStatus) : String {
    return when (tourStatus) {
        KmlTourStatus.Paused -> "Paused"
        KmlTourStatus.Playing -> "Playing"
        KmlTourStatus.Completed -> "Completed"
        KmlTourStatus.Initializing -> "Initializing"
        KmlTourStatus.Initialized -> "Initialized"
        KmlTourStatus.NotInitialized -> "Not Initialized"
    }
}
