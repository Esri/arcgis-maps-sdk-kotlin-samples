/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.identifygraphics.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.unit.dp
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.identifygraphics.screens.MessageDialogState
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class IdentifyGraphicsViewModel(app: Application) : AndroidViewModel(app) {

    // Create a polygon from a list of points
    private val polygon: Polygon = PolygonBuilder(SpatialReference.webMercator()).apply {
        addPoint(Point(x = -20e5, y = 20e5))
        addPoint(Point(x = 20e5, y = 20e5))
        addPoint(Point(x = 20e5, y = -20e5))
        addPoint(Point(x = -20e5, y = -20e5))
    }.toGeometry()

    // ArcGISMap displayed by the MapView using a topographic basemap.
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        // Set an initial viewpoint with the polygon graphic centered on the screen.
        initialViewpoint = Viewpoint(
            center = polygon.extent.center,
            scale = 1.3e8
        )
    }

    // MapViewProxy enables identify operations from the ViewModel.
    val mapViewProxy = MapViewProxy()

    // Graphics overlay that holds the polygon graphic and a renderer.
    private val graphicsOverlay = GraphicsOverlay().apply {
        // Configure the graphics overlay with a renderer and add a graphic for the polygon.
        val polygonFillSymbol = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = Color.yellow
        )

        renderer = SimpleRenderer(polygonFillSymbol)
        graphics.add(Graphic(geometry = polygon))
    }

    val graphicsOverlays = listOf(graphicsOverlay)

    // Dialog state to present error messages.
    val messageDialogState = MessageDialogState()

    init {
        // Load the map and handle any load failures.
        viewModelScope.launch {
            arcGISMap.load().onFailure { error ->
                messageDialogState.showError(error)
            }
        }
    }

    /**
     * Called when single tap is detected on the map. Identifies graphics in [graphicsOverlay] near the
     * [screenCoordinate] using [mapViewProxy], then shows the number of identified graphics in an
     * alert dialog.
     */
    fun identifyGraphics(screenCoordinate: ScreenCoordinate) {
        viewModelScope.launch {
            mapViewProxy.identify(
                graphicsOverlay = graphicsOverlay,
                screenCoordinate = screenCoordinate,
                tolerance = 12.dp,
                returnPopupsOnly = false,
                maximumResults = 10
            ).onSuccess { identifyResult ->
                val count = identifyResult.graphics.size
                val message = if (count > 0) {
                    "Tapped on $count graphic(s)."
                } else {
                    "No graphics at tapped location."
                }
                // Show an alert dialog with the identify result.
                messageDialogState.showMessage(
                    title = "Identify Result",
                    description = message
                )
            }.onFailure { error ->
                messageDialogState.showError(error)
            }
        }
    }
}
