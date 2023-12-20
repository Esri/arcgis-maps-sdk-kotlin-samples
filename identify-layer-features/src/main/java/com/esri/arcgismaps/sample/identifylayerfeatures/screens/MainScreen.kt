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

package com.esri.arcgismaps.sample.identifylayerfeatures.screens

import android.app.Application
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geocompose.MapView
import com.arcgismaps.toolkit.geocompose.MapViewProxy
import com.arcgismaps.toolkit.geocompose.MapViewpointOperation
import com.esri.arcgismaps.sample.identifylayerfeatures.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.launch

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    // coroutineScope that will be cancelled when this call leaves the composition
    val sampleCoroutineScope = rememberCoroutineScope()
    // get the application property that will be used to construct MapViewModel
    val sampleApplication = LocalContext.current.applicationContext as Application
    // create a ViewModel to handle MapView interactions
    val mapViewModel = remember { MapViewModel(sampleApplication, sampleCoroutineScope) }
    // create a mapViewProxy that will be used to identify features in the MapView
    // should also be passed to the MapView composable this mapViewProxy is associated with
    val mapViewProxy = MapViewProxy()
    // create a Viewpoint
    val viewpoint = Viewpoint(
        center = Point(
            x = -10977012.785807,
            y = 4514257.550369,
            spatialReference = SpatialReference(wkid = 3857)
        ),
        scale = 68015210.0
    )

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .animateContentSize(),
                    arcGISMap = mapViewModel.arcGISMap,
                    viewpointOperation = MapViewpointOperation.Set(viewpoint = viewpoint),
                    mapViewProxy = mapViewProxy,
                    onSingleTapConfirmed = { singleTapConfirmedEvent ->
                        sampleCoroutineScope.launch {
                            val identifyResult = mapViewProxy.identifyLayers(
                                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                                tolerance = 12.dp,
                                maximumResults = 10
                            )
                            mapViewModel.handleIdentifyResult(identifyResult)
                        }
                    }
                )
                // Bottom text to display the identify results
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Text(text = mapViewModel.bottomTextBanner.value)
                }
                // display a dialog if the sample encounters an error
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
    )
}
