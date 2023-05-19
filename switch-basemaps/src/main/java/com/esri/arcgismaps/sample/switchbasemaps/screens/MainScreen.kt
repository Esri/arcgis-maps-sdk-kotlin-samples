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

package com.esri.arcgismaps.sample.switchbasemaps.screens

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.colorPrimary
import com.esri.arcgismaps.sample.switchbasemaps.components.ComposeMapView
import com.esri.arcgismaps.sample.switchbasemaps.components.MapViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String, application: Application) {
    // create a ViewModel to handle MapView interactions
    val mapViewModel = MapViewModel(application)

    Scaffold(
        topBar = { TopAppBar(
            title = { Text(text = sampleName) },
        ) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                // composable function that wraps the MapView
                ComposeMapView(
                    modifier = Modifier.fillMaxSize(),
                    mapViewModel = mapViewModel,
                    onSingleTap = {
                        mapViewModel.changeBasemap()
                    }
                )
            }
        }
    )
}
