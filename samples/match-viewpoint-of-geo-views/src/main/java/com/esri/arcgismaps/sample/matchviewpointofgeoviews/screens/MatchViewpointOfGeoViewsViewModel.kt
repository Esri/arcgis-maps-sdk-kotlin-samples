package com.esri.arcgismaps.sample.matchviewpointofgeoviews.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy

class MatchViewpointOfGeoViewsViewModel : ViewModel() {
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISImagery)
    val arcGISScene = ArcGISScene(BasemapStyle.ArcGISImagery)
    val mapViewProxy = MapViewProxy()
    val sceneViewProxy = SceneViewProxy()

    // Initial viewpoint
    private val initialViewpoint = Viewpoint(
        center = Point(-13637000.0, 4550000.0, SpatialReference.webMercator()),
        scale = 100_000.0
    )

    // Track navigation state
    var isMapNavigating by mutableStateOf(false)
    var isSceneNavigating by mutableStateOf(false)

    init {
        arcGISMap.initialViewpoint = initialViewpoint
        arcGISScene.initialViewpoint = initialViewpoint
    }

    fun updateMapIsNavigating(navigating: Boolean) {
        isMapNavigating = navigating
    }

    fun updateSceneIsNavigating(navigating: Boolean) {
        isSceneNavigating = navigating
    }

    fun onMapViewpointChanged(newViewpoint: Viewpoint) {
        if (!isSceneNavigating) {
            sceneViewProxy.setViewpoint(newViewpoint)
        }
    }

    fun onSceneViewpointChanged(newViewpoint: Viewpoint) {
        if (!isMapNavigating) {
            mapViewProxy.setViewpoint(newViewpoint)
        }
    }
}
