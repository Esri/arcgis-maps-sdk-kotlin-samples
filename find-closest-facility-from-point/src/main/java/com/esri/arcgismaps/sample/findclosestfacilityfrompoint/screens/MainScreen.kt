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

package com.esri.arcgismaps.sample.findclosestfacilityfrompoint.screens

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.findclosestfacilityfrompoint.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {

    val context = LocalContext.current
    val application = context.applicationContext as Application
    ArcGISEnvironment.applicationContext = application
    val mapViewModel = remember { MapViewModel(application) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                arcGISMap = mapViewModel.map,
                graphicsOverlays = mapViewModel.graphicsOverlays,
                onSingleTapConfirmed = { event ->
                    mapViewModel.onSingleTapConfirmed(mapViewModel.currentJob, event, mapViewModel.incidentGraphicsOverlay)
                },
            )
        }
    )
}


