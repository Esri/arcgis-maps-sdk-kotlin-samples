/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.augmentrealitytonavigateroute.components

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.AngularUnit
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeodeticDistanceResult
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.RouteTrackerLocationDataSource
import com.arcgismaps.location.SystemLocationDataSource
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.ElevationSource
import com.arcgismaps.mapping.symbology.ModelSceneSymbol
import com.arcgismaps.mapping.symbology.SceneSymbolAnchorPosition
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SurfacePlacement
import com.arcgismaps.navigation.RouteTracker
import com.arcgismaps.tasks.networkanalysis.DirectionManeuverType
import com.arcgismaps.tasks.networkanalysis.Route
import com.arcgismaps.tasks.networkanalysis.RouteResult
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewProxy
import com.arcgismaps.toolkit.ar.WorldScaleVpsAvailability
import com.esri.arcgismaps.sample.augmentrealitytonavigateroute.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Math.toDegrees
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.atan2

class AugmentedRealityViewModel(app: Application) : AndroidViewModel(app) {

    val worldScaleSceneViewProxy = WorldScaleSceneViewProxy()

    var isVpsAvailable by mutableStateOf(false)

    // Path to the model file
    val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() + File.separator + app.getString(
            R.string.augment_reality_to_navigate_route_app_name
        ) + File.separator
    }

    // Create a symbol of a taxi using the model file
    val arrowSymbol = ModelSceneSymbol(
        uri = provisionPath + "arrow.FBX",
        scale = 10F,
    ).apply {
        anchorPosition = SceneSymbolAnchorPosition.Bottom
    }

    // Boolean to check if Android text-speech is initialized
    private var isTextToSpeechInitialized = AtomicBoolean(false)

    // Instance of Android text-speech
    private var textToSpeech: TextToSpeech? = null

    // Mutable variables used in the UI
    var currentGraphicsShown by mutableIntStateOf(5)
    var nextDirectionText: String by mutableStateOf("")

    // Create a scene with an elevation source and grid and surface hidden
    val arcGISScene = ArcGISScene(BasemapStyle.ArcGISHumanGeography).apply {
        baseSurface.elevationSources.add(ElevationSource.fromTerrain3dService())
        baseSurface.backgroundGrid.isVisible = false
        baseSurface.opacity = 0.0f // hide the background
    }

    // Route result passed from the route view model via the repository
    val routeResult = SharedRepository.route

    // Graphics overlay for the route ahead
    val routeAheadGraphicsOverlay = GraphicsOverlay().apply {
        sceneProperties.surfacePlacement = SurfacePlacement.Absolute
    }

    // Graphics overlay for the route behind
    val routeBehindGraphicsOverlay = GraphicsOverlay().apply {
        sceneProperties.surfacePlacement = SurfacePlacement.Absolute
        opacity = 0.5f
    }

    // List of all graphics, used to find the closest graphic on location changes
    val routeAllGraphics: MutableList<Graphic> = mutableListOf()

    // The current closest graphic
    var currentClosestGraphic: Graphic? = null

    init {
        routeResult?.let { routeResult ->
            viewModelScope.launch {
                drawRoute(routeResult.routes.first())
            }
            setupRouteTracker(routeResult, app)
        }
    }

    /**
     * Draws route graphics in augmented reality with turn arrows stood up like a billboard and other arrows lying flat.
     */
    suspend fun drawRoute(route: Route) {
        // Loop through all the direction maneuvers and draw the route
        route.directionManeuvers.forEachIndexed { i, maneuver ->
            // If the maneuver is a stop
            if (maneuver.geometry is Point && maneuver.maneuverType == DirectionManeuverType.Stop) {
                val thisPoint = addZValuesGeometry(maneuver.geometry as Point) as Point
                // Check there are enough direction maneuvers to get the previous point
                if (route.directionManeuvers.size > 1) {
                    // Get the second to last point of the previous maneuver. The last point is coincident with the stop.
                    val previousPoint =
                        (route.directionManeuvers[i - 1].geometry as? Polyline)?.parts?.last()?.points?.toList()
                            ?.takeLast(2)?.first()
                    previousPoint?.let { previousPoint ->
                        val distanceInformation = GeometryEngine.distanceGeodeticOrNull(
                            point1 = thisPoint,
                            point2 = previousPoint,
                            distanceUnit = LinearUnit.meters,
                            azimuthUnit = AngularUnit.degrees,
                            curveType = GeodeticCurveType.Geodesic
                        )
                        val headingToPreviousPoint = calculateHeading(distanceInformation)
                        drawArrow(
                            position = thisPoint,
                            heading = headingToPreviousPoint,
                            pitch = -90f,
                            roll = 90f,
                            animate = true
                        )
                    }
                }
            } else if (maneuver.geometry is Polyline) {
                val densifiedPolyline = GeometryEngine.densifyGeodeticOrNull(
                    geometry = maneuver.geometry as Polyline,
                    maxSegmentLength = 15.0,
                    lengthUnit = LinearUnit.meters,
                    curveType = GeodeticCurveType.Geodesic
                ) as Polyline
                val polylineWithElevation = addZValuesGeometry(densifiedPolyline) as Polyline
                polylineWithElevation.parts.forEach { part ->
                    var previousPoint = part.points.first()
                    var previousHeading = 0f
                    var isFirstPoint = true
                    part.points.drop(1).forEach { thisPoint ->
                        val distanceInformation = GeometryEngine.distanceGeodeticOrNull(
                            point1 = previousPoint,
                            point2 = thisPoint,
                            distanceUnit = LinearUnit.meters,
                            azimuthUnit = AngularUnit.degrees,
                            curveType = GeodeticCurveType.Geodesic
                        )
                        val headingToNextPoint = calculateHeading(distanceInformation)
                        val pitchToNextPoint = calculatePitch(previousPoint, thisPoint, distanceInformation)
                        // If the first point of a non-straight maneuver or if the heading change is more than 30
                        // degrees, set the roll to 90 degrees and thus stand the graphic up like a billboard.
                        if ((maneuver.maneuverType != DirectionManeuverType.Straight && isFirstPoint) || (abs(
                                previousHeading - headingToNextPoint
                            ) > 30f)
                        ) {
                            drawArrow(
                                position = previousPoint,
                                heading = headingToNextPoint,
                                pitch = pitchToNextPoint,
                                roll = 0f,
                                animate = true
                            )
                        } else {
                            drawArrow(
                                position = previousPoint,
                                heading = headingToNextPoint,
                                pitch = pitchToNextPoint,
                                roll = 90f,
                                animate = false
                            )
                        }

                        isFirstPoint = false
                        previousPoint = thisPoint
                        previousHeading = headingToNextPoint
                    }
                }
            }
            setNumberOfArrowsVisible(currentGraphicsShown)
        }
    }

    /**
     * Draws an arrow at the given position with the specified heading, pitch, and roll and adds it to the graphics
     * overlay and list of graphics used for closest graphic calculations. Will animate the graphic if specified.
     */
    fun drawArrow(position: Point, heading: Float, pitch: Float, roll: Float, animate: Boolean) {
        val arrowGraphic = Graphic(
            geometry = position, symbol = arrowSymbol.apply {
                this.heading = heading
                this.pitch = pitch
                this.roll = roll
            }.clone()
        )
        // Animate arrow if specified
        if (animate) {
            animateModelSceneSymbolScale(arrowGraphic)
        }
        routeAheadGraphicsOverlay.graphics.add(arrowGraphic)
        routeAllGraphics.add(arrowGraphic)
    }

    /**
     * Adds Z values to the geometry by getting the elevation from the base surface.
     */
    suspend fun addZValuesGeometry(geometry: Geometry): Geometry {
        if (geometry is Polyline) {
            val polylineBuilder = PolylineBuilder(SpatialReference.wgs84())
            geometry.parts.forEach { part ->
                part.points.forEach { point ->
                    arcGISScene.baseSurface.elevationSources.first().load().onSuccess {
                        arcGISScene.baseSurface.getElevation(point).let { elevationResult ->
                            elevationResult.getOrNull()?.let { elevation ->
                                polylineBuilder.addPoint(
                                    Point(
                                        x = point.x,
                                        y = point.y,
                                        z = elevation,
                                        spatialReference = SpatialReference.wgs84()
                                    )
                                )
                            }
                        }
                    }
                }

            }
            return polylineBuilder.toGeometry()
        } else {
            var point = geometry as Point
            arcGISScene.baseSurface.elevationSources.first().load().onSuccess {
                arcGISScene.baseSurface.getElevation(point).let { elevationResult ->
                    elevationResult.getOrNull()?.let { elevation ->
                        point = Point(
                            x = point.x, y = point.y, z = elevation, spatialReference = SpatialReference.wgs84()
                        )
                    }
                }
            }
            return point
        }
    }

    /**
     * Calculates the heading from the distance information.
     */
    fun calculateHeading(distanceInformation: GeodeticDistanceResult?): Float {
        return distanceInformation?.azimuth1?.toFloat() ?: 0.0f
    }

    /**
     * Calculates the pitch between two points using the elevation difference and horizontal distance.
     */
    fun calculatePitch(previousPoint: Point, thisPoint: Point, distanceInformation: GeodeticDistanceResult?): Float {
        val elevationDifference = previousPoint.z?.let { thisPoint.z?.minus(it) }
        val horizontalDistance = distanceInformation?.distance ?: 0.0
        return if (elevationDifference != null && horizontalDistance != 0.0) {
            toDegrees(atan2(elevationDifference, horizontalDistance)).toFloat()
        } else {
            0.0f
        }
    }

    /**
     * Gets the closest graphic to the given location by calculating the distance to each graphic and returning the
     * closest one.
     */
    suspend fun getClosestGraphic(location: Point): Graphic? {
        val locationWithZ = addZValuesGeometry(location) as Point
        return routeAllGraphics.minByOrNull { graphic ->
            GeometryEngine.distanceGeodeticOrNull(
                locationWithZ,
                graphic.geometry as Point,
                LinearUnit.meters,
                AngularUnit.degrees,
                GeodeticCurveType.Geodesic
            )?.distance ?: Double.MAX_VALUE
        }
    }

    /**
     * Update current graphics shown based on input from the UI.
     */
    fun onCurrentGraphicsShownChanged(numGraphics: Int) {
        currentGraphicsShown = numGraphics
        setNumberOfArrowsVisible(currentGraphicsShown)
    }

    /**
     * Set the number of arrows visible in the route ahead graphics overlay. Updated from both the UI and when the list
     * of graphics in the route ahead graphics overlay changes.
     */
    fun setNumberOfArrowsVisible(numArrows: Int) {
        routeAheadGraphicsOverlay.graphics.forEachIndexed { index, graphic ->
            graphic.isVisible = index < numArrows
        }
    }

    /**
     * Setup the route tracker to track the route, update graphic visibility and provide voice guidance.
     */
    fun setupRouteTracker(routeResult: RouteResult, app: Application) {

        // Create text-to-speech to replay navigation voice guidance
        textToSpeech = TextToSpeech(app) { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeech?.language = getApplication<Application>().resources.configuration.locales[0]
                isTextToSpeechInitialized.set(true)
            }
        }

        with(viewModelScope) {
            // Set a route tracker
            val routeTracker = RouteTracker(routeResult, 0, true).apply {
                setSpeechEngineReadyCallback {
                    isTextToSpeechInitialized.get() && textToSpeech?.isSpeaking == false
                }
            }.apply {
                launch {
                    // Listen for new voice guidance events
                    newVoiceGuidance.collect { voiceGuidance ->
                        // use Android's text to speech to speak the voice guidance
                        textToSpeech?.speak(voiceGuidance.text, TextToSpeech.QUEUE_FLUSH, null, null)
                        // set next direction text
                        nextDirectionText = voiceGuidance.text
                    }
                }
            }
            // Setup location data sources
            launch {
                // Start a new system location data source
                val systemLocationDataSource = SystemLocationDataSource().also {
                    it.start()
                }
                // Start a route tracker location data source to snap the location display to the route
                RouteTrackerLocationDataSource(
                    routeTracker = routeTracker, locationDataSource = systemLocationDataSource
                ).also {
                    it.start()
                }
                // Collect location changes from the system location data source to update the route tracker
                systemLocationDataSource.locationChanged.collect { location ->
                    routeTracker.trackLocation(location)
                }
            }
            // Collect tracking status changes to update the closest graphic
            launch {
                routeTracker.trackingStatus.collect { trackingStatus ->
                    // Get the current position of the route tracker
                    val currentPosition = trackingStatus?.locationOnRoute?.position
                    // Get the closest graphic to the current position
                    currentPosition?.let { currentClosestGraphic = getClosestGraphic(it) }
                    // Move the closest graphic from the route ahead graphics overlay to the route behind
                    // graphics overlay
                    if (routeAheadGraphicsOverlay.graphics.contains(currentClosestGraphic)) {
                        val closestGraphicIndex =
                            routeAheadGraphicsOverlay.graphics.indexOf(currentClosestGraphic)
                        if (closestGraphicIndex != -1) {
                            // Select all graphics up to the closest graphic
                            val graphicsToMove =
                                routeAheadGraphicsOverlay.graphics.subList(0, closestGraphicIndex).toSet()
                            if (graphicsToMove.isNotEmpty()) {
                                // Move the graphics to the route behind graphics overlay
                                routeAheadGraphicsOverlay.graphics.removeAll(graphicsToMove)
                                routeBehindGraphicsOverlay.graphics.addAll(graphicsToMove)
                                // Update the visibility of the graphics in the route ahead graphics overlay
                                setNumberOfArrowsVisible(currentGraphicsShown)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Animate the model scene symbol scale using a sine wave function.
     */
    fun animateModelSceneSymbolScale(arrowGraphic: Graphic) {
        viewModelScope.launch {
            val animationDuration = 2000L
            val frameRate = 20
            val frameDelay = 1000L / frameRate
            val totalFrames = (animationDuration / frameDelay).toInt()

            val symbol = arrowGraphic.symbol as ModelSceneSymbol

            while (true) {
                for (frame in 0 until totalFrames) {
                    val progress = frame.toFloat() / totalFrames
                    val scaleFactor = 1 + 0.2 * kotlin.math.sin(2 * kotlin.math.PI * progress)
                    symbol.height = 1 * scaleFactor
                    symbol.depth = 2 * scaleFactor
                    delay(frameDelay)
                }
            }
        }
    }

    /**
     * Checks if the current viewpoint camera location is within the VPS availability area.
     */
    fun onCurrentViewpointCameraChanged(location: Point) {
        viewModelScope.launch {
            worldScaleSceneViewProxy.checkVpsAvailability(location.y, location.x).onSuccess {
                isVpsAvailable = it == WorldScaleVpsAvailability.Available
            }
        }
    }
}
