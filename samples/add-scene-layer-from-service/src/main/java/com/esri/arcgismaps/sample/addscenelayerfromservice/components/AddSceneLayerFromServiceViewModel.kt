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

package com.esri.arcgismaps.sample.addscenelayerfromservice.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.view.Camera
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class AddSceneLayerFromServiceViewModel(app: Application) : AndroidViewModel(app) {
    // URL of the Portland buildings scene layer
    private val portlandBuildingsSceneLayerUrl =
        "https://tiles.arcgis.com/tiles/P3ePLMYs2RVChkJx/arcgis/rest/services/Buildings_Portland/SceneServer"
    // URL of the world elevation tiled elevation source
    private val worldElevationServiceUrl =
        "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"

    // Create the ArcGISScene with imagery basemap
    val arcGISScene by mutableStateOf(
        ArcGISScene(BasemapStyle.ArcGISImagery).apply {
            // Add the Portland buildings scene layer
            operationalLayers.add(
                ArcGISSceneLayer(portlandBuildingsSceneLayerUrl)
            )
            // Add the elevation source to the base surface
            baseSurface = Surface().apply {
                elevationSources.add(ArcGISTiledElevationSource(worldElevationServiceUrl))
            }
            // Set the initial viewpoint to match the Swift sample
            val cameraLocation = Point(
                x = -122.66949,
                y = 45.51869,
                z = 227.0,
                spatialReference = SpatialReference.wgs84()
            )
            val camera = Camera(
                locationPoint = cameraLocation,
                heading = 219.0,
                pitch = 82.0,
                roll = 0.0
            )
            initialViewpoint = Viewpoint(cameraLocation, camera)
        }
    )

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }
}
