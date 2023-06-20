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
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.view.Camera
import com.esri.arcgismaps.sample.addscenelayerwithelevation.R

class SceneViewModel(private val application: Application) : AndroidViewModel(application) {
    // set the SceneView state
    val sceneViewState = SceneViewState()

    fun addSceneLayer() {
        // create a scene layer from a scene service for viewing buildings
        var sceneLayer =  ArcGISSceneLayer(application.getString(R.string.brest_buildings))
        // add the scene layer to the scene's operational layer
        sceneViewState.arcGISScene.operationalLayers.add(sceneLayer)
    }
}

/**
 * Data class that represents the SceneView state
 */
class SceneViewState(
    var arcGISScene: ArcGISScene = ArcGISScene(BasemapStyle.ArcGISTopographic),
    var camera: Camera = Camera(48.378, -4.494, 200.0, 345.0, 65.0, 0.0)
)
