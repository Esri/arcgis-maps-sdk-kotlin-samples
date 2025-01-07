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

package com.esri.arcgismaps.sample.queryfeatureswitharcadeexpression.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.queryfeatureswitharcadeexpression.R
import com.esri.arcgismaps.sample.queryfeatureswitharcadeexpression.components.LoadState
import com.esri.arcgismaps.sample.queryfeatureswitharcadeexpression.components.QueryFeaturesWithArcadeExpressionViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun QueryFeaturesWithArcadeExpressionScreen(sampleName: String) {
    val mapViewModel: QueryFeaturesWithArcadeExpressionViewModel = viewModel()

    val queryState by mapViewModel.queryStateFlow.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    MapView(
                        modifier = Modifier.fillMaxSize(),
                        arcGISMap = mapViewModel.arcGISMap,
                        graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                        mapViewProxy = mapViewModel.mapViewProxy,
                        onSingleTapConfirmed = { tapEvent ->
                            tapEvent.mapPoint?.let { point ->
                                mapViewModel.handleTap(
                                    point = point,
                                    screenCoordinate = tapEvent.screenCoordinate
                                )
                            }
                        }
                    )
                    if (queryState.loadState == LoadState.LOADING) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(96.dp),
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val resultText =
                        queryState.crimes?.let { stringResource(R.string.crime_info_text, it.toInt()) }
                            ?: stringResource(R.string.no_features_found)

                    Text(
                        text = when (queryState.loadState) {
                            LoadState.READY_TO_START -> stringResource(R.string.tap_to_begin)
                            LoadState.LOADING -> stringResource(R.string.loading)
                            LoadState.LOADED -> resultText
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    )
}
