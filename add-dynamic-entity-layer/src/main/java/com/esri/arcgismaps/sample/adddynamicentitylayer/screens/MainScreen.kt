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

package com.esri.arcgismaps.sample.adddynamicentitylayer.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geocompose.MapView
import com.arcgismaps.toolkit.geocompose.MapViewpointOperation
import com.esri.arcgismaps.sample.adddynamicentitylayer.components.DynamicEntityLayerProperties
import com.esri.arcgismaps.sample.adddynamicentitylayer.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    /// coroutineScope that will be cancelled when this call leaves the composition
    val sampleCoroutineScope = rememberCoroutineScope()
    // get the application context
    val application = LocalContext.current.applicationContext as Application
    
    // create a ViewModel to handle MapView interactions
    val mapViewModel = remember { MapViewModel(application, sampleCoroutineScope) }

    // display connect/disconnect based on the boolean state
    var isDisconnected by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                // composable function that wraps the MapView
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.map,
                    viewpointOperation = MapViewpointOperation.Set(viewpoint = Viewpoint(40.559691, -111.869001, 150000.0))
                )
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        if (!isDisconnected)
                            mapViewModel.disconnectStreamService()
                        else
                            mapViewModel.connectStreamService()
                        isDisconnected = !isDisconnected
                    }) {
                        Text(text = if (!isDisconnected) "Disconnect" else "Connect")
                    }
                    TextButton(onClick = {
                        mapViewModel.showBottomSheet()
                    }) {
                        Text(text = "Dynamic Entity Settings")
                    }
                }

            }
            // display a bottom sheet to set dynamic entity layer properties
            BottomSheet(isVisible = mapViewModel.isBottomSheetVisible.value) {
                DynamicEntityLayerProperties(
                    onTrackLineVisibilityChanged = mapViewModel::trackLineVisibility,
                    onPrevObservationsVisibilityChanged = mapViewModel::prevObservationsVisibility,
                    onObservationsChanged = mapViewModel::setObservations,
                    onPurgeAllObservations = mapViewModel::purgeAllObservations,
                    isTrackLineVisible = mapViewModel.trackLineCheckedState.value,
                    isPrevObservationsVisible = mapViewModel.prevObservationCheckedState.value,
                    observationsPerTrack = mapViewModel.trackSliderValue.value,
                    onDismiss = { mapViewModel.dismissBottomSheet() }
                )
            }
        }
    )
}
