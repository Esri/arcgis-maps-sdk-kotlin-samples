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

package com.esri.arcgismaps.sample.stylepointwithscenesymbol.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SceneSymbolAnchorPosition
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.SurfacePlacement
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbolStyle
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

/**
 * ViewModel for the StylePointWithSceneSymbol sample.
 */
class StylePointWithSceneSymbolViewModel(application: Application) : AndroidViewModel(application) {

    // Message dialog view model used to show errors
    val messageDialogVM = MessageDialogViewModel()

    // Set an initial camera position
    val camera = Camera(
        latitude = 48.973,
        longitude = 4.92,
        altitude = 2082.0,
        heading = 60.0,
        pitch = 75.0,
        roll = 0.0
    )

    // Create the scene used by the SceneView.
    var arcGISScene = ArcGISScene(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = Viewpoint(
            boundingGeometry = camera.location,
            camera = camera
        )
        // add an elevation source to the base surface
        baseSurface.elevationSources.add(
            ArcGISTiledElevationSource(
                "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
            )
        )
    }


    // Graphics overlay that will contain the 3D symbols. SurfacePlacement.Absolute so symbols use absolute Z values.
    val graphicsOverlay = GraphicsOverlay(graphics = makeSceneSymbolGraphics()).apply {
        sceneProperties.surfacePlacement = SurfacePlacement.Absolute
    }

    init {
        // Load the scene and show a message dialog if loading fails
        viewModelScope.launch {
            arcGISScene.load().onFailure {
                messageDialogVM.showMessageDialog(it)
            }
        }
    }

    /**
     * Create a list of graphics each using a [SimpleMarkerSceneSymbol] of different styles.
     */
    private fun makeSceneSymbolGraphics(): List<Graphic> {
        // Scene symbol styles to show
        val styles = listOf(
            SimpleMarkerSceneSymbolStyle.Cone,
            SimpleMarkerSceneSymbolStyle.Cube,
            SimpleMarkerSceneSymbolStyle.Cylinder,
            SimpleMarkerSceneSymbolStyle.Diamond,
            SimpleMarkerSceneSymbolStyle.Sphere,
            SimpleMarkerSceneSymbolStyle.Tetrahedron
        )

        // Starting location and spacing in longitude
        val startLongitude = 4.975
        val latitude = 49.0
        val altitude = 500.0
        val spacing = 0.01

        return styles.mapIndexed { index, style ->
            // Create a scene symbol for the style
            val symbol = SimpleMarkerSceneSymbol(
                style = style,
                color = randomColor(),
                height = 200.0,
                width = 200.0,
                depth = 200.0,
                anchorPosition = SceneSymbolAnchorPosition.Center
            )

            // Position the symbol slightly offset in longitude for each symbol
            val point = Point(
                x = startLongitude + spacing * index,
                y = latitude,
                z = altitude,
                spatialReference = SpatialReference.wgs84()
            )

            Graphic(geometry = point, symbol = symbol)
        }
    }

    /**
     * Helper function to produce a random color using ArcGIS [Color.fromRgba]
     */
    private fun randomColor(): Color {
        val r = (0..255).random()
        val g = (0..255).random()
        val b = (0..255).random()
        return Color.fromRgba(r, g, b, 255)
    }
}
