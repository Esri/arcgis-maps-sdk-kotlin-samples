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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.esri.arcgismaps.sample.traceutilitynetwork.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.utilitynetworks.AddStartingPointMode
import com.arcgismaps.toolkit.utilitynetworks.Trace
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.traceutilitynetwork.components.TraceUtilityNetworkViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Main screen layout for the sample app
 */
@Composable
fun TraceUtilityNetworkScreen(sampleName: String) {

    val mapViewModel: TraceUtilityNetworkViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val sheetHeight = (LocalConfiguration.current.screenHeightDp * 0.4)

    val addStartingPointMode by mapViewModel.traceState.addStartingPointMode
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded,
            skipHiddenState = true
        )
    )

    LaunchedEffect(key1 = addStartingPointMode) {
        if (addStartingPointMode == AddStartingPointMode.Started) {
            scaffoldState.bottomSheetState.partialExpand()
        } else if (addStartingPointMode == AddStartingPointMode.Stopped) {
            scaffoldState.bottomSheetState.expand()
        }
    }

    BottomSheetScaffold(
        sheetContent = {
            Trace(
                modifier = Modifier.height(sheetHeight.dp),
                traceState = mapViewModel.traceState
            )
        },
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        sheetSwipeEnabled = true,
        topBar = { SampleTopAppBar(sampleName) },
    ) { padding ->
        MapView(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            arcGISMap = mapViewModel.arcGISMap,
            mapViewProxy = mapViewModel.mapViewProxy,
            graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
            onPan = { coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() } },
            onSingleTapConfirmed = { tapEvent ->
                coroutineScope.launch {
                    tapEvent.mapPoint?.let {
                        mapViewModel.traceState.addStartingPoint(it)
                    }
                }
            }
        )
    }
}


