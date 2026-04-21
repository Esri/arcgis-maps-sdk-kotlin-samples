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

package com.esri.arcgismaps.sample.setfeaturelayerrenderingmodeonscene.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.FeatureRenderingMode
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel for the SetFeatureLayerRenderingModeOnScene sample.
 *
 * Builds two scenes: one where feature layers are rendered statically and
 * another where they are rendered dynamically. Exposes Scene objects and
 * SceneViewProxy instances so the Compose UI can render SceneViews and
 * interact with them.
 */
class SetFeatureLayerRenderingModeOnSceneViewModel(application: Application) : AndroidViewModel(application) {

    // URLs for the sample feature service tables (point, polyline, polygon)
    private val pointLayerUrl =
        "https://sampleserver6.arcgisonline.com/arcgis/rest/services/Energy/Geology/FeatureServer/0"
    private val polylineLayerUrl =
        "https://sampleserver6.arcgisonline.com/arcgis/rest/services/Energy/Geology/FeatureServer/8"
    private val polygonLayerUrl =
        "https://sampleserver6.arcgisonline.com/arcgis/rest/services/Energy/Geology/FeatureServer/9"

    // Scenes exposed to the UI
    var staticScene: ArcGISScene = ArcGISScene().apply {
        initialViewpoint = Viewpoint(
            center = Point(-118.37, 34.46, SpatialReference.wgs84()),
            scale = 30000.0
        )
    }

    var dynamicScene: ArcGISScene = ArcGISScene().apply {
        initialViewpoint = Viewpoint(
            center = Point(-118.37, 34.46, SpatialReference.wgs84()),
            scale = 30000.0
        )
    }

    // SceneViewProxy instances used to control each SceneView from the ViewModel
    val staticSceneViewProxy = SceneViewProxy()
    val dynamicSceneViewProxy = SceneViewProxy()

    var isZoomedIn by mutableStateOf(true)
        private set

    // Message dialog helper to present errors to the user
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Build the scenes and load them.
        viewModelScope.launch {
            try {
                buildScenes()
                // Load both scenes; report any failures to the message dialog VM
                staticScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
                dynamicScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
            } catch (ex: Exception) {
                messageDialogVM.showMessageDialog(ex)
            }
        }
    }

    /**
     * Constructs two scenes. Each scene gets the same set of feature tables but the
     * rendering mode for the layers is set differently: static vs dynamic.
     */
    private fun buildScenes() {
        // Create feature layers from the tables
        val staticPointLayer = FeatureLayer.createWithFeatureTable(ServiceFeatureTable(uri = pointLayerUrl)).apply {
            renderingMode = FeatureRenderingMode.Static
        }
        val staticPolylineLayer = FeatureLayer.createWithFeatureTable(ServiceFeatureTable(uri = polylineLayerUrl)).apply {
            renderingMode = FeatureRenderingMode.Static
        }
        val staticPolygonLayer = FeatureLayer.createWithFeatureTable(ServiceFeatureTable(uri = polygonLayerUrl)).apply {
            renderingMode = FeatureRenderingMode.Static
        }

        val dynamicPointLayer = FeatureLayer.createWithFeatureTable(ServiceFeatureTable(uri = pointLayerUrl)).apply {
            // Set rendering mode to Dynamic for the dynamic scene layers
            renderingMode = FeatureRenderingMode.Dynamic
        }
        val dynamicPolylineLayer = FeatureLayer.createWithFeatureTable(ServiceFeatureTable(uri = polylineLayerUrl)).apply {
            renderingMode = FeatureRenderingMode.Dynamic
        }
        val dynamicPolygonLayer = FeatureLayer.createWithFeatureTable(ServiceFeatureTable(uri = polygonLayerUrl)).apply {
            renderingMode = FeatureRenderingMode.Dynamic
        }

        // Create scenes and add layers
        staticScene.operationalLayers.addAll(listOf(staticPolygonLayer, staticPolylineLayer, staticPointLayer))

        dynamicScene.operationalLayers.addAll(listOf(dynamicPolygonLayer, dynamicPolylineLayer, dynamicPointLayer))
    }

    /**
     * Toggle between zoomed-in and zoomed-out cameras for both scenes.
     */
    fun toggleZoom() {

        val zoomedIn = isZoomedIn
        val staticPoint: Point
        val dynamicPoint: Point
        val distance: Double
        val heading: Double
        val pitch: Double

        if (!zoomedIn) {
            // Zoom in
            staticPoint = Point(-118.45, 34.395, SpatialReference.wgs84())
            dynamicPoint = Point(-118.45, 34.395, SpatialReference.wgs84())
            distance = 2500.0
            heading = 90.0
            pitch = 75.0
        } else {
            // Zoom out
            staticPoint = Point(-118.37, 34.46, SpatialReference.wgs84())
            dynamicPoint = Point(-118.37, 34.46, SpatialReference.wgs84())
            distance = 30000.0
            heading = 0.0
            pitch = 0.0
        }

        val staticCamera = Camera(staticPoint, distance, heading, pitch, 0.0)
        val dynamicCamera = Camera(dynamicPoint, distance, heading, pitch, 0.0)
        viewModelScope.launch {
            staticSceneViewProxy.setViewpointCameraAnimated(staticCamera,5.seconds)
        }
        viewModelScope.launch {
            dynamicSceneViewProxy.setViewpointCameraAnimated(dynamicCamera, 5.seconds)
        }
        isZoomedIn = !isZoomedIn
    }
}
