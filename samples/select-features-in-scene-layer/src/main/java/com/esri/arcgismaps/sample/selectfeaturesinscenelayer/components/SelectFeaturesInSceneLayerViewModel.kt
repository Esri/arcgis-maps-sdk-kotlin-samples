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
import com.arcgismaps.data.Feature
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class SelectFeaturesInSceneLayerViewModel(app: Application) : AndroidViewModel(app) {

    // Create a SceneViewProxy which is passed to the composable SceneView
    val sceneViewProxy = SceneViewProxy()

    // The scene layer that is added on top of the scene.
    private var sceneLayer = ArcGISSceneLayer(BREST_BUILDING_SERVICE)

    // The user's selected feature
    private var selectedFeature: ArcGISFeature? = null


    //Initial Camera object
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
        val elevationSource =
            ArcGISTiledElevationSource(uri = WORLD_ELEVATION_SERVICE_URL)
        val surface = Surface().apply {
            elevationSources.add(elevationSource)
        }
        baseSurface = surface
        initialViewpoint = Viewpoint(boundingGeometry = camera.location, camera = camera)
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the map and handle any errors by showing a message dialog
        viewModelScope.launch {
            scene.load().onFailure { messageDialogVM.showMessageDialog(it) }
            scene.operationalLayers.apply { add(sceneLayer) }
        }
    }

    /**
     * Identifies the tapped screen coordinate in the provided [singleTapConfirmedEvent] and gets
     * the asset at that location.
     */
    fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        viewModelScope.launch {
            sceneViewProxy.identifyLayers(
                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                tolerance = 12.dp,
                returnPopupsOnly = false,
                maximumResults = 1
            ).onSuccess { identifyResultList ->
                val identifiedFeature = identifyResultList.firstOrNull()?.geoElements?.firstOrNull()
                if (identifiedFeature !is ArcGISFeature || selectedFeature != null) {
                    resetSelections()
                }else{
                    identifiedFeature.select()
                }
            }
        }
    }

    /**
     * Selects the [ArcGISFeature]. Deselects other selection
     */
    private fun ArcGISFeature.select() {
        val feature = this@select
        if(selectedFeature != null && selectedFeature != feature) {
            resetSelections()
        }else {
            selectedFeature = feature
            sceneLayer.selectFeature(feature)
        }
    }

    /**
     * Clears the selection on the layer
     */
    private fun resetSelections() {
        if(selectedFeature != null) {
            sceneLayer.clearSelection()
            selectedFeature = null
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
