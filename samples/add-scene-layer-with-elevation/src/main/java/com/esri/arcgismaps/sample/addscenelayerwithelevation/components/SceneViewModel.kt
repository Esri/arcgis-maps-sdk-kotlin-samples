/* Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.addscenelayerwithelevation.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.view.Camera
import com.esri.arcgismaps.sample.addscenelayerwithelevation.R

class SceneViewModel(application: Application) : AndroidViewModel(application) {

    private val initLocation = Point(
        x = 48.378,
        y = -4.494
    )

    // create a camera position for the scene
    private val camera: Camera = Camera(
        latitude = 48.378,
        longitude = -4.494,
        altitude = 200.0,
        heading = 345.0,
        pitch = 65.0,
        roll = 0.0
    )
    // the scene used to display on the SceneView
    val scene: ArcGISScene = ArcGISScene(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = Viewpoint(initLocation,camera)
    }

    // add a scene layer to the ArcGISScene
    init {
        // create a scene layer from a scene service for viewing buildings
        val sceneLayer =  ArcGISSceneLayer(application.getString(R.string.brest_buildings))
        // add the scene layer to the scene's operational layer
        scene.operationalLayers.add(sceneLayer)
    }
}

