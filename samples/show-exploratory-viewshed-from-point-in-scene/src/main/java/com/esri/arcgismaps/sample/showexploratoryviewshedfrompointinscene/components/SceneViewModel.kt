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

package com.esri.arcgismaps.sample.showexploratoryviewshedfrompointinscene.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.analysis.interactive.ExploratoryLocationViewshed
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.showexploratoryviewshedfrompointinscene.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class SceneViewModel(private val application: Application) : AndroidViewModel(application) {

    // initialize location viewshed parameters
    private var viewShed: ExploratoryLocationViewshed
    private val initHeading = 82.0
    private val initPitch = 60.0
    private val initHorizontalAngle = 75.0
    private val initVerticalAngle = 90.0
    private val initMinDistance = 0.0
    private val initMaxDistance = 1500.0
    private val initFrustumVisible = true
    private val initAnalysisVisible = true

    private val initViewshedUiState = ViewshedUiState(
        heading = initHeading.toFloat(),
        pitch = initPitch.toFloat(),
        horizontalAngle = initHorizontalAngle.toFloat(),
        verticalAngle = initVerticalAngle.toFloat(),
        minDistance = initMinDistance.toFloat(),
        maxDistance = initMaxDistance.toFloat(),
        isFrustumVisible = initFrustumVisible,
        isAnalysisVisible = initAnalysisVisible
    )

    val initLocation = Point(
        x = -4.50,
        y = 48.4,
        z = 1000.0
    )

    private val initialCamera = Camera(
        lookAtPoint = initLocation,
        distance = 3500.0,
        heading = 50.0,
        pitch = 70.0,
        roll = 0.0
    )
    var scene by mutableStateOf(ArcGISScene(BasemapStyle.ArcGISNavigationNight))
    var analysisOverlay by mutableStateOf(AnalysisOverlay())

    private val _viewshedUiState = MutableStateFlow(initViewshedUiState)
    val viewshedUiState = _viewshedUiState.asStateFlow()

    val sceneViewProxy = SceneViewProxy()


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

        // create viewshed from the initial location
        viewShed = ExploratoryLocationViewshed(
            location = initLocation,
            heading = initHeading,
            pitch = initPitch,
            horizontalAngle = initHorizontalAngle,
            verticalAngle = initVerticalAngle,
            minDistance = initMinDistance,
            maxDistance = initMaxDistance
        ).apply {
            frustumOutlineVisible = initFrustumVisible
        }

        // add the buildings scene to the sceneView
        scene = buildingsScene.apply {
            baseSurface = surface
            initialViewpoint = Viewpoint(initLocation, initialCamera)
        }
        // add the viewshed to the analysisOverlay of the  scene view
        analysisOverlay.apply {
            analyses.add(viewShed)
            isVisible = initAnalysisVisible
        }
    }

    fun setHeading(sliderHeading: Float) {
        viewShed.heading = sliderHeading.toDouble()
        _viewshedUiState.update { it.copy(heading = sliderHeading) }
    }

    fun setMaximumDistanceSlider(sliderValue: Float) {
        viewShed.maxDistance = sliderValue.toDouble()
        _viewshedUiState.update { it.copy(maxDistance = sliderValue) }
    }

    fun setMinimumDistanceSlider(sliderValue: Float) {
        viewShed.minDistance = sliderValue.toDouble()
        _viewshedUiState.update { it.copy(minDistance = sliderValue) }
    }

    fun setVerticalAngleSlider(sliderValue: Float) {
        viewShed.verticalAngle = sliderValue.toDouble()
        _viewshedUiState.update { it.copy(verticalAngle = sliderValue) }
    }

    fun setHorizontalAngleSlider(sliderValue: Float) {
        viewShed.horizontalAngle = sliderValue.toDouble()
        _viewshedUiState.update { it.copy(horizontalAngle = sliderValue) }
    }

    fun setPitch(sliderValue: Float) {
        viewShed.pitch = sliderValue.toDouble()
        _viewshedUiState.update { it.copy(pitch = sliderValue) }
    }

    fun frustumVisibility(checkedValue: Boolean) {
        viewShed.frustumOutlineVisible = checkedValue
        _viewshedUiState.update { it.copy(isFrustumVisible = checkedValue) }
    }

    fun analysisVisibility(checkedValue: Boolean) {
        viewShed.isVisible = checkedValue
        _viewshedUiState.update { it.copy(isAnalysisVisible = checkedValue) }
    }

    fun resetViewshedOptions() {
        viewShed.apply {
            heading = initHeading
            pitch = initPitch
            horizontalAngle = initHorizontalAngle
            verticalAngle = initVerticalAngle
            minDistance = initMinDistance
            maxDistance = initMaxDistance
            frustumOutlineVisible = initFrustumVisible
            isVisible = initAnalysisVisible
        }
        _viewshedUiState.value = initViewshedUiState
        viewModelScope.launch {
            sceneViewProxy.setViewpointCameraAnimated(
                camera = initialCamera,
                duration = 1.seconds
            )
        }
    }

    /**
     * Animates the camera to a meaningful overview centered on the viewshed analysis location.
     */
    fun setViewpointToAnalysisExtent() {
        viewModelScope.launch {
            sceneViewProxy.setViewpointCameraAnimated(
                camera = Camera(
                    lookAtPoint = viewShed.location,
                    distance = viewShed.maxDistance ?: initMaxDistance,
                    heading = viewShed.heading,
                    pitch = viewShed.pitch,
                    roll = 0.0
                ),
                duration = 2.seconds
            )
        }
    }
}

data class ViewshedUiState(
    val heading: Float,
    val pitch: Float,
    val horizontalAngle: Float,
    val verticalAngle: Float,
    val minDistance: Float,
    val maxDistance: Float,
    val isFrustumVisible: Boolean,
    val isAnalysisVisible: Boolean
)
