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

package com.esri.arcgismaps.sample.playkmltour2.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.playkmltour2.R
import com.esri.arcgismaps.sample.playkmltour2.components.PlayKMLTour2ViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun PlayKMLTour2Screen(sampleName: String) {
    val viewModel: PlayKMLTour2ViewModel = viewModel()
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
                    arcGISScene = viewModel.arcGISScene
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = { 100.0f })
                Text(stringResource(R.string.tour_status))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        // reset tour
                    }) {
                        Text(stringResource(R.string.reset))
                    }
                    Button(modifier = Modifier.fillMaxWidth(0.5f), onClick = {
                        // play tour
                        viewModel.kmlTourController.play()
                    }) {
                        Text(stringResource(R.string.play))
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

@Preview
@Composable
fun Preview() {
    Scaffold(
        topBar = { SampleTopAppBar(title = "Play KML tour") },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = { 100.0f })
                Text("Tour status:")
                Row(modifier = Modifier.fillMaxWidth()) {
                    // TODO: Add UI components in this Column ...
                    Button(modifier = Modifier.fillMaxWidth(0.5f), onClick = {}) {
                        Text("Play tour")
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                        Text("Reset tour")
                    }
                }
            }
        }
    )
}
