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

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.ArcGISTiledLayer
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.geocompose.GraphicsOverlayCollection
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
fun MainScreen(sampleName: String, application: Application) {
    // create a ViewModel to handle MapView interactions
    val mapViewModel: MapViewModel = viewModel()
    // create a map that has the WGS 84 coordinate system and set this into the map
    val basemapLayer = ArcGISTiledLayer(application.getString(R.string.basemap_url))
    val arcGISMap = ArcGISMap(Basemap(basemapLayer))
    // the collection of graphics overlays used by the MapView
    val graphicsOverlays = rememberGraphicsOverlayCollection()



    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { it ->
            Column(
                modifier = Modifier.fillMaxSize().padding(it)
            ) {
                // layout to display the coordinate text fields.
                CoordinatesLayout(mapViewModel = mapViewModel)
                MapView(
                    modifier = Modifier.fillMaxSize().padding(it),
                    arcGISMap = arcGISMap,
                    graphicsOverlays = graphicsOverlays,
                    onSingleTapConfirmed = { singleTapConfirmedEvent ->
                        /**
                         * Updates the tapped graphic and coordinate notations using the [tappedPoint]
                         */
                        if (singleTapConfirmedEvent.mapPoint != null) {
                            // update the tapped location graphic
                            mapViewModel.coordinateLocation.geometry = singleTapConfirmedEvent.mapPoint
                            graphicsOverlays.onEach {
                                it.graphics.apply {
                                    clear()
                                    add(mapViewModel.coordinateLocation)
                                }
                                // update the coordinate notations using the tapped point
                                mapViewModel.toCoordinateNotationFromPoint(singleTapConfirmedEvent.mapPoint!!)
                            }
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
