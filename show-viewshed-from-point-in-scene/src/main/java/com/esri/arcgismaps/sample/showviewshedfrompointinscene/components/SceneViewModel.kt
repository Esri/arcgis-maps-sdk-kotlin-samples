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

package com.esri.arcgismaps.sample.showviewshedfrompointinscene.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.analysis.LocationViewshed
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.OrbitLocationCameraController
import com.esri.arcgismaps.sample.showviewshedfrompointinscene.R

class SceneViewModel(private val application: Application) : AndroidViewModel(application) {
    // set the SceneView mutable stateflow
    val sceneViewState = SceneViewState()

    private var viewShed: LocationViewshed

    // initialize location viewshed parameters
    private val initHeading = 82.0
    private val initPitch = 60.0
    private val initHorizontalAngle = 75.0
    private val initVerticalAngle = 90.0
    private val initMinDistance = 0.0
    private val initMaxDistance = 1500.0

    init {
        // create a surface for elevation data
        val surface = Surface().apply {
            elevationSources.add(ArcGISTiledElevationSource(application.getString(R.string.elevation_service)))
        }

        // create a layer of buildings
        val buildingsSceneLayer =
            ArcGISSceneLayer(application.getString(R.string.buildings_layer))

        // create a scene and add imagery basemap, elevation surface, and buildings layer to it
        val buildingsScene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
            baseSurface = surface
            operationalLayers.add(buildingsSceneLayer)
        }

        val initLocation = Point(-4.50, 48.4, 1000.0)
        // create viewshed from the initial location
        viewShed = LocationViewshed(
            location = initLocation,
            heading = initHeading,
            pitch = initPitch,
            horizontalAngle = initHorizontalAngle,
            verticalAngle = initVerticalAngle,
            minDistance = initMinDistance,
            maxDistance = initMaxDistance
        ).apply {
            frustumOutlineVisible = true
        }

        sceneViewState.apply {
            // add the buildings scene to the sceneView
            arcGISScene = buildingsScene
            // add the viewshed to the analysisOverlay of the  scene view
            analysisOverlay.apply {
                analyses.add(viewShed)
                isVisible = true
            }
        }
    }

    fun setHeading(sliderHeading: Float) {
        viewShed.heading = sliderHeading.toDouble()
    }

    fun setMaximumDistanceSlider(sliderValue: Float) {
        viewShed.maxDistance = sliderValue.toDouble()

    }

    fun setMinimumDistanceSlider(sliderValue: Float) {
        viewShed.minDistance = sliderValue.toDouble()
    }

    fun setVerticalAngleSlider(sliderValue: Float) {
        viewShed.verticalAngle = sliderValue.toDouble()
    }

    fun setHorizontalAngleSlider(sliderValue: Float) {
        viewShed.horizontalAngle = sliderValue.toDouble()
    }

    fun setPitch(sliderValue: Float) {
        viewShed.pitch = sliderValue.toDouble()
    }

    fun frustumVisibility(checkedValue: Boolean) {
        viewShed.frustumOutlineVisible = checkedValue
    }

    fun analysisVisibility(checkedValue: Boolean) {
        viewShed.isVisible = checkedValue
    }
}

/**
 * Data class that represents the SceneView state
 */
class SceneViewState {
    var arcGISScene: ArcGISScene by mutableStateOf(ArcGISScene(BasemapStyle.ArcGISNavigationNight))
    private val initLocation = Point(-4.50, 48.4, 1000.0)
    val camera: Camera = Camera(initLocation, 20000000.0, 0.0, 55.0, 0.0)
    val cameraController: OrbitLocationCameraController =
        OrbitLocationCameraController(initLocation, 5000.0)
    var analysisOverlay: AnalysisOverlay by mutableStateOf(AnalysisOverlay())
}

