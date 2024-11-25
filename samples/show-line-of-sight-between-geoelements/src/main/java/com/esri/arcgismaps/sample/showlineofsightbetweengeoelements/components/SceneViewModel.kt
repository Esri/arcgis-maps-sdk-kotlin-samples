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

package com.esri.arcgismaps.sample.showlineofsightbetweengeoelements.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.getString
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.analysis.GeoElementLineOfSight
import com.arcgismaps.analysis.LineOfSightTargetVisibility
import com.arcgismaps.geometry.AngularUnit
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.PointBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.symbology.ModelSceneSymbol
import com.arcgismaps.mapping.symbology.SceneSymbolAnchorPosition
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SurfacePlacement
import com.esri.arcgismaps.sample.showlineofsightbetweengeoelements.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.concurrent.timer

class SceneViewModel(private var application: Application) : AndroidViewModel(application) {

    // Keep track of target visibility status string state.
    var targetVisibilityString by mutableStateOf("")
        private set

    // Set visibility status string in the UI.
    private fun updateTargetVisibilityString(targetVisibility: String) {
        targetVisibilityString = targetVisibility
    }

    // Initialize z to 50 as starting point and emit its state changes
    private val _observerHeight = MutableStateFlow(50.0)
    val observerHeight: StateFlow<Double> = _observerHeight.asStateFlow()

    // Keeps track of wayPoints
    private var waypointsIndex = 0

    // Create waypoints around a block for the taxi to drive to
    private val wayPoints = listOf(
        Point(-73.984513, 40.748469, SpatialReference.wgs84()),
        Point(-73.985068, 40.747786, SpatialReference.wgs84()),
        Point(-73.983452, 40.747091, SpatialReference.wgs84()),
        Point(-73.982961, 40.747762, SpatialReference.wgs84()),
    )

    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(
            R.string.show_line_of_sight_between_geoelements_app_name
        ) + File.separator
    }

    private val filePath = provisionPath + application.getString(R.string.dolmus_model)

    // Create a symbol of a taxi using the model file
    private val taxiSymbol = ModelSceneSymbol(
        uri = filePath,
        scale = 3.0F
    ).apply {
        anchorPosition = SceneSymbolAnchorPosition.Bottom
    }

    // Create a graphic of a taxi to be the target
    private val taxiGraphic = Graphic(
        geometry = wayPoints[0],
        symbol = taxiSymbol
    ).apply {
        attributes["HEADING"] = 0.0
    }

    // Create a graphic near the Empire State Building to be the observer
    private val observerGraphic = Graphic(
        geometry = Point(
            x = -73.9853,
            y = 40.7484,
            z = 50.0,
            spatialReference = SpatialReference.wgs84()
        ),
        symbol = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Circle,
            color = Color.red,
            size = 5f
        )
    )

    // Zoom to show the observer
    private val camera = Camera(
        lookAtPoint = observerGraphic.geometry as Point,
        distance = 700.0,
        roll = 0.0,
        pitch = 45.0,
        heading = -30.0,
    )


    // Define base surface for elevation data
    private val surface = Surface().apply {
        elevationSources.add(
            ArcGISTiledElevationSource(
                uri = getString(
                    application,
                    R.string.elevation_service_url
                )
            )
        )
    }

    // Define a scene layer for the New York buildings
    private val buildings =
        ArcGISSceneLayer(uri = application.getString(R.string.new_york_buildings_service_url))


    // Create a scene and add a basemap to it.
    // Set the surface and buildings in the scene, and define the viewpoint on launch
    val scene = ArcGISScene(BasemapStyle.ArcGISTopographic).apply {
        baseSurface = surface
        operationalLayers.add(buildings)
        initialViewpoint = Viewpoint(
            boundingGeometry = observerGraphic.geometry as Point,
            camera = camera
        )
    }


    // Set up a heading expression to handle graphic rotation
    private val renderer3D = SimpleRenderer().apply {
        sceneProperties.headingExpression = ("[HEADING]")
    }


    // Create graphic overlay to hold graphics
    // Set the surface placement, renderer, and add graphics,
    val graphicsOverlay = GraphicsOverlay().apply {
        sceneProperties.surfacePlacement = SurfacePlacement.RelativeToScene
        renderer = renderer3D
        graphics.addAll(listOf(observerGraphic, taxiGraphic))
    }


    // Create a line of sight between the two graphics and add it to the analysis overlay
    private val lineOfSight = GeoElementLineOfSight(
        observerGeoElement = observerGraphic,
        targetGeoElement = taxiGraphic
    ).apply {
        // Observe the visibility status of the moving taxi
        viewModelScope.launch(Dispatchers.Main) {

            // Update target visibility status and select (highlight) the taxi when the line of sight target visibility changes to visible
            targetVisibility.collect { targetVisibility ->
                when(targetVisibility) {
                    is LineOfSightTargetVisibility.Visible -> {
                        updateTargetVisibilityString("Visible")
                        taxiGraphic.isSelected = true
                    }
                    is LineOfSightTargetVisibility.Obstructed -> {
                        updateTargetVisibilityString("Obstructed")
                        taxiGraphic.isSelected = false
                    }
                    is LineOfSightTargetVisibility.Unknown -> {
                        updateTargetVisibilityString("Unknown")
                        taxiGraphic.isSelected = false
                    }
                }
            }
        }
    }

    // Create an analysis overlay to hold the line of sight
    val analysisOverlay = AnalysisOverlay().apply {
        analyses.add(lineOfSight)
    }

    init {

        // Create a timer to animate the tank
        timer(
            initialDelay = 0,
            period = 50,
            action = {
                animate()
            }
        )
    }

    /**
     * Updates elevation of the observer graphic using the given [height]
     */
    fun updateHeight(height: Double) {
        val pointBuilder = PointBuilder(observerGraphic.geometry as Point).apply {
            z = height
        }
        observerGraphic.geometry = pointBuilder.toGeometry()
        _observerHeight.value = height
    }

    /**
     * Moves the taxi toward the current waypoint a short distance.
     */
    private fun animate() {

        val meters = LinearUnit.meters
        val degrees = AngularUnit.degrees
        val waypoint = wayPoints[waypointsIndex]
        val location = taxiGraphic.geometry as Point

        // Calculate the geodetic distance between current taxi location and next waypoint
        GeometryEngine.distanceGeodeticOrNull(
            point1 = location,
            point2 = waypoint,
            distanceUnit = meters,
            azimuthUnit = degrees,
            curveType = GeodeticCurveType.Geodesic
        )?.let { geodeticDistanceResult ->

            taxiGraphic.apply {

                // Move toward waypoint a short distance
                geometry = GeometryEngine.tryMoveGeodetic(
                    pointCollection = listOf(location),
                    distance = 1.0,
                    distanceUnit = meters,
                    azimuth = geodeticDistanceResult.azimuth1,
                    azimuthUnit = degrees,
                    curveType = GeodeticCurveType.Geodesic
                )[0]

                // Rotate to the waypoint
                attributes["HEADING"] = geodeticDistanceResult.azimuth1

                // Reached waypoint, move to next waypoint
                if (geodeticDistanceResult.distance <= 2) {
                    waypointsIndex = (waypointsIndex + 1) % wayPoints.size
                }
            }
        }
    }

}
