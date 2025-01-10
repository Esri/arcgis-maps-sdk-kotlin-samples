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

package com.esri.arcgismaps.sample.projectgeometry.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.projectgeometry.R
import com.esri.arcgismaps.sample.projectgeometry.components.ProjectGeometryViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import java.util.Locale

/**
 * Main screen layout for the sample app
 */
@Composable
fun ProjectGeometryScreen(sampleName: String) {
    val mapViewModel: ProjectGeometryViewModel = viewModel()
    val pointProjectionInfo by mapViewModel.pointProjectionFlow.collectAsStateWithLifecycle()

    val infoText = pointProjectionInfo?.let { (original, projection) ->
        stringResource(
            R.string.projection_info_text,
            original.toDisplayFormat(),
            projection.toDisplayFormat()
        )
    } ?: ""

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    onSingleTapConfirmed = mapViewModel::onMapViewTapped
                )

                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = pointProjectionInfo?.let { stringResource(R.string.title_text) }
                                ?: stringResource(R.string.tap_to_begin),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = infoText,
                            minLines = 2
                        )
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
    )
}

/**
 * Extension function for the Point type that returns
 * a float-precision formatted string suitable for display
 */
private fun Point.toDisplayFormat() =
    "${String.format(Locale.getDefault(),"%.5f", x)}, ${String.format(Locale.getDefault(),"%.5f", y)}"
