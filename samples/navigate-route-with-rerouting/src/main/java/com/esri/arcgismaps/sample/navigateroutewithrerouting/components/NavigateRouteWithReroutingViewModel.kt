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

    // Path of the San Diego transport network used by the route task
    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() +
                File.separator + application.getString(R.string.navigate_route_with_rerouting_app_name)
    }

    // Passed to the composable MapView to set the mapViewProxy
    val mapViewProxy = MapViewProxy()

    // The map to display basemap, route traveled & route ahead graphics
    val map = ArcGISMap(BasemapStyle.ArcGISStreets)

    // Graphics overlay to display the route ahead and traveled graphics
    val graphicsOverlay = GraphicsOverlay()

    // Keep track of the the location display job when navigation is enabled
    private var locationDisplayJob: Job? = null

    // Default location display object, which is updated by rememberLocationDisplay
    private var locationDisplay: LocationDisplay = LocationDisplay()

    // Starting point: San Diego Convention Center
    private val startingStop = Stop(Point(-117.160386, 32.706608, SpatialReference.wgs84()))
        .apply { name = "San Diego Convention Center" }

    // Destination point: Fleet Science Center
    private val destinationStop = Stop(Point(-117.146679, 32.730351, SpatialReference.wgs84()))
        .apply { name = "RH Fleet Aerospace Museum" }

    // Generate a route with directions and stops for navigation
    val routeTask = RouteTask(
        pathToDatabase = "$provisionPath/sandiego.geodatabase",
        networkName = "Streets_ND"
    )

    // The route parameters needed to calculate a route from a start and stop point
    private var routeParameters = RouteParameters()

    // The resulted route from the route task using the route parameters
    private var routeResult: RouteResult? = null

    // Instance of the route ahead polyline
    private var routeAheadGraphic: Graphic = Graphic(
        symbol = SimpleLineSymbol(
            SimpleLineSymbolStyle.Dash,
            Color(getColorArgb(com.esri.arcgismaps.sample.sampleslib.R.color.colorPrimary)),
            3f
        )
    )

    // Graphic to represent the route that's been traveled (initially empty)
    private var routeTraveledGraphic: Graphic = Graphic(
        symbol = SimpleLineSymbol(
            SimpleLineSymbolStyle.Solid,
            Color.black,
            3f
        )
    )

    var distanceRemainingText by mutableStateOf("")
        private set

    var timeRemainingText by mutableStateOf("")
        private set

    var nextDirectionText by mutableStateOf("")
        private set

    var isNavigateButtonEnabled by mutableStateOf(true)
        private set

    var isRecenterButtonEnabled by mutableStateOf(false)
        private set

    // Boolean to check if Android text-speech is initialized
    private var isTextToSpeechInitialized = AtomicBoolean(false)

    // Instance of Android text-speech
    private var textToSpeech: TextToSpeech? = null

    // JSON of polylines of the path for the simulated data source
    private val polylineJSON: String by lazy {
        application.getString(R.string.simulation_path_json)
    }

    // Polyline representing simulation path
    private val simulationPolyline = Geometry.fromJsonOrNull(polylineJSON) as Polyline

    // Create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    fun initialize(locationDisplay: LocationDisplay) {
        // Set the location display to be used by this view model
        this.locationDisplay = locationDisplay
        // Initialize text-to-speech to replay navigation voice guidance
        val context = getApplication<Application>().applicationContext
        textToSpeech = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeech?.language = context.resources.configuration.locales[0]
                isTextToSpeechInitialized.set(true)
            }
        }
        // Load and set the route parameters
        viewModelScope.launch {
            routeParameters = routeTask.createDefaultParameters()
                .getOrElse { return@launch messageDialogVM.showMessageDialog(it) }.apply {
                    setStops(listOf(startingStop, destinationStop))
                    returnDirections = true
                    returnStops = true
                    returnRoutes = true
                }
            // Load the map
            map.load().onFailure { return@launch messageDialogVM.showMessageDialog(it) }
            // Get the solved route result
            routeResult = routeTask.solveRoute(routeParameters).getOrElse {
                return@launch messageDialogVM.showMessageDialog(it)
            }
            // Reset navigation to initial state
            resetNavigation()
        }
    }

    /**
     * Start the navigation along the provided route using the [routeResult] and
     * collects updates in the location using the MapView's location display.
     * */
    fun startNavigation() {
        // Get the current route result
        val routeResult = routeResult
            ?: return messageDialogVM.showMessageDialog("Error retrieving route result")
        // Set up a simulated location data source which simulates movement along the route
        val simulationParameters = SimulationParameters(
            startTime = Instant.now(),
            velocity = 35.0,
            horizontalAccuracy = 5.0,
            verticalAccuracy = 5.0
        )
        // Create the simulated data source using the polyline geometry and parameters
        val simulatedLocationDataSource = SimulatedLocationDataSource(
            polyline = simulationPolyline,
            parameters = simulationParameters
        )
        // set up a RouteTracker for navigation along the calculated route
        val routeTracker = RouteTracker(
            routeResult = routeResult,
            routeIndex = 0,
            skipCoincidentStops = true
        ).apply {
            setSpeechEngineReadyCallback {
                isTextToSpeechInitialized.get() && textToSpeech?.isSpeaking == false
            }
        }
        // Manage the job which triggers the location display
        locationDisplayJob = with(viewModelScope) {
            launch {
                // Check if this route task supports rerouting
                if (routeTask.getRouteTaskInfo().supportsRerouting) {
                    // Set up the re-routing parameters
                    val reroutingParameters = ReroutingParameters(
                        routeTask = routeTask,
                        routeParameters = routeParameters
                    ).apply {
                        strategy = ReroutingStrategy.ToNextWaypoint
                        visitFirstStopOnStart = false
                    }
                    // Enable automatic re-routing
                    routeTracker.enableRerouting(parameters = reroutingParameters)
                        .onFailure { return@launch messageDialogVM.showMessageDialog(it) }
                }
                // Create a route tracker location data source to snap the location display to the route
                val routeTrackerLocationDataSource = RouteTrackerLocationDataSource(
                    routeTracker = routeTracker,
                    locationDataSource = simulatedLocationDataSource
                )
                // Set the simulated location data source as the location data source for this app
                locationDisplay.dataSource = routeTrackerLocationDataSource
                // Start the location data source
                locationDisplay.dataSource.start().getOrElse {
                    return@launch messageDialogVM.showMessageDialog(it)
                }
                // Set the auto pan to navigation mode
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
                // Plays the direction voice guidance
                updateVoiceGuidance(routeTracker)
                // Zoom in the scale to focus on the navigation route
                mapViewProxy.setViewpointScale(10000.0)
            }
            launch {
                // Listen for changes in location
                locationDisplay.location.collect {
                    // Get the route's tracking status
                    val trackingStatus = routeTracker.trackingStatus.value ?: return@collect
                    // Displays the remaining and traversed route
                    updateRouteGraphics(trackingStatus)
                    // Display route status and directions info
                    displayRouteInfo(trackingStatus)
                    // Disable navigation button
                    isNavigateButtonEnabled = false
                }
            }
            launch {
                // Automatically enable recenter button when navigation pan is disabled
                locationDisplay.autoPanMode.filter { it == LocationDisplayAutoPanMode.Off }
                    .collect { isRecenterButtonEnabled = true }
            }
            launch {
                routeTracker.rerouteStarted.collect {
                    nextDirectionText = "Re-routing..."
                }
            }
        }
    }

    /**
     * Displays the route distance and time information using [trackingStatus].
     * When destination is reached the location data source is stopped.
     */
    private suspend fun displayRouteInfo(trackingStatus: TrackingStatus) {
        // Get remaining distance information
        val remainingDistance = trackingStatus.destinationProgress.remainingDistance
        // Convert remaining minutes to hours:minutes:seconds
        val remainingTimeString = DateUtils.formatElapsedTime(
            (trackingStatus.destinationProgress.remainingTime * 60).toLong()
        )
        // Update text views
        timeRemainingText = getString(R.string.time_remaining) + " " + remainingTimeString
        distanceRemainingText = getString(R.string.distance_remaining) + " " +
                remainingDistance.displayText + " " +
                remainingDistance.displayTextUnits.abbreviation
        // If the destination has been reached
        if (trackingStatus.destinationStatus == DestinationStatus.Reached) {
            // Stop the location data source
            locationDisplay.dataSource.stop()
        }
    }

    /**
     * Update the remaining and traveled route graphics using [trackingStatus]
     */
    private fun updateRouteGraphics(trackingStatus: TrackingStatus) {
        trackingStatus.routeProgress.let {
            // Set geometries for the route ahead and the remaining route
            routeAheadGraphic.geometry = it.remainingGeometry
            routeTraveledGraphic.geometry = it.traversedGeometry
        }
    }

    /**
     * Initialize and add route travel graphics to the map using [routeResult]'s [Polyline] geometry.
     */
    private fun createRouteGraphics() {
        // Clear any graphics from the current graphics overlay
        graphicsOverlay.graphics.clear()
        // Set the view point to show the whole route
        val routeGeometry = routeResult?.routes?.get(0)?.routeGeometry
        // Set the geometry of the route ahead to the solved route geometry
        routeAheadGraphic.geometry = routeGeometry
        // Create the start and stop marker graphics
        val startGraphic = Graphic(
            geometry = startingStop.geometry,
            symbol = SimpleMarkerSymbol(
                style = SimpleMarkerSymbolStyle.Cross,
                color = Color.green,
                size = 20f
            )
        )
        val destinationGraphic = Graphic(
            geometry = destinationStop.geometry,
            symbol = SimpleMarkerSymbol(
                style = SimpleMarkerSymbolStyle.X,
                color = Color.red,
                size = 20f
            )
        )
        // Add the graphics to the graphics overlays
        graphicsOverlay.graphics.addAll(
            listOf(routeAheadGraphic, routeTraveledGraphic, startGraphic, destinationGraphic)
        )
    }

    /**
     * Resets the navigation back to the initial state by stopping the
     * [locationDisplay]'s datasource and cancels related coroutine tasks.
     */
    fun resetNavigation() {
        // Reset the navigation if button is clicked
        viewModelScope.launch {
            if (locationDisplayJob?.isActive == true) {
                // Stop location data sources
                locationDisplay.dataSource.stop()
                // Reset location display auto-pan-mode
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Off)
                // Cancel the coroutine job
                locationDisplayJob?.cancelAndJoin()
            }
            // Set the map view view point to show the whole route
            routeResult?.routes?.get(0)?.routeGeometry?.extent?.let {
                mapViewProxy.setViewpointGeometry(it, 50.0)
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
        // Listen for new voice guidance events
        routeTracker.newVoiceGuidance.collect { voiceGuidance ->
            // Use Android's text to speech to speak the voice guidance
            textToSpeech?.speak(voiceGuidance.text, TextToSpeech.QUEUE_FLUSH, null, null)
            // Set next direction text
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

    private fun getColorArgb(id: Int): Int {
        return ContextCompat.getColor(getApplication(), id)
    }
}
