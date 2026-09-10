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

package com.esri.arcgismaps.sample.orbitcameraaroundobject.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.DistanceCompositeSceneSymbol
import com.arcgismaps.mapping.symbology.DistanceSymbolRange
import com.arcgismaps.mapping.symbology.ModelSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.OrbitGeoElementCameraController
import com.arcgismaps.mapping.view.SurfacePlacement
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.orbitcameraaroundobject.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class OrbitCameraAroundObjectViewModel(app: Application) : AndroidViewModel(app) {

    // Lazy provision path for reference offline resources.
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() +
                File.separator +
                app.getString(R.string.orbit_camera_around_object_app_name)
    }

    // Construct the model file URI from the provision path.
    private val bristolModelUri
        get() = "$provisionPath${File.separator}Bristol.dae"


    // The model (3D) graphic target.
    private val planePosition = Point(
        x = -2.708, y = 56.096, z = 5000.0,
        spatialReference = SpatialReference.wgs84()
    )

    // The plane's own 3D model symbol.
    private val planeModelSymbol: ModelSceneSymbol by lazy {
        ModelSceneSymbol(
            uri = bristolModelUri,
            scale = 100.0F
        )
    }

    // Distance composite symbol showing the detailed 3D model at close range.
    private val distanceCompositeSymbol: DistanceCompositeSceneSymbol by lazy {
        DistanceCompositeSceneSymbol().apply {
            ranges.add(
                DistanceSymbolRange(
                    symbol = planeModelSymbol,
                    minDistance = null,
                    maxDistance = null
                )
            )
        }
    }

    // Graphic for the plane using the distance composite symbol.
    // Define PITCH attributes for OrbitController's AutoPitch function
    private val planeGraphic by lazy {
        Graphic(
            geometry = planePosition,
            attributes = mapOf("PITCH" to 0.0),
            symbol = distanceCompositeSymbol
        )
    }

    // Orbit camera controller that targets the plane graphic.
    val orbitCameraController: OrbitGeoElementCameraController by lazy {
        OrbitGeoElementCameraController(planeGraphic, 2000.0).apply {
            setCameraPitchOffset(50.0)
            setCameraHeadingOffset(0.0)
        }
    }

    // Graphics overlay to display the plane graphic using a distance composite symbol.
    private val graphicsOverlay by lazy {
        GraphicsOverlay(graphics = listOf(planeGraphic)).apply {
            sceneProperties.surfacePlacement = SurfacePlacement.Relative
            // Rotate the plane graphic based on its PITCH attribute.
            renderer = SimpleRenderer().apply {
                sceneProperties.pitchExpression = "[PITCH]"
            }
        }
    }

    // Expose the graphics overlay list for the SceneView
    val graphicsOverlays = listOf(graphicsOverlay)

    // SceneView proxy to hand to the composable SceneView
    val sceneViewProxy = SceneViewProxy()

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

    // Create a state flow to hold the UI state for the supporting pane controls
    private val _adaptiveUiState = MutableStateFlow(AdaptiveUiState.defaultState)

    // Expose the state flow as read-only for the UI
    val adaptiveUiState = _adaptiveUiState.asStateFlow()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the Scene and handle any errors by showing a message dialog
        viewModelScope.launch {
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }

        // Load the plane's 3D model asset up front too. This is a separate, independent load
        // from the scene above (it reads Bristol.dae off disk), and switchViewMode() awaits it
        // before moving the camera so a cockpit-view tap before the model has loaded doesn't
        // lock the camera onto a not-yet-known model bounds.
        viewModelScope.launch {
            planeModelSymbol.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }

        // Restrict the camera's heading/pitch to stay behind the plane.
        orbitCameraController.minCameraHeadingOffset = -180.0
        orbitCameraController.maxCameraHeadingOffset = 180.0

        orbitCameraController.minCameraPitchOffset = -90.0
        orbitCameraController.maxCameraPitchOffset = 90.0

        // Restrict the camera to stay within 100 meters of the plane.
        orbitCameraController.maxCameraDistance = 10000.0

        // Position the plane a third from the top of the screen,
        // so it isn't covered by the settings sheet.
        orbitCameraController.targetVerticalScreenFactor = 0.66F

        // Don't pitch the camera when the plane pitches.
        orbitCameraController.isAutoPitchEnabled= false

    }

    // Adjust Camera Heading
    fun setCameraHeading(sliderValue : Float){
        orbitCameraController.setCameraHeadingOffset(sliderValue.toDouble())
        _adaptiveUiState.update { it.copy(heading = sliderValue) }
    }

    // Adjust Plane Pitch
    fun setPlanePitch(sliderValue : Float){
        // Update the plane's PITCH attribute, which rotates the graphic (via the renderer's
        // pitch expression) and, when isAutoPitchEnabled is true, the orbit camera along with it.
        planeGraphic.attributes["PITCH"] = sliderValue.toDouble()
        _adaptiveUiState.update { it.copy(pitch = sliderValue) }
    }


    // Switch View Mode
    fun switchViewMode(viewMode: ViewMode){
        _adaptiveUiState.update { it.copy(viewMode = viewMode) }
        viewModelScope.launch {
            // Wait for both scene and Symbol to load before change view
            arcGISScene.load().onFailure {
                messageDialogVM.showMessageDialog(it)
                return@launch
            }
            planeModelSymbol.load().onFailure {
                messageDialogVM.showMessageDialog(it)
                return@launch
            }
            when (viewMode) {
                ViewMode.CockpitView -> moveToCockpitView()
                ViewMode.CenterView -> moveToCenterView()
            }
        }
    }

    // Shift to Cockpit View
    private suspend fun moveToCockpitView() {
        orbitCameraController.apply {
            isCameraDistanceInteractive = false
            minCameraDistance = 0.0
            maxCameraDistance = 10.0
            minCameraPitchOffset = 90.0
            maxCameraPitchOffset = 90.0
            isAutoPitchEnabled = true
            setCameraDistance(1.0)
            setCameraHeadingOffset(0.0)
            setCameraPitchOffset(90.0)
            setTargetOffsets(x = 0.0, y = -200.0, z = 110.0, duration = 1.0F)
                .onFailure { messageDialogVM.showMessageDialog(it) }
        }
        _adaptiveUiState.update { it.copy(heading = 0F, allowCameraDistanceInteraction = false) }
    }

    // Shift to center view
    private suspend fun moveToCenterView() {
        orbitCameraController.apply {
            minCameraPitchOffset = -90.0
            maxCameraPitchOffset = 90.0
            maxCameraDistance = 10000.0
            minCameraDistance = 0.0
            isAutoPitchEnabled = false
            isCameraDistanceInteractive = _adaptiveUiState.value.allowCameraDistanceInteraction
            setCameraDistance(1000.0)
            setCameraHeadingOffset(0.0)
            setTargetOffsets(x = 0.0, y = 0.0, z = 20.0, duration = 1.0F)
                .onFailure { messageDialogVM.showMessageDialog(it) }
        }
        _adaptiveUiState.update { it.copy(heading = 0F) }
    }

    // Toggle Interaction mode
    fun setInteraction(value : Boolean){
        orbitCameraController.isCameraDistanceInteractive = value
        _adaptiveUiState.update { it.copy(allowCameraDistanceInteraction = value) }
    }
}


// Data Class for Current UI state
data class AdaptiveUiState(
    val heading : Float,
    val pitch : Float,
    val viewMode : ViewMode,
    val allowCameraDistanceInteraction : Boolean
) {
    companion object {
        val defaultState = AdaptiveUiState(
            heading = 0F,
            pitch = 0F,
            viewMode = ViewMode.CenterView,
            allowCameraDistanceInteraction = true
        )

    }
}
enum class ViewMode{
    CockpitView, CenterView
}

