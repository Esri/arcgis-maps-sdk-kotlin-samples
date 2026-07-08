/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.changeviewpoint.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.changeviewpoint.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class ChangeViewpointViewModel(app: Application) : AndroidViewModel(app) {

    private val londonEastViewpoint = Viewpoint(
        center = Point(
            x = 0.1275,
            y = 51.5072,
            spatialReference = SpatialReference.wgs84()
        ),
        scale = 4e4
    )

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISImagery).apply {
        initialViewpoint = londonEastViewpoint
    }

    val graphicsOverlay = GraphicsOverlay()

    val mapViewProxy = MapViewProxy()

    private var visibleArea: Polygon? by mutableStateOf(null)

    private var currentMapScale: Double? by mutableStateOf(null)

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Create the geometry from JSON and the simple fill symbol for the graphic
        val griffithParkPolygon = Geometry.fromJsonOrNull(
            json = app.resources.openRawResource(R.raw.griffith_park_geometry_json)
                .bufferedReader()
                .use { it.readText() }
        ) as? Polygon

        val fillSymbol = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = Color.fromRgba(r = 0, g = 128, b = 0, a = 179)
        )

        // Create the graphic using the geometry and symbol, and add it to the graphics overlay
        griffithParkPolygon?.let { polygon ->
            val griffithParkGraphic = Graphic(
                geometry = polygon,
                symbol = fillSymbol
            )
            graphicsOverlay.graphics.add(griffithParkGraphic)
        }

        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Track the current visible area for animated viewpoint logic.
     */
    fun onVisibleAreaChanged(newVisibleArea: Polygon) {
        visibleArea = newVisibleArea
    }

    /**
     * Track current map scale for animated viewpoint logic.
     */
    fun onMapScaleChanged(scale: Double) {
        currentMapScale = scale
    }

    /**
     * Function for when "Geometry" button is clicked
     */
    fun onGeometryClicked() {
        val polygon = graphicsOverlay.graphics[0].geometry ?: return messageDialogVM.showMessageDialog(
            title = "Failed to parse geometry",
            description = "The sample polygon JSON could not be parsed."
        )

        viewModelScope.launch {
            mapViewProxy.setViewpointGeometry(
                boundingGeometry = polygon,
                paddingInDips = 50.0
            )
        }
    }

    /**
     * Function for when "Center" button is clicked
     */
    fun onCenterClicked() {
        val center = londonEastViewpoint.targetGeometry.extent.center
        val scale = londonEastViewpoint.targetScale
        viewModelScope.launch {
            mapViewProxy.setViewpointCenter(
                center = center,
                scale = scale
            )
        }
    }

    /**
     * Function for when "Animate" button is clicked
     */
    fun onAnimateClicked() {
        val center = visibleArea?.extent?.center ?: return
        val scale = currentMapScale ?: return

        viewModelScope.launch {
            val finishedWithoutInterruption = mapViewProxy.setViewpointAnimated(
                viewpoint = Viewpoint(center = center, scale = scale / 2),
                duration = 5.seconds
            ).getOrElse {
                messageDialogVM.showMessageDialog(it)
                false
            }
            if (finishedWithoutInterruption) {
                mapViewProxy.setViewpointAnimated(
                    viewpoint = Viewpoint(center = center, scale = scale),
                    duration = 5.seconds
                )
            }
        }
    }
}
