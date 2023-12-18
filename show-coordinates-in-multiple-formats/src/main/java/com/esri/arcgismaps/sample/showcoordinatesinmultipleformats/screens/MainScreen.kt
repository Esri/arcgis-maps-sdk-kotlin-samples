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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.layers.ArcGISTiledLayer
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.geocompose.MapView
import com.arcgismaps.toolkit.geocompose.rememberGraphicsOverlayCollection
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
    // graphics overlay for the MapView to draw the graphics
    val graphicsOverlay = remember { GraphicsOverlay() }
    // the collection of graphics overlays used by the MapView
    val graphicsOverlayCollection = rememberGraphicsOverlayCollection().apply {
        add(graphicsOverlay)
    }

    graphicsOverlay.graphics.add(mapViewModel.coordinateLocationGraphic)
    // update the coordinate notations using the initial point
    mapViewModel.toCoordinateNotationFromPoint(mapViewModel.initialPoint)

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                // layout to display the coordinate text fields.
                CoordinatesLayout(mapViewModel = mapViewModel)
                MapView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISMap = arcGISMap,
                    graphicsOverlays = graphicsOverlayCollection,
                    onSingleTapConfirmed = { singleTapConfirmedEvent ->
                        // retrieve the map point on MapView tapped
                        val tappedPoint = singleTapConfirmedEvent.mapPoint
                        if (tappedPoint != null) {
                            // update the tapped location graphic
                            mapViewModel.coordinateLocationGraphic.geometry = tappedPoint
                            graphicsOverlay.graphics.apply {
                                clear()
                                add(mapViewModel.coordinateLocationGraphic)
                            }
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
        }
    )
}
