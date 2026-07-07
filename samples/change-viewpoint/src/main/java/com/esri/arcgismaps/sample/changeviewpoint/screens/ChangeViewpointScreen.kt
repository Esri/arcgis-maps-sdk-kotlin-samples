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

import android.provider.Settings.Global.getString
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.changeviewpoint.R
import com.esri.arcgismaps.sample.changeviewpoint.components.ChangeViewpointViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Main screen layout for the sample app
 */
@Composable
fun ChangeViewpointScreen(sampleName: String) {
    val mapViewModel: ChangeViewpointViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            //Mapview
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                arcGISMap = mapViewModel.arcGISMap,
                mapViewProxy = mapViewModel.mapViewProxy
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { mapViewModel.onGeometryClicked() }) {
                    Text(text = stringResource(R.string.geometry))
                }

                Button(onClick = { mapViewModel.onCenterClicked() }) {
                    Text(text = stringResource(R.string.center))
                }

                Button(onClick = { mapViewModel.onAnimateClicked() }) {
                    Text(text = stringResource((R.string.animate)))
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
