/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.add3dtileslayer.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.Ogc3DTilesLayer
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import kotlinx.coroutines.launch

class SceneViewModel(application: Application) : AndroidViewModel(application) {

    // Create a scene with a dark gray basemap. The scene is added to the composable SceneView in
    // MainScreen.kt.
    val scene = ArcGISScene(BasemapStyle.ArcGISDarkGray).apply {

        // Add an elevation source to the scene's base surface.
        val arcGISTiledElevationSource = ArcGISTiledElevationSource(
            "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
        )
        baseSurface.elevationSources.add(arcGISTiledElevationSource)

        // Add a 3D tiles layer to the scene. The URL points to a 3D tiles layer that shows
        // buildings in Stuttgart, Germany.
        val ogc3DTilesLayer = Ogc3DTilesLayer(
            "https://tiles.arcgis.com/tiles/ZQgQTuoyBrtmoGdP/arcgis/rest/services/Stuttgart/3DTilesServer/tileset.json"
        )
        operationalLayers.add(ogc3DTilesLayer)
    }

    val sceneViewProxy = SceneViewProxy().apply {
        viewModelScope.launch {
            // Wait for the scene to load before setting the viewpoint camera.
            scene.loadStatus.collect { loadStatus ->
                if (loadStatus == LoadStatus.Loaded) {
                    setViewpointCamera(
                        Camera(
                            latitude = 48.84553,
                            longitude = 9.16275,
                            altitude = 450.0,
                            heading = 0.0,
                            pitch = 60.0,
                            roll = 0.0
                        )
                    )
                }
            }
        }
    }
}
