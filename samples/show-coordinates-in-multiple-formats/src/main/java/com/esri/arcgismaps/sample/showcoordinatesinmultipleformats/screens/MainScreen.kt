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

package com.esri.arcgismaps.sample.showcoordinatesinmultipleformats.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.Color
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.layers.ArcGISTiledLayer
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showcoordinatesinmultipleformats.R
import com.esri.arcgismaps.sample.showcoordinatesinmultipleformats.components.MapViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    // create a ViewModel to handle MapView interactions
    val mapViewModel: MapViewModel = viewModel()
    // create a map that has the WGS 84 coordinate system and set this into the map
    val basemapLayer = ArcGISTiledLayer(LocalContext.current.applicationContext.getString(R.string.basemap_url))
    val arcGISMap = ArcGISMap(Basemap(basemapLayer))
    val coordinateLocationGraphic = Graphic(
        geometry = mapViewModel.initialPoint,
        symbol = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Cross,
            color = Color.fromRgba(255, 255, 0, 255),
            size = 20f
        )
    )
    // graphics overlay to display a graphics of the coordinate location
    val graphicsOverlay = GraphicsOverlay().apply {
        graphics.add(coordinateLocationGraphic)
    }
    // the collection of graphics overlays used by the MapView
    val graphicsOverlays = remember { listOf(graphicsOverlay) }
    // update the coordinate notations using the initial point
    LaunchedEffect(Unit) {
        mapViewModel.toCoordinateNotationFromPoint(mapViewModel.initialPoint)
    }
    // track the bottom sheet visibility
    var isBottomSheetVisible by remember { mutableStateOf(true) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            if (!isBottomSheetVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { isBottomSheetVisible = true }) {
                    Icon(
                        imageVector = Icons.Filled.EditLocationAlt,
                        contentDescription = "Show coordinate formats"
                    )
                }
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                MapView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISMap = arcGISMap,
                    graphicsOverlays = graphicsOverlays,
                    onSingleTapConfirmed = { singleTapConfirmedEvent ->
                        // retrieve the map point on MapView tapped
                        val tappedPoint = singleTapConfirmedEvent.mapPoint
                        if (tappedPoint != null) {
                            // update the tapped location graphic
                            coordinateLocationGraphic.geometry = tappedPoint
                            // update the coordinate notations using the tapped point
                            mapViewModel.toCoordinateNotationFromPoint(tappedPoint)
                        }
                    }
                )

                // display a dialog if the sample encounters an error
                mapViewModel.messageDialogVM.apply {
                    if (dialogStatus) {
                        MessageDialog(
                            title = messageTitle,
                            onDismissRequest = ::dismissDialog
                        )
                    }
                }
            }
            BottomSheet(
                sheetTitle = "Set Coordinates",
                isVisible = isBottomSheetVisible,
                onDismissRequest = { isBottomSheetVisible = false }
            ) {
                // layout to display the coordinate text fields.
                CoordinatesLayout(mapViewModel = mapViewModel)
            }
        }
    )
}
