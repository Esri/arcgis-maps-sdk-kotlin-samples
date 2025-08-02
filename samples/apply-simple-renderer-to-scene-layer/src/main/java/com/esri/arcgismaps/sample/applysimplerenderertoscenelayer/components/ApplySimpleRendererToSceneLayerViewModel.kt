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

package com.esri.arcgismaps.sample.applysimplerenderertoscenelayer.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.symbology.MaterialFillSymbolLayer
import com.arcgismaps.mapping.symbology.MultilayerMeshSymbol
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SymbolLayerEdges3D
import com.arcgismaps.mapping.view.Camera
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

class ApplySimpleRendererToSceneLayerViewModel(app: Application) : AndroidViewModel(app) {
    // URL of the Portland buildings scene server
    private val portlandBuildingsSceneLayerUrl =
        "https://tiles.arcgis.com/tiles/P3ePLMYs2RVChkJx/arcgis/rest/services/Buildings_Portland/SceneServer"

    // URL of the world elevation tiled elevation source
    private val worldElevationServiceUrl =
        "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"

    // ArcGISTiledElevationSource for world elevation
    private val elevationSource: ArcGISTiledElevationSource by lazy {
        ArcGISTiledElevationSource(worldElevationServiceUrl)
    }

    // ArcGISSceneLayer for Portland buildings
    val sceneLayer: ArcGISSceneLayer by lazy {
        ArcGISSceneLayer(portlandBuildingsSceneLayerUrl)
    }

    // Camera location point (Portland, OR)
    private val cameraLocation: Point by lazy {
        Point(
            x = -122.66949,
            y = 45.51869,
            z = 227.0,
            spatialReference = SpatialReference.wgs84()
        )
    }

    // Camera to view the scene
    private val camera: Camera by lazy {
        Camera(
            locationPoint = cameraLocation,
            heading = 219.0,
            pitch = 82.0,
            roll = 0.0
        )
    }

    // Create the ArcGISScene with imagery basemap
    val arcGISScene by mutableStateOf(
        ArcGISScene(BasemapStyle.ArcGISImagery).apply {
            // Add the Portland buildings scene layer
            operationalLayers.add(sceneLayer)
            // Add the elevation source to the base surface
            baseSurface = Surface().apply {
                elevationSources.add(elevationSource)
            }
            // Set the viewpoint camera at Portland
            initialViewpoint = Viewpoint(
                boundingGeometry = cameraLocation,
                camera = camera
            )
        }
    )

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Applies a simple renderer with a random color to the scene layer.
     */
    fun applyRandomSimpleRenderer() {
        // Pick a random color from the list
        val colorList = listOf(Color.red, Color.yellow, Color.blue, Color.green)
        val randomColor = colorList[Random.nextInt(colorList.size)]
        // Create a multilayer mesh symbol
        val materialFillSymbolLayer = MaterialFillSymbolLayer(randomColor).apply {
            edges = SymbolLayerEdges3D(Color.black, 1.0)
        }
        val meshSymbol = MultilayerMeshSymbol(materialFillSymbolLayer)
        // Create and apply the simple renderer
        val renderer = SimpleRenderer(meshSymbol)
        sceneLayer.renderer = renderer
    }
}

// Extension property to provide blue color for ArcGISMaps Color class
val Color.Companion.blue: Color
    get() = fromRgba(0, 0, 255, 255)
