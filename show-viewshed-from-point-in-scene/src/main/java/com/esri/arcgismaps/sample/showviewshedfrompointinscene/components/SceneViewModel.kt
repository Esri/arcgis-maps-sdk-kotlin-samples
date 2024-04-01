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
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.OrbitLocationCameraController
import com.esri.arcgismaps.sample.showviewshedfrompointinscene.R

class SceneViewModel(private val application: Application) : AndroidViewModel(application) {

    private var viewShed: LocationViewshed

    // initialize location viewshed parameters
    private val initHeading = 82.0
    private val initPitch = 60.0
    private val initHorizontalAngle = 75.0
    private val initVerticalAngle = 90.0
    private val initMinDistance = 0.0
    private val initMaxDistance = 1500.0

    private val initLocation = Point(
        x = -4.50,
        y = 48.4,
        z = 1000.0
    )
    val camera = Camera(
        lookAtPoint = initLocation,
        distance = 20000000.0,
        heading = 0.0,
        pitch = 55.0,
        roll = 0.0
    )
    val cameraController = OrbitLocationCameraController(
        targetPoint = initLocation,
        distance = 5000.0
    )
    var scene by mutableStateOf(ArcGISScene(BasemapStyle.ArcGISNavigationNight))
    var analysisOverlay by mutableStateOf(AnalysisOverlay())


    init {
        // create a surface for elevation data
        val surface = Surface().apply {
            elevationSources.add(ArcGISTiledElevationSource(application.getString(R.string.elevation_service)))
        }

        // create a layer of buildings
        val buildingsSceneLayer = ArcGISSceneLayer(application.getString(R.string.buildings_layer))

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

        // add the buildings scene to the sceneView
        scene = buildingsScene.apply {
            baseSurface = surface
            initialViewpoint = Viewpoint(initLocation, camera)
        }
        // add the viewshed to the analysisOverlay of the  scene view
        analysisOverlay.apply {
            analyses.add(viewShed)
            isVisible = true
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

