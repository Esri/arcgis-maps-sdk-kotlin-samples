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

package com.esri.arcgismaps.sample.queryfeaturetable.screens

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.esri.arcgismaps.sample.queryfeaturetable.components.ComposeMapView
import com.esri.arcgismaps.sample.queryfeaturetable.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String, application: Application) {
    // coroutineScope that will be cancelled when this call leaves the composition
    val sampleCoroutineScope = rememberCoroutineScope()
    // create a ViewModel to handle MapView interactions
    val mapViewModel = remember { MapViewModel(application, sampleCoroutineScope) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(modifier = Modifier.fillMaxSize().padding(it)) {
                // composable function that wraps the MapView
                ComposeMapView(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    mapViewModel = mapViewModel
                )
                SearchBar(
                    modifier = Modifier.fillMaxWidth(),
                    onQuerySubmit = mapViewModel::searchForState
                )
            }

            mapViewModel.messageDialogVM.apply {
                if (dialogStatus.value) {
                    // display a dialog if the sample encounters an error
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
