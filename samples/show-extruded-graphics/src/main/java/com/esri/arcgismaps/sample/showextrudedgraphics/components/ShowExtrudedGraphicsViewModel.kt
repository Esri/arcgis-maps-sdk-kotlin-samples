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

package com.esri.arcgismaps.sample.showextrudedgraphics.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.ExtrusionMode
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SurfacePlacement
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.symbology.RendererSceneProperties
import com.arcgismaps.mapping.view.Camera
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel for the ShowExtrudedGraphics sample.
 *
 * Prepares a 3D scene, adds an elevation source and a graphics overlay whose
 * renderer is configured to extrude graphics based on the "height" attribute.
 */
class ShowExtrudedGraphicsViewModel(application: Application) : AndroidViewModel(application) {

    // Create a camera and set the scene's initial viewpoint.
    private val camera = Camera(
        latitude = 28.4,
        longitude = 83.0,
        altitude = 20_000.0,
        heading = 10.0,
        pitch = 70.0,
        roll = 0.0
    )
    // A 3D scene with a topographic basemap and an initial camera viewpoint.
    val arcGISScene = ArcGISScene(BasemapStyle.ArcGISTopographic).apply {

        initialViewpoint = Viewpoint(camera = camera, boundingGeometry = camera.location)

        // Add a global elevation source to the base surface so extruded geometry has context.
        baseSurface.elevationSources.add(
            ArcGISTiledElevationSource(
                uri = WORLD_ELEVATION_SERVICE_URL
            )
        )
    }


    // Graphics overlay that will contain extruded polygon graphics.
    val graphicsOverlay: GraphicsOverlay

    // Message dialog view model for reporting load errors.
    val messageDialogVM = MessageDialogViewModel()

    val squareSize = 0.01

    init {
        // Build renderer configured to extrude using the "height" attribute.
        val outline = SimpleLineSymbol(
            style = SimpleLineSymbolStyle.Solid,
            color = Color.white,
            width = 1f
        )

        val fillSymbol = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = Color.fromRgba(220, 50, 50, 200),
            outline = outline
        )

        val renderer = SimpleRenderer(symbol = fillSymbol).apply {
            // Set extrusion mode and expression so the renderer uses the "height" attribute.
            sceneProperties = RendererSceneProperties().apply {
                extrusionMode = ExtrusionMode.BaseHeight
                extrusionExpression = "[height]"
            }
        }

        // Initialize the graphics overlay and assign the renderer.
        graphicsOverlay = GraphicsOverlay().apply {
            sceneProperties.surfacePlacement = SurfacePlacement.DrapedBillboarded
            this.renderer = renderer
        }

        // Populate the overlay with extruded graphics.
        addExtrudedGraphics()
        viewModelScope.launch {
            
            // Load the scene; if loading fails, show a message.
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    // Adds a grid of square polygons to the graphics overlay and assigns each a random height.
    private fun addExtrudedGraphics() {
        val baseX = camera.location.x - 0.01
        val baseY = camera.location.y + 0.25
        val spacing = 0.01
        val columns = 6
        val rows = 4
        val maxHeight = 10_000

        for (column in 0 until columns) {
            for (row in 0 until rows) {
                val startX = baseX + column * (squareSize + spacing)
                val startY = baseY + row * (squareSize + spacing)
                val polygon = polygonForPoint(startX, startY)
                val height = Random.nextInt(0, maxHeight + 1)
                val graphic = Graphic(geometry = polygon).apply {
                    // The renderer will use this attribute to extrude the graphic.
                    attributes["height"] = height
                }
                graphicsOverlay.graphics.add(graphic)
            }
        }
    }

    // Helper to construct a square polygon given a lower-left origin.
    private fun polygonForPoint(x: Double, y: Double) =
        PolygonBuilder().apply {
            addPoint(Point(x = x, y = y))
            addPoint(Point(x = x, y = y + squareSize))
            addPoint(Point(x = x + squareSize, y = y + squareSize))
            addPoint(Point(x = x + squareSize, y = y))
        }.toGeometry()

    companion object {
        // World elevation service used to provide base surface elevation.
        private const val WORLD_ELEVATION_SERVICE_URL =
            "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
    }
}
