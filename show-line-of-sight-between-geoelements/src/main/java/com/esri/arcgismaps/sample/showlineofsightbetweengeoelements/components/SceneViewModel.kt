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
import androidx.core.content.ContextCompat.getString
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.analysis.GeoElementLineOfSight
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Timer
import java.util.TimerTask


class SceneViewModel(private var application: Application) : AndroidViewModel(application) {


    val scene = ArcGISScene(BasemapStyle.ArcGISTopographic)
    val graphicsOverlay = GraphicsOverlay()
    val analysisOverlay = AnalysisOverlay()

    private val _currentZValue = MutableStateFlow(200.0)
    val currentZValue: StateFlow<Double> = _currentZValue.asStateFlow()

    private var waypointsIndex = 0
    private val meters = LinearUnit.meters
    private val degrees = AngularUnit.degrees

    private val wayPoints = listOf(
        Point(-73.984513, 40.748469, SpatialReference.wgs84()),
        Point(-73.985068, 40.747786, SpatialReference.wgs84()),
        Point(-73.983452, 40.747091, SpatialReference.wgs84()),
        Point(-73.982961, 40.747762, SpatialReference.wgs84()),
    )

    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(
            R.string.app_name
        ) + "/"
    }

    // create a graphic of a taxi to be the target
    private val filePath = provisionPath + application.getString(R.string.dolmus_model)

    private val taxiSymbol = ModelSceneSymbol(filePath, 3.0F).apply {
        SceneSymbolAnchorPosition.Bottom
        viewModelScope.launch {
            load()
        }
    }

    private val taxiGraphic = Graphic(wayPoints[0], taxiSymbol).apply {
        attributes["HEADING"] = 0.0
    }


    private val observerGraphic = Graphic(
        geometry = Point(
            x = -73.9853,
            y = 40.7484,
            z = 200.0,
            spatialReference = SpatialReference.wgs84()
        ),
        symbol = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Circle,
            color = Color.red,
            size = 5f
        )
    )

    init {

        val camera = Camera(
            observerGraphic.geometry as Point,
            distance = 700.0,
            roll = -30.0,
            pitch = 45.0,
            heading = 0.0,
        )

        // Add base surface for elevation data
        val surface = Surface().apply {
            elevationSources.add(
                ArcGISTiledElevationSource(
                    uri = getString(
                        application,
                        R.string.elevation_service_url
                    )
                )
            )
        }

        val buildings =
            ArcGISSceneLayer(uri = application.getString(R.string.new_york_buildings_service_url))

        // add the buildings scene to the sceneView
        scene.apply {
            baseSurface = surface
            operationalLayers.add(buildings)
            initialViewpoint = Viewpoint(
                boundingGeometry = observerGraphic.geometry as Point,
                camera = camera
            )
        }

        // Set up a heading expression to handle graphic rotation
        val renderer3D = SimpleRenderer().apply {
            sceneProperties.headingExpression = ("[HEADING]")

        }

        // create a graphics overlay for the graphics
        graphicsOverlay.apply {
            sceneProperties.surfacePlacement = SurfacePlacement.RelativeToScene
            graphicsOverlay.graphics.add(observerGraphic)
            graphicsOverlay.graphics.add(taxiGraphic)
            graphicsOverlay.renderer = renderer3D
        }

        viewModelScope.launch {
            scene.load().onSuccess {

                val lineOfSight = GeoElementLineOfSight(
                    observerGeoElement = observerGraphic,
                    targetGeoElement = taxiGraphic
                )

                analysisOverlay.analyses.add(lineOfSight)
                analysisOverlay.isVisible = true

                //select (highlight) the taxi when the line of sight target visibility changes to visible
                if (lineOfSight.isVisible) {
                    taxiGraphic.isSelected = lineOfSight.isVisible
                }

                val timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        animate()
                    }
                }, 0, 50)

            }


        }

    }

    fun updateHeight(height: Double) {
        val pointBuilder = PointBuilder(observerGraphic.geometry as Point).apply {
            z = height
        }
        observerGraphic.geometry = pointBuilder.toGeometry()
        _currentZValue.value = height
    }

    /**
     * Moves the taxi toward the current waypoint a short distance.
     */
    private fun animate() {
        val waypoint = wayPoints[waypointsIndex]
        // get current location and distance from waypoint
        var location = taxiGraphic.geometry as Point
        val distance = GeometryEngine.distanceGeodeticOrNull(
            location,
            waypoint,
            meters,
            degrees,
            GeodeticCurveType.Geodesic
        )
        // move toward waypoint a short distance
        if (distance != null) {
            location = GeometryEngine.tryMoveGeodetic(
                listOf(location), 1.0, meters, distance.azimuth1, degrees,
                GeodeticCurveType.Geodesic
            )[0]
        }
        taxiGraphic.geometry = location

        // rotate to the waypoint
        if (distance != null) {
            taxiGraphic.attributes["HEADING"] = distance.azimuth1
        }


        // reached waypoint, move to next waypoint
        if (distance != null) {
            if (distance.distance <= 2) {
                waypointsIndex = (waypointsIndex + 1) % wayPoints.size
            }
        }
    }

}
