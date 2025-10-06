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

package com.esri.arcgismaps.sample.stylepointwithdistancecompositescenesymbol.components

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
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.symbology.DistanceCompositeSceneSymbol
import com.arcgismaps.mapping.symbology.DistanceSymbolRange
import com.arcgismaps.mapping.symbology.ModelSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.SurfacePlacement
import com.arcgismaps.mapping.view.OrbitGeoElementCameraController
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.stylepointwithdistancecompositescenesymbol.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * ViewModel for the sample. Builds an ArcGISScene, a distance composite scene symbol and an orbit
 * camera controller.
 */
class StylePointWithDistanceCompositeSceneSymbolViewModel(app: Application) :
    AndroidViewModel(app) {

    // Lazy provision path for reference offline resources.
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() +
                File.separator +
                app.getString(R.string.style_point_with_distance_composite_scene_symbol_app_name)
    }

    // Construct the model file URI from the provision path.
    private val bristolModelUri
        get() = "$provisionPath${File.separator}Bristol.dae"

    // The model (3D) graphic target.
    private val planePosition = Point(
        x = -2.708, y = 56.096, z = 5000.0,
        spatialReference = SpatialReference.wgs84()
    )

    // Distance composite symbol with three ranges (detailed model, simplified model, and a simple circle).
    private val distanceCompositeSymbol: DistanceCompositeSceneSymbol by lazy {
        // Close-up: Detailed 3D model.
        val planeModel = ModelSceneSymbol(
            uri = bristolModelUri,
            scale = 100.0F
        )

        // Mid-distance: Simple cone symbol.
        val coneSymbol = SimpleMarkerSceneSymbol.cone(
            color = Color.red,
            diameter = 200.0,
            height = 600.0
        )

        // Far-distance: Simple 2D symbol.
        val circleSymbol = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Circle,
            color = Color.red,
            size = 10f
        )

        DistanceCompositeSceneSymbol().apply {
            // Close-up: Detailed 3D model.
            ranges.add(
                DistanceSymbolRange(
                    symbol = planeModel,
                    minDistance = null,
                    maxDistance = 10000.0
                )
            )
            // Mid-distance: Simple cone symbol.
            ranges.add(
                DistanceSymbolRange(
                    symbol = coneSymbol,
                    minDistance = 10001.0,
                    maxDistance = 30000.0
                )
            )
            // Far-distance: Simple 2D symbol.
            ranges.add(
                DistanceSymbolRange(
                    symbol = circleSymbol,
                    minDistance = 30001.0,
                    maxDistance = null
                )
            )
        }
    }

    // Graphic for the plane using the distance composite symbol.
    private val planeGraphic by lazy {
        Graphic(
            geometry = planePosition,
            symbol = distanceCompositeSymbol
        )
    }

    // Orbit camera controller that targets the plane graphic.
    val orbitCameraController: OrbitGeoElementCameraController by lazy {
        OrbitGeoElementCameraController(planeGraphic, 4000.0).apply {
            setCameraPitchOffset(80.0)
            setCameraHeadingOffset(-30.0)
        }
    }

    // Scene using imagery basemap and a world elevation service.
    val arcGISScene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
        // Set the tiled elevation source
        baseSurface.elevationSources += ArcGISTiledElevationSource(
            uri = "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
        )
        // Set the initial viewpoint
        val camera = Camera(
            latitude = 56.096,
            longitude = -2.708,
            altitude = 5000.0,
            heading = -30.0,
            pitch = 80.0,
            roll = 0.0
        )
        initialViewpoint = Viewpoint(boundingGeometry = camera.location, camera = camera)
    }

    // Graphics overlay to display the plane graphic using a distance composite symbol.
    private val graphicsOverlay by lazy {
        GraphicsOverlay(graphics = listOf(planeGraphic)).apply {
            sceneProperties.surfacePlacement = SurfacePlacement.Relative
        }
    }

    // Expose the graphics overlay list for the SceneView
    val graphicsOverlays = listOf(graphicsOverlay)

    // SceneView proxy to hand to the composable SceneView
    val sceneViewProxy = SceneViewProxy()

    // Flow exposing the distance between camera and target (meters).
    private val _cameraDistanceMeters = MutableStateFlow(0.0)
    val cameraDistanceMeters = _cameraDistanceMeters.asStateFlow()

    // Message dialog VM to surface errors.
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the scene and surface errors via the message dialog VM.
        viewModelScope.launch {
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }

        // Collect the cameraDistance flow exposed by the controller
        viewModelScope.launch {
            // Update flow to keep UI reactive.
            orbitCameraController.cameraDistance.collect { distance ->
                _cameraDistanceMeters.value = distance
            }
        }
    }
}
