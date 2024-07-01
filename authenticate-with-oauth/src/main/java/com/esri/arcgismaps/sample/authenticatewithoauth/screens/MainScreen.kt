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

package com.esri.arcgismaps.sample.authenticatewithoauth.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.authentication.DialogAuthenticator
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.authenticatewithoauth.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {

    // create a ViewModel to handle interactions
    val mapViewModel: MapViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                mapViewProxy = mapViewModel.mapViewProxy,
                arcGISMap = mapViewModel.arcGISMap
            )
            // Displays appropriate Authentication UI when an authentication challenge is issued.
            // Because the authenticatorState has an oAuthUserConfiguration set, authentication
            // challenges will happen via OAuth.
            DialogAuthenticator(authenticatorState = mapViewModel.authenticatorState)
        }
    )
}
