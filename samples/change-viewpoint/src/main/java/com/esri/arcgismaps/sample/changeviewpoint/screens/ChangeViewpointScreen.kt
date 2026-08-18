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

package com.esri.arcgismaps.sample.changeviewpoint.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.changeviewpoint.R
import com.esri.arcgismaps.sample.changeviewpoint.components.ChangeViewpointViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun ChangeViewpointScreen(sampleName: String) {
    val mapViewModel: ChangeViewpointViewModel = viewModel()

    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        topBar = { SampleTopAppBar(title = sampleName) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                arcGISMap = mapViewModel.arcGISMap,
                graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                mapViewProxy = mapViewModel.mapViewProxy,
                onMapScaleChanged = mapViewModel::onMapScaleChanged,
                onVisibleAreaChanged = mapViewModel::onVisibleAreaChanged
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier.testTag("change-viewpoint:geometry"),
                    onClick = mapViewModel::onGeometrySelected,
                ) {
                    Text(text = stringResource(R.string.geometry))
                }

                Button(onClick = mapViewModel::onCenterSelected) {
                    Text(text = stringResource(R.string.center_and_scale))
                }

                Button(
                    modifier = Modifier.testTag("change-viewpoint:animate"),
                    onClick = mapViewModel::onAnimateSelected,
                ) {
                    Text(text = stringResource(R.string.animate))
                }
            }
        }
        mapViewModel.messageDialogVM.apply {
            if (dialogStatus) {
                MessageDialog(
                    title = messageTitle,
                    description = messageDescription,
                    onDismissRequest = ::dismissDialog
                )
            }
        }
    }
}
