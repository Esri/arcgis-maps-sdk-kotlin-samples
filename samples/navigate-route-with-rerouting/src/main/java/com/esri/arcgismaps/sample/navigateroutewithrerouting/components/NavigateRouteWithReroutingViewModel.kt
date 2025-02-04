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

package com.esri.arcgismaps.sample.navigateroutewithrerouting.components

import android.app.Application
import android.speech.tts.TextToSpeech
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.location.RouteTrackerLocationDataSource
import com.arcgismaps.location.SimulatedLocationDataSource
import com.arcgismaps.location.SimulationParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.navigation.DestinationStatus
import com.arcgismaps.navigation.ReroutingParameters
import com.arcgismaps.navigation.ReroutingStrategy
import com.arcgismaps.navigation.RouteTracker
import com.arcgismaps.navigation.TrackingStatus
import com.arcgismaps.tasks.networkanalysis.DirectionManeuver
import com.arcgismaps.tasks.networkanalysis.Route
import com.arcgismaps.tasks.networkanalysis.RouteParameters
import com.arcgismaps.tasks.networkanalysis.RouteResult
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.navigateroutewithrerouting.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class NavigateRouteWithReroutingViewModel(application: Application) :
    AndroidViewModel(application) {
    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() +
                File.separator + application.getString(R.string.navigate_route_with_rerouting_app_name)
    }

    val map = ArcGISMap(BasemapStyle.ArcGISStreets)

    // graphics overlay to display the route ahead and traveled graphics
    val graphicsOverlay = GraphicsOverlay()

    // the route tracker for navigation. Use delegate methods to update tracking status
    private var routeTracker: RouteTracker? = null

    // generate a route with directions and stops for navigation
    val routeTask = RouteTask(
        pathToDatabase = "$provisionPath/sandiego.geodatabase",
        networkName = "Streets_ND"
    )

    // destination list of stops for the RouteParameters
    private val routeStops = listOf(
        // San Diego Convention Center
        Stop(Point(-117.160386, 32.706608, SpatialReference.wgs84())),
        // Fleet Science Center
        Stop(Point(-117.146679, 32.730351, SpatialReference.wgs84()))
    )

    // a list to keep track of directions solved by the route task
    private val directionManeuvers = mutableListOf<DirectionManeuver>()

    // the calculated route with direction maneuvers and route geometry
    private var route: Route? = null

    // the route parameters needed to calculate a route from a start and stop point
    private var routeParameters: RouteParameters? = null

    // passed to the composable MapView to set the mapViewProxy
    val mapViewProxy = MapViewProxy()

    // keep track of the the location display job when navigation is enabled
    private var locationDisplayJob: Job? = null

    // default location display object, which is updated by rememberLocationDisplay
    private var locationDisplay: LocationDisplay = LocationDisplay()

    // the resulted route from the route task using the route parameters
    private var routeResult: RouteResult? = null

    // instance of the route ahead polyline
    private var routeAheadGraphic: Graphic = Graphic()

    // instance of the route traveled polyline
    private var routeTraveledGraphic: Graphic = Graphic()

    var distanceRemainingText by mutableStateOf("")
        private set

    var timeRemainingText by mutableStateOf("")
        private set

    var nextDirectionText by mutableStateOf("")
        private set

    var nextStopText by mutableStateOf("")
        private set

    var isNavigateButtonEnabled by mutableStateOf(true)
        private set

    var isRecenterButtonEnabled by mutableStateOf(false)
        private set

    // boolean to check if Android text-speech is initialized
    private var isTextToSpeechInitialized = AtomicBoolean(false)

    // instance of Android text-speech
    private var textToSpeech: TextToSpeech? = null

    // the JSON of polylines of the path for the simulated data source
    private val polylineJSON: String by lazy {
        application.getString(R.string.simulation_path_json)
    }

    // create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    init {
        // create text-to-speech to replay navigation voice guidance
        textToSpeech = TextToSpeech(application) { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeech?.language =
                    getApplication<Application>().resources.configuration.locales[0]
                isTextToSpeechInitialized.set(true)
            }
        }

        viewModelScope.launch {
            // load and set the route parameters
            routeParameters = routeTask.createDefaultParameters().getOrElse {
                return@launch messageDialogVM.showMessageDialog(
                    title = "Error creating default parameters",
                    description = it.message.toString()
                )
            }.apply {
                setStops(routeStops)
                returnDirections = true
                returnStops = true
                returnRoutes = true
            }

            // get the solved route result
            routeResult = routeTask.solveRoute(routeParameters!!).getOrElse {
                return@launch messageDialogVM.showMessageDialog(
                    title = "Error solving route",
                    description = it.message.toString()
                )
            }

            // reset navigation to initial state
            resetNavigation()
        }

    }

    /**
     * Start the navigation along the provided route using the [routeResult] and
     * collects updates in the location using the MapView's location display.
     * */
    fun startNavigation() {
        val routeResult = routeResult
            ?: return messageDialogVM.showMessageDialog("Error retrieving route result")
        // get the route's geometry from the route result
        val routeGeometry: Polyline = routeResult.routes[0].routeGeometry
            ?: return messageDialogVM.showMessageDialog("Route is missing geometry")

        // set up a simulated location data source which simulates movement along the route
        val simulationParameters = SimulationParameters(
            Instant.now(),
            velocity = 35.0,
            horizontalAccuracy = 5.0,
            verticalAccuracy = 5.0
        )

        // create the simulated data source using the geometry and parameters
        val simulatedLocationDataSource = SimulatedLocationDataSource(
            polyline = Geometry.fromJsonOrNull(polylineJSON) as Polyline,
            parameters = simulationParameters
        )

        // TODO
        route?.directionManeuvers?.let { directionManeuvers.addAll(it) }

        // set up a RouteTracker for navigation along the calculated route
        routeTracker = RouteTracker(
            routeResult = routeResult,
            routeIndex = 0,
            skipCoincidentStops = true
        ).apply {
            setSpeechEngineReadyCallback {
                isTextToSpeechInitialized.get() && textToSpeech?.isSpeaking == false
            }
        }



        locationDisplayJob = with(viewModelScope) {
            launch {
                // automatically enable recenter button when navigation pan is disabled
                locationDisplay.autoPanMode.filter { it == LocationDisplayAutoPanMode.Off }
                    .collect { isRecenterButtonEnabled = true }
            }

            launch {
                // check if this route task supports rerouting
                if (routeTask.getRouteTaskInfo().supportsRerouting) {
                    // set up the re-routing parameters
                    val reroutingParameters = ReroutingParameters(
                        routeTask = routeTask,
                        routeParameters = routeParameters!!
                    ).apply {
                        strategy = ReroutingStrategy.ToNextWaypoint
                        visitFirstStopOnStart = false
                    }
                    // enable automatic re-routing
                    routeTracker?.enableRerouting(parameters = reroutingParameters)?.onFailure {
                        messageDialogVM.showMessageDialog(
                            title = it.message.toString(),
                            description = it.cause.toString()
                        )
                    }
                }

                // create a route tracker location data source to snap the location display to the route
                val routeTrackerLocationDataSource = RouteTrackerLocationDataSource(
                    routeTracker = routeTracker!!,
                    locationDataSource = simulatedLocationDataSource
                )
                // set the simulated location data source as the location data source for this app
                locationDisplay.dataSource = routeTrackerLocationDataSource

                // start the location data source
                locationDisplay.dataSource.start().getOrElse {
                    messageDialogVM.showMessageDialog(
                        title = "Error starting location data source",
                        description = it.message.toString()
                    )
                }

                // set the auto pan to navigation mode
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)

                // data source has started, display stop message
                nextStopText = getStringArray(R.array.stop_message)[0]

                // plays the direction voice guidance
                updateVoiceGuidance(routeTracker!!)

                launch {
                    // zoom in the scale to focus on the navigation route
                    mapViewProxy.setViewpointScale(10000.0)
                }
            }

            launch {
                // listen for changes in location
                locationDisplay.location.collect {
                    // get the route's tracking status
                    val trackingStatus = routeTracker?.trackingStatus?.value ?: return@collect
                    // displays the remaining and traversed route
                    updateRouteGraphics(trackingStatus)
                    // display route status and directions info
                    displayRouteInfo(routeTracker, trackingStatus)
                    // disable navigation button
                    isNavigateButtonEnabled = false
                }
            }
            launch {
                routeTracker?.rerouteStarted?.collect {
                    Log.e("REROUTE", "Started rerouting")
                }
            }
            launch {
                routeTracker?.rerouteCompleted?.collect {
                    val status = it.getOrNull()
                    Log.e("REROUTE", "Reroute completed: ${status?.javaClass?.simpleName}")
                }
            }
        }
    }

    /**
     * Displays the route distance and time information using [trackingStatus], and
     * switches destinations using [routeTracker]. When final destination is reached,
     * the location data source is stopped.
     */
    private suspend fun displayRouteInfo(
        routeTracker: RouteTracker?,
        trackingStatus: TrackingStatus
    ) {
        // get remaining distance information
        val remainingDistance = trackingStatus.destinationProgress.remainingDistance
        // convert remaining minutes to hours:minutes:seconds
        val remainingTimeString =
            DateUtils.formatElapsedTime((trackingStatus.destinationProgress.remainingTime * 60).toLong())

        // update text views
        timeRemainingText = getString(R.string.time_remaining) + " " + remainingTimeString
        distanceRemainingText = getString(R.string.distance_remaining) + " " +
                remainingDistance.displayText + " " +
                remainingDistance.displayTextUnits.abbreviation

        // if a destination has been reached
        if (trackingStatus.destinationStatus == DestinationStatus.Reached) {
            // if there are more destinations to visit. Greater than 1 because the start point is considered a "stop"
            if (trackingStatus.remainingDestinationCount > 1) {
                // switch to the next destination
                routeTracker?.switchToNextDestination()?.getOrElse {
                    return messageDialogVM.showMessageDialog(
                        title = "Error retrieving next destination",
                        description = it.message.toString()
                    )
                }
                // set second stop message
                nextStopText = getStringArray(R.array.stop_message)[1]
            } else {
                // the final destination has been reached,
                // stop the location data source
                locationDisplay.dataSource.stop()
                // set last stop message
                nextStopText = getStringArray(R.array.stop_message)[2]
            }
        }
    }

    /**
     * Update the remaining and traveled route graphics using [trackingStatus]
     */
    private fun updateRouteGraphics(trackingStatus: TrackingStatus) {
        trackingStatus.routeProgress.let {
            // set geometries for the route ahead and the remaining route
            routeAheadGraphic.geometry = it.remainingGeometry
            routeTraveledGraphic.geometry = it.traversedGeometry
        }
    }

    /**
     * Initialize and add route travel graphics to the map using [routeResult]'s [Polyline] geometry.
     */
    private fun createRouteGraphics() {
        // clear any graphics from the current graphics overlay
        graphicsOverlay.graphics.clear()

        // set the map view view point to show the whole route
        val routeGeometry = routeResult?.routes?.get(0)?.routeGeometry

        // create a graphic (with a dashed line symbol) to represent the route
        routeAheadGraphic = Graphic(
            routeGeometry,
            SimpleLineSymbol(
                SimpleLineSymbolStyle.Dash,
                Color(getColorArgb(com.esri.arcgismaps.sample.sampleslib.R.color.colorPrimary)),
                3f
            )
        )

        // create a graphic (solid) to represent the route that's been traveled (initially empty)
        routeTraveledGraphic = Graphic(
            routeGeometry,
            SimpleLineSymbol(
                SimpleLineSymbolStyle.Solid,
                Color.cyan,
                3f
            )
        )

        val stopGraphics = routeStops.map {
            Graphic(
                geometry = it.geometry,
                symbol = SimpleMarkerSymbol(
                    style = SimpleMarkerSymbolStyle.Circle,
                    color = Color.red,
                    size = 10f
                )
            )
        }

        // add the graphics to the mapView's graphics overlays
        graphicsOverlay.graphics.addAll(
            listOf(routeAheadGraphic, routeTraveledGraphic) + stopGraphics
        )
    }

    /**
     * Resets the navigation back to the initial state by stopping the
     * [locationDisplay]'s datasource and cancels related coroutine tasks.
     */
    fun resetNavigation() {
        // reset the navigation if button is clicked
        viewModelScope.launch {
            if (locationDisplayJob?.isActive == true) {
                // stop location data sources
                locationDisplay.dataSource.stop()
                // reset location display auto-pan-mode
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Off)
                // cancel the coroutine job
                locationDisplayJob?.cancelAndJoin()
            }
            // set the map view view point to show the whole route
            routeResult?.routes?.get(0)?.routeGeometry?.extent?.let {
                mapViewProxy.setViewpoint(Viewpoint(it.extent))
            }
            mapViewProxy.setViewpointRotation(0.0)
            createRouteGraphics()

            isNavigateButtonEnabled = true
        }
    }

    /**
     * Uses Android's [textToSpeech] to speak to say the latest
     * voice guidance from the [routeTracker] out loud.
     */
    private suspend fun updateVoiceGuidance(routeTracker: RouteTracker) {
        // listen for new voice guidance events
        routeTracker.newVoiceGuidance.collect { voiceGuidance ->
            // use Android's text to speech to speak the voice guidance
            textToSpeech?.speak(voiceGuidance.text, TextToSpeech.QUEUE_FLUSH, null, null)
            // set next direction text
            nextDirectionText = getString(R.string.next_direction) + " " + voiceGuidance.text
        }
    }

    fun recenterNavigation() {
        locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
        isRecenterButtonEnabled = false
    }

    private fun getString(id: Int): String {
        return getApplication<Application>().resources.getString(id)
    }

    private fun getStringArray(id: Int): Array<out String> {
        return getApplication<Application>().resources.getStringArray(id)
    }

    private fun getColorArgb(id: Int): Int {
        return ContextCompat.getColor(getApplication(), id)
    }

    fun setLocationDisplay(locationDisplay: LocationDisplay) {
        this.locationDisplay = locationDisplay
    }
}
