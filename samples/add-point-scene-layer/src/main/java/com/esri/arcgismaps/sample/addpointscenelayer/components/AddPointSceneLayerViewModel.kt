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

package com.esri.arcgismaps.sample.addpointscenelayer.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class AddPointSceneLayerViewModel(app: Application) : AndroidViewModel(app) {
    // Scene with world airports point scene layer
    val arcGISScene by mutableStateOf(
        ArcGISScene(BasemapStyle.ArcGISImagery).apply {
            // Set initial viewpoint to show the world
            initialViewpoint = Viewpoint(
                Point(
                    x = -98.6, // longitude
                    y = 39.8,  // latitude
                    spatialReference = SpatialReference.wgs84()
                ),
                scale = 1e8
            )
            // Add the airports point scene layer
            val sceneLayer = ArcGISSceneLayer(
                uri = "https://tiles.arcgis.com/tiles/V6ZHFr6zdgNZuVG0/arcgis/rest/services/Airports_PointSceneLayer/SceneServer/layers/0"
            )
            operationalLayers.add(sceneLayer)
            // Add an elevation source to display elevation
            val elevationSource = ArcGISTiledElevationSource(
                uri = "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
            )
            baseSurface = Surface().apply {
                elevationSources.add(elevationSource)
            }
        }
    )

    // Message dialog for error handling
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }
}
