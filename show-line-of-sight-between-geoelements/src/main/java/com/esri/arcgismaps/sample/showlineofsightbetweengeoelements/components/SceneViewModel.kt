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
import com.arcgismaps.geometry.AngularUnit
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.Point
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
import com.arcgismaps.mapping.view.LayerSceneProperties
import com.arcgismaps.mapping.view.SurfacePlacement
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.showlineofsightbetweengeoelements.R
import kotlinx.coroutines.launch
import java.io.File


class SceneViewModel(private var application: Application) : AndroidViewModel(application) {


    private val meters = LinearUnit.meters
    private val degrees = AngularUnit.degrees
    var analysisOverlay by mutableStateOf(AnalysisOverlay())
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()
    private var waypointsIndex = 0
    var scene by mutableStateOf(ArcGISScene(BasemapStyle.ArcGISTopographic))

    // create waypoints around a block for the taxi to drive to
    private val wayPoints = listOf(
        Point(-73.984513, 40.748469, SpatialReference.wgs84()),
        Point(-73.985068, 40.747786, SpatialReference.wgs84()),
        Point(-73.983452, 40.747091, SpatialReference.wgs84()),
        Point(-73.982961, 40.747762, SpatialReference.wgs84()),
    )

    // Create a point graph near the Empire State Building to be the observer
    private val observationPoint = Point(-73.9853, 40.7484, 200.0, SpatialReference.wgs84())
    private val observer = Graphic(
        geometry = observationPoint,
        symbol = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Circle,
            color = Color.red,
            size = 5F
        )
    )

    // create a graphic of a taxi to be the target
    private val filePath = application.cacheDir.toString() + File.separator + getString(
        application,
        R.string.dolmus_model
    )
    private val taxiSymbol = ModelSceneSymbol(filePath, 1.0F).apply {
        SceneSymbolAnchorPosition.Bottom
        viewModelScope.launch {
            load()
        }
    }
    private val taxiGraphic = Graphic(wayPoints[0], taxiSymbol).apply {
        attributes["HEADING"] = 0.0
    }


    private val camera = Camera(
        observer.geometry as Point,
        distance = 700.0,
        roll = -30.0,
        pitch = 45.0,
        heading = 0.0,
    )

    init {

        // Add base surface for elevation data
        val surface = Surface().apply {
            elevationSources.add(
                ArcGISTiledElevationSource(
                    getString(
                        application,
                        R.string.elevation_service_url
                    )
                )
            )
        }

        // Add buildings from New York City
        val buildingsURL = (application.getString(R.string.new_york_buildings_service_url))

        // Create a scene and add imagery basemap, elevation surface, and buildings layer to it
        val buildings = ArcGISSceneLayer(buildingsURL)

        // Set up a heading expression to handle graphic rotation
        val renderer3D = SimpleRenderer().apply {
            sceneProperties.headingExpression = ("[HEADING]")
        }

        // add the buildings scene to the sceneView
        scene.apply {
            baseSurface = surface
            operationalLayers.add(buildings)
            initialViewpoint = Viewpoint(observationPoint, camera)
        }

        getGraphicsOverlay().renderer = renderer3D
        getGraphicsOverlay().graphics.add(observer)
        getGraphicsOverlay().graphics.add(taxiGraphic)

        val lineOfSight = GeoElementLineOfSight(
            observerGeoElement = observer,
            targetGeoElement = taxiGraphic
        )

        analysisOverlay.analyses.add(lineOfSight)
        analysisOverlay.isVisible = true

        // select (highlight) the taxi when the line of sight target visibility changes to visible
//        if(lineOfSight.isVisible){
//            taxiGraphic.isSelected = lineOfSight.isVisible
//        }
//
//        val timer = Timer()
//        // create a timer to animate the tank
//        timer.schedule(object : TimerTask() {
//            override fun run() {
//           //     animate()
//            }
//        }, 0, 50)
    }


    fun getGraphicsOverlay(): GraphicsOverlay {
        // Create a graphics overlay for the graphics
        val graphicsOverlay = GraphicsOverlay()
        graphicsOverlay.sceneProperties.surfacePlacement.apply {
            LayerSceneProperties(SurfacePlacement.Relative)
        }
        return graphicsOverlay
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
        // taxiGraphic.geometry = location

        // rotate to the waypoint
        // mTaxiGraphic.getAttributes().put("HEADING", distance.azimuth1)


        // reached waypoint, move to next waypoint
        if (distance != null) {
            if (distance.distance <= 2) {
                waypointsIndex = (waypointsIndex + 1) % wayPoints.size
            }
        }
    }

}
