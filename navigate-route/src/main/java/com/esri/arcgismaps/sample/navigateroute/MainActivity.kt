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

package com.esri.arcgismaps.sample.navigateroute

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.format.DateUtils
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
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
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.navigation.DestinationStatus
import com.arcgismaps.navigation.RouteTracker
import com.arcgismaps.navigation.TrackingStatus
import com.arcgismaps.tasks.networkanalysis.RouteResult
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import com.esri.arcgismaps.sample.navigateroute.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val resetNavigationButton: Button by lazy {
        activityMainBinding.resetNavigationButton
    }

    private val recenterButton: Button by lazy {
        activityMainBinding.recenterButton
    }

    private val distanceRemainingTextView: TextView by lazy {
        activityMainBinding.distanceRemainingTextView
    }

    private val timeRemainingTextView: TextView by lazy {
        activityMainBinding.timeRemainingTextView
    }

    private val nextDirectionTextView: TextView by lazy {
        activityMainBinding.nextDirectionTextView
    }

    private val nextStopTextView: TextView by lazy {
        activityMainBinding.nextStopTextView
    }

    /**
     * Destination list of stops for the RouteParameters
     */
    private val routeStops by lazy {
        listOf(
            // San Diego Convention Center
            Stop(Point(-117.160386, 32.706608, SpatialReference.wgs84())),
            // USS San Diego Memorial
            Stop(Point(-117.173034, 32.712327, SpatialReference.wgs84())),
            // RH Fleet Aerospace Museum
            Stop(Point(-117.147230, 32.730467, SpatialReference.wgs84()))
        )
    }

    // instance of the route ahead polyline
    private var routeAheadGraphic: Graphic = Graphic()

    // instance of the route traveled polyline
    private var routeTraveledGraphic: Graphic = Graphic()

    // instance of the MapView's graphic overlay
    private val graphicsOverlay = GraphicsOverlay()

    // boolean to check if Android text-speech is initialized
    private var isTextToSpeechInitialized = AtomicBoolean(false)

    // instance of Android text-speech
    private var textToSpeech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        // some parts of the API require an Android Context to properly interact with Android system
        // features, such as LocationProvider and application resources
        ArcGISEnvironment.applicationContext = applicationContext

        lifecycle.addObserver(mapView)

        // create and add a map with a streets basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISStreets)
        mapView.map = map

        // create a graphics overlay to hold our route graphics
        mapView.graphicsOverlays.add(graphicsOverlay)

        // create text-to-speech to replay navigation voice guidance
        textToSpeech = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeech?.language = resources.configuration.locales[0]
                isTextToSpeechInitialized.set(true)
            }
        }

        // generate a route with directions and stops for navigation
        val routeTask = RouteTask(getString(R.string.routing_service_url))

        // create the default parameters and solve route
        lifecycleScope.launch {
            // load and set the route parameters
            val routeParameters = routeTask.createDefaultParameters().getOrElse {
                return@launch showError("Error creating default parameters:${it.message}")
            }.apply {
                setStops(routeStops)
                returnDirections = true
                returnStops = true
                returnRoutes = true
            }

            // get the solved route result
            val routeResult = routeTask.solveRoute(routeParameters).getOrElse {
                return@launch showError("Error solving route:${it.message}")
            }

            // get the route geometry from the route result
            val routeGeometry = routeResult.routes[0].routeGeometry

            // set the map view view point to show the whole route
            if (routeGeometry?.extent != null) {
                mapView.setViewpoint(Viewpoint(routeGeometry.extent))
            } else {
                return@launch showError("Route geometry extent is null.")
            }

            // start navigating on app launch
            startNavigation(routeResult)
        }

        // wire up recenter button
        recenterButton.setOnClickListener {
            mapView.locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
            recenterButton.isEnabled = false
        }
    }

    /**
     * Start the navigation along the provided route using the [routeResult] and
     * collects updates in the location using the MapView's location display.
     * */
    private fun startNavigation(routeResult: RouteResult) {
        // get the route's geometry from the route result
        val routeGeometry: Polyline = routeResult.routes[0].routeGeometry
            ?: return showError("Route is missing geometry")

        // create the graphics using the route's geometry
        createRouteGraphics(routeGeometry)

        // set up a simulated location data source which simulates movement along the route
        val simulationParameters = SimulationParameters(
            Instant.now(),
            velocity = 35.0,
            horizontalAccuracy = 5.0,
            verticalAccuracy = 5.0
        )

        // create the simulated data source using the geometry and parameters
        val simulatedLocationDataSource = SimulatedLocationDataSource(
            routeGeometry, simulationParameters
        )

        // set up a RouteTracker for navigation along the calculated route
        val routeTracker = RouteTracker(
            routeResult,
            routeIndex = 0,
            skipCoincidentStops = true
        ).apply {
            isSpeechEngineReady =
                { isTextToSpeechInitialized.get() && textToSpeech?.isSpeaking == false }
        }
        // plays the direction voice guidance
        lifecycleScope.launch {
            updateVoiceGuidance(routeTracker)
        }

        // create a route tracker location data source to snap the location display to the route
        val routeTrackerLocationDataSource = RouteTrackerLocationDataSource(
            routeTracker,
            simulatedLocationDataSource
        )

        // get the map view's location display and set it up
        val locationDisplay = mapView.locationDisplay.also {
            // set the simulated location data source as the location data source for this app
            it.dataSource = routeTrackerLocationDataSource
            it.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
        }

        // start the LocationDisplay, which starts the
        // RouteTrackerLocationDataSource and SimulatedLocationDataSource
        lifecycleScope.launch {
            locationDisplay.dataSource.start().getOrElse {
                showError("Error starting LocationDataSource: ${it.message} ")
            }
            // set the text for first destination
            nextStopTextView.text = resources.getStringArray(R.array.stop_message)[0]
        }

        // listen for changes in location
        val locationDisplayJob = lifecycleScope.launch {
            locationDisplay.location.collect {
                // get the route's tracking status
                val trackingStatus = routeTracker.trackingStatus.value ?: return@collect

                // displays the remaining and traversed route
                updateRouteGraphics(trackingStatus)

                // display route status and directions info
                displayRouteInfo(routeTracker,
                    trackingStatus,
                    simulatedLocationDataSource,
                    routeTrackerLocationDataSource)
            }
        }

        // listen if user navigates the map view away from the
        // location display, activate the recenter button
        val autoPanModeJob = lifecycleScope.launch {
            locationDisplay.autoPanMode.filter { it == LocationDisplayAutoPanMode.Off }
                .collect { recenterButton.isEnabled = true }
        }

        // reset the navigation if button is clicked
        resetNavigationButton.setOnClickListener {
            lifecycleScope.launch {
                if (locationDisplayJob.isActive) {
                    // stop location data sources
                    simulatedLocationDataSource.stop()
                    routeTrackerLocationDataSource.stop()
                    // cancel the coroutine jobs
                    locationDisplayJob.cancelAndJoin()
                    autoPanModeJob.cancelAndJoin()
                    // start navigation again
                    startNavigation(routeResult)
                }
            }
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
            nextDirectionTextView.text = getString(R.string.next_direction, voiceGuidance.text)
        }
    }

    /**
     * Displays the route distance and time information using [trackingStatus], and
     * switches destinations using [routeTracker]. When final destination is reached,
     * and [routeTrackerLocationDataSource] is stopped.
     */
    private suspend fun displayRouteInfo(
        routeTracker: RouteTracker,
        trackingStatus: TrackingStatus,
        simulatedLocationDataSource: SimulatedLocationDataSource,
        routeTrackerLocationDataSource: RouteTrackerLocationDataSource,
    ) {
        // get remaining distance information
        val remainingDistance = trackingStatus.destinationProgress.remainingDistance
        // convert remaining minutes to hours:minutes:seconds
        val remainingTimeString =
            DateUtils.formatElapsedTime((trackingStatus.destinationProgress.remainingTime * 60).toLong())

        // update text views
        distanceRemainingTextView.text = getString(
            R.string.distance_remaining,
            remainingDistance.displayText,
            remainingDistance.displayTextUnits.abbreviation
        )
        timeRemainingTextView.text = getString(R.string.time_remaining, remainingTimeString)

        // if a destination has been reached
        if (trackingStatus.destinationStatus == DestinationStatus.Reached) {
            // if there are more destinations to visit. Greater than 1 because the start point is considered a "stop"
            if (trackingStatus.remainingDestinationCount > 1) {
                // switch to the next destination
                routeTracker.switchToNextDestination().getOrElse {
                    return showError("Error retrieving next destination: ${it.message}")
                }
                // set second stop message
                nextStopTextView.text = resources.getStringArray(R.array.stop_message)[1]
            } else {
                // the final destination has been reached,
                // stop the location data source
                mapView.locationDisplay.dataSource.stop()
                // set last stop message
                nextStopTextView.text = resources.getStringArray(R.array.stop_message)[2]
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
     * Initialize and add route travel graphics to the map using [routeGeometry]
     */
    private fun createRouteGraphics(routeGeometry: Polyline) {
        // clear any graphics from the current graphics overlay
        graphicsOverlay.graphics.clear()

        // create a graphic (with a dashed line symbol) to represent the route
        routeAheadGraphic = Graphic(
            routeGeometry,
            SimpleLineSymbol(
                SimpleLineSymbolStyle.Dash,
                Color(getColor(R.color.colorPrimary)),
                5f
            )
        )

        // create a graphic (solid) to represent the route that's been traveled (initially empty)
        routeTraveledGraphic = Graphic(
            routeGeometry,
            SimpleLineSymbol(
                SimpleLineSymbolStyle.Solid,
                Color.red,
                5f
            )
        )

        // add the graphics to the mapView's graphics overlays
        mapView.graphicsOverlays[0].graphics.addAll(
            listOf(
                routeAheadGraphic,
                routeTraveledGraphic
            )
        )
    }

    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}

