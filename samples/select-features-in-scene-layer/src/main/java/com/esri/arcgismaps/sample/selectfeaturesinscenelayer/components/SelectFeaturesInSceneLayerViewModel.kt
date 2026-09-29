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

package com.esri.arcgismaps.sample.selectfeaturesinscenelayer.components

import android.app.Application
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class SelectFeaturesInSceneLayerViewModel(app: Application) : AndroidViewModel(app) {

    // Create a SceneViewProxy which is passed to the composable SceneView
    val sceneViewProxy = SceneViewProxy()

    // The layer added to the scene's operational layers.
    private val sceneLayer = ArcGISSceneLayer(BREST_BUILDING_SERVICE)

    // Initial Camera object
    val camera = Camera(
        latitude = 48.38282,
        longitude = -4.49779,
        altitude = 40.0,
        heading = 41.65,
        pitch = 71.2,
        roll = 0.0
    )
    // Create a scene to be present on scene view
    val scene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
        // Add base surface for elevation data
        baseSurface = Surface().apply {
            elevationSources.add(ArcGISTiledElevationSource(uri = WORLD_ELEVATION_SERVICE_URL))
        }

        // Add the scene layer with features to select
        operationalLayers.add(sceneLayer)

        // Set the initial viewpoint of the scene
        initialViewpoint = Viewpoint(boundingGeometry = camera.location, camera = camera)
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the map and handle any errors by showing a message dialog
        viewModelScope.launch {
            scene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Identifies the tapped screen coordinate in the provided [tapEvent]
     * and selects the first identified [ArcGISFeature]
     */
    fun identify(tapEvent: SingleTapConfirmedEvent) {

        // Clear any previous selection
        sceneLayer.clearSelection()

        viewModelScope.launch {
            sceneViewProxy.identify(
                layer = sceneLayer,
                screenCoordinate = tapEvent.screenCoordinate,
                tolerance = 12.dp,
            ).onSuccess { identifyResult ->
                identifyResult
                    .geoElements
                    .filterIsInstance<ArcGISFeature>()
                    .firstOrNull()
                    ?.let { arcGISFeature ->
                        // Select the feature on the scene layer
                        sceneLayer.selectFeature(arcGISFeature)
                    }
            }.onFailure {
                messageDialogVM.showMessageDialog(it)
            }
        }
    }

    companion object {
        // The URL of a Brest, France buildings scene service.
        private const val BREST_BUILDING_SERVICE =
            "https://tiles.arcgis.com/tiles/P3ePLMYs2RVChkJx/arcgis/rest/services/Buildings_Brest/SceneServer/layers/0"

        // World elevation service used to provide base surface elevation.
        private const val WORLD_ELEVATION_SERVICE_URL =
            "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
    }

}
