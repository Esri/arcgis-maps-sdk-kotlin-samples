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

package com.esri.arcgismaps.sample.navigateroute.components

import android.app.Application
import android.speech.tts.TextToSpeech
import android.text.format.DateUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.getString
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
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
import com.arcgismaps.navigation.RouteTracker
import com.arcgismaps.navigation.TrackingStatus
import com.arcgismaps.tasks.networkanalysis.RouteResult
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.navigateroute.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class NavigateRouteViewModel(application: Application) : AndroidViewModel(application) {

    val map = ArcGISMap(BasemapStyle.ArcGISStreets)

    val graphicsOverlay = GraphicsOverlay()

    val mapViewProxy = MapViewProxy()

    // generate a route with directions and stops for navigation
    private val routeTask = RouteTask(getString(application, R.string.routing_service_url))

    // Destination list of stops for the RouteParameters
    private val routeStops = listOf(
        // San Diego Convention Center
        Stop(Point(-117.160386, 32.706608, SpatialReference.wgs84())),
        // USS San Diego Memorial
        Stop(Point(-117.173034, 32.712327, SpatialReference.wgs84())),
        // RH Fleet Aerospace Museum
        Stop(Point(-117.147230, 32.730467, SpatialReference.wgs84()))
    )

    private var locationDisplayJob: Job? = null

    private var locationDisplay: LocationDisplay = LocationDisplay()

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

        viewModelScope.launch(Dispatchers.IO) {
            // load and set the route parameters
            val routeParameters = routeTask.createDefaultParameters().getOrElse {
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
            routeResult = routeTask.solveRoute(routeParameters).getOrElse {
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
            polyline = routeGeometry,
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


        // create a route tracker location data source to snap the location display to the route
        val routeTrackerLocationDataSource = RouteTrackerLocationDataSource(
            routeTracker = routeTracker,
            locationDataSource = simulatedLocationDataSource
        )

        locationDisplayJob = with(viewModelScope) {
            launch(Dispatchers.IO) {
                // automatically enable recenter button when navigation pan is disabled
                locationDisplay.autoPanMode.filter { it == LocationDisplayAutoPanMode.Off }
                    .collect { isRecenterButtonEnabled = true }
            }

            launch(Dispatchers.IO) {
                // set the simulated location data source as the location data source for this app
                locationDisplay.dataSource = routeTrackerLocationDataSource

                // start the location data source
                locationDisplay.dataSource.start().getOrElse {
                    messageDialogVM.showMessageDialog(
                        title = "Error starting location data source",
                        description = it.message.toString()
                    )
                }

                // data source has started, display stop message
                nextStopText = getStringArray(R.array.stop_message)[0]

                // plays the direction voice guidance
                updateVoiceGuidance(routeTracker)

                // set the auto pan to navigation mode
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)

                launch(Dispatchers.IO) {
                    // zoom in the scale to focus on the navigation route
                    mapViewProxy.setViewpointScale(10000.0)
                }
            }

            launch(Dispatchers.IO) {
                // listen for changes in location
                locationDisplay.location.collect {
                    // get the route's tracking status
                    val trackingStatus = routeTracker.trackingStatus.value ?: return@collect
                    // displays the remaining and traversed route
                    updateRouteGraphics(trackingStatus)
                    // display route status and directions info
                    displayRouteInfo(routeTracker, trackingStatus)
                    // disable navigation button
                    isNavigateButtonEnabled = false
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
        routeTracker: RouteTracker,
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
                routeTracker.switchToNextDestination().getOrElse {
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

    fun resetNavigation() {
        // reset the navigation if button is clicked
        viewModelScope.launch(Dispatchers.IO) {
            if (locationDisplayJob?.isActive == true) {
                // stop location data sources
                locationDisplay.dataSource.stop()
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

    @Suppress("DEPRECATION")
    private fun getColorArgb(id: Int): Int {
        return getApplication<Application>().resources.getColor(id)
    }

    fun setLocationDisplay(locationDisplay: LocationDisplay) {
        this.locationDisplay = locationDisplay
    }
}
