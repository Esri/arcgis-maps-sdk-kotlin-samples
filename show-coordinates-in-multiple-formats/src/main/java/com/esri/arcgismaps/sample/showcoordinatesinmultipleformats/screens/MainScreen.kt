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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.layers.ArcGISTiledLayer
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
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
fun MainScreen(sampleName: String, application: Application) {
    // create a ViewModel to handle MapView interactions
    val mapViewModel: MapViewModel = viewModel()
    // create a map that has the WGS 84 coordinate system and set this into the map
    val basemapLayer = ArcGISTiledLayer(application.getString(R.string.basemap_url))
    val arcGISMap = ArcGISMap(Basemap(basemapLayer))
    // the collection of graphics overlays used by the MapView
    val graphicsOverlays = rememberGraphicsOverlayCollection()
    // the collection of graphics overlays used by the MapView
    val graphicsOverlay = remember { GraphicsOverlay() }
    // set up a graphic to indicate where the coordinates relate to, with an initial location
    val initialPoint = Point(0.0, 0.0, SpatialReference.wgs84())

    val coordinateLocation = Graphic(
        geometry = initialPoint,
        symbol = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Cross,
            color = Color.fromRgba(255, 255, 0, 255),
            size = 20f
        )
    )
    graphicsOverlay.graphics.add(coordinateLocation)
    // update the coordinate notations using the initial point
    coordinateLocation.geometry = initialPoint
    mapViewModel.toCoordinateNotationFromPoint(initialPoint)

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { it ->
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
                    graphicsOverlays = graphicsOverlays.apply {
                        this.add(graphicsOverlay)
                    },
                    onSingleTapConfirmed = { singleTapConfirmedEvent ->
                        /**
                         * Updates the tapped graphic and coordinate notations using the [tappedPoint]
                         */
                        if (singleTapConfirmedEvent.mapPoint != null) {
                            // update the tapped location graphic
                            coordinateLocation.geometry = singleTapConfirmedEvent.mapPoint
                            graphicsOverlay.graphics.clear()
                            graphicsOverlay.graphics.add(coordinateLocation)
                            // update the coordinate notations using the tapped point
                            coordinateLocation.geometry = singleTapConfirmedEvent.mapPoint
                            mapViewModel.toCoordinateNotationFromPoint(
                                singleTapConfirmedEvent.mapPoint!!
                            )
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
