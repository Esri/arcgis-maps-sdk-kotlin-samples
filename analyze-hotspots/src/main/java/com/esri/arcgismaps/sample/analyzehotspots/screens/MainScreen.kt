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

package com.esri.arcgismaps.sample.analyzehotspots.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.analyzehotspots.components.ComposeMapView
import com.esri.arcgismaps.sample.analyzehotspots.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sampleName: String, application: Application) {
    // create a ViewModel to handle MapView interactions
    val mapViewModel = MapViewModel(application)

    Scaffold(topBar = { SampleTopAppBar(title = sampleName) }, content = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            // composable function that wraps the MapView
            ComposeMapView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f), mapViewModel = mapViewModel
            )

            // bottom row to load a portal item on button click
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomAppContent(runAnalysisClicked = { fromDateInMillis, toDateInMillis ->
                    if (fromDateInMillis != null && toDateInMillis != null){
                        CoroutineScope(Dispatchers.Default).launch {
                            mapViewModel.analyzeHotspots(fromDateInMillis, toDateInMillis)
                        }
                    }

                    else{
                        // TODO
                    }
                }
                )
            }
        }
    })
}