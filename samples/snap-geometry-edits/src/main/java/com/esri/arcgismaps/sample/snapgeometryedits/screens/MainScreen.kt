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

package com.esri.arcgismaps.sample.snapgeometryedits.screens

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.snapgeometryedits.components.MapViewModel

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
    // the collection of graphics overlays used by the MapView
    val graphicsOverlayCollection = listOf(mapViewModel.graphicsOverlay)

    Scaffold(
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                SampleTopAppBar(title = sampleName)
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.map,
                    geometryEditor = mapViewModel.geometryEditor,
                    graphicsOverlays = graphicsOverlayCollection,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    onSingleTapConfirmed = mapViewModel::identify,
                    onPan = { mapViewModel.dismissBottomSheet() }
                )
                ButtonMenu(mapViewModel = mapViewModel)
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
            BottomSheet(isVisible = mapViewModel.isBottomSheetVisible.value) {
                SnapSettings(
                    snapSourceList = mapViewModel.snapSourceList.collectAsState(),
                    onSnappingChanged = mapViewModel::snappingEnabledStatus,
                    onSnapSourceChanged = mapViewModel::sourceEnabledStatus,
                    isSnappingEnabled = mapViewModel.snappingCheckedState.value,
                    isSnapSourceEnabled = mapViewModel.snapSourceCheckedState
                ) { mapViewModel.dismissBottomSheet() }
            }
        }
    )
}
