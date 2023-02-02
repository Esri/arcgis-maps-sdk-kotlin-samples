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

import android.content.res.Resources
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.format.DateUtils
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
import com.arcgismaps.navigation.ReroutingParameters
import com.arcgismaps.navigation.RouteTracker
import com.arcgismaps.tasks.networkanalysis.RouteParameters
import com.arcgismaps.tasks.networkanalysis.RouteResult
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import com.esri.arcgismaps.sample.navigateroute.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val navigateRouteButton: Button by lazy {
        activityMainBinding.navigationControls.navigateRouteButton
    }

    private val recenterButton: Button by lazy {
        activityMainBinding.navigationControls.recenterButton
    }

    private val distanceRemainingTextView: TextView by lazy {
        activityMainBinding.navigationControls.distanceRemainingTextView
    }

    private val timeRemainingTextView: TextView by lazy {
        activityMainBinding.navigationControls.timeRemainingTextView
    }

    private val nextDirectionTextView: TextView by lazy {
        activityMainBinding.navigationControls.nextDirectionTextView
    }

    private var textToSpeech: TextToSpeech? = null


    private var isTextToSpeechInitialized = false

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

        // create a graphics overlay to hold our route graphics and clear any graphics
        mapView.graphicsOverlays.add(GraphicsOverlay())

        // create text-to-speech to replay navigation voice guidance
        textToSpeech = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeech?.language = Resources.getSystem().configuration.locale
                isTextToSpeechInitialized = true
            }
        }

        // generate a route with directions and stops for navigation
        val routeTask = RouteTask(getString(R.string.routing_service_url))

        // create the default parameters and solve route
        lifecycleScope.launch {
            // load and set the route parameters
            val routeParameters = routeTask.createDefaultParameters().getOrElse {
                return@launch showMessage("Error creating default parameters:${it.message}")
            }.apply {
                setStops(routeStops)
                returnDirections = true
                returnStops = true
                returnRoutes = true
            }

            // get the solved route result
            val routeResult = routeTask.solveRoute(routeParameters).getOrElse {
                return@launch showMessage("Error solving route:${it.message}")
            }

            // get the route geometry from the route result
            val routeGeometry = routeResult.routes[0].routeGeometry

            // create a graphic for the route geometry
            val routeGraphic = Graphic(
                routeGeometry, SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.red, 5f)
            )
            // add it to the graphics overlay
            mapView.graphicsOverlays[0].graphics.add(routeGraphic)
            // set the map view view point to show the whole route
            mapView.setViewpoint(Viewpoint(routeGeometry?.extent!!))

            // set button to start navigation with the given route
            navigateRouteButton.setOnClickListener {
                startNavigation(
                    routeTask, routeParameters, routeResult
                )
            }

            // start navigating
            startNavigation(routeTask, routeParameters, routeResult)
        }

        // wire up recenter button
        recenterButton.apply {
            isEnabled = false
            setOnClickListener {
                mapView.locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
                recenterButton.isEnabled = false
            }
        }
    }

    /**
     * Start the navigation along the provided route.
     *
     * @param routeTask used to generate the route.
     * @param routeParameters to describe the route.
     * @param routeResult solved from the routeTask.
     * */
    private fun startNavigation(
        routeTask: RouteTask,
        routeParameters: RouteParameters,
        routeResult: RouteResult,
    ) {
        // clear any graphics from the current graphics overlay
        mapView.graphicsOverlays[0].graphics.clear()

        // get the route's geometry from the route result
        val routeGeometry: Polyline = routeResult.routes[0].routeGeometry!!
        // create a graphic (with a dashed line symbol) to represent the route
        val routeAheadGraphic = Graphic(
            routeGeometry,
            SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color(R.color.colorPrimary), 5f)
        )
        // create a graphic (solid) to represent the route that's been traveled (initially empty)
        val routeTraveledGraphic = Graphic(
            routeGeometry,
            SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.red, 5f)
        )
        // add the graphics to the mapView's graphics overlays
        mapView.graphicsOverlays[0].graphics.addAll(listOf(routeAheadGraphic, routeTraveledGraphic))

        // set up a simulated location data source which simulates movement along the route
        val simulationParameters = SimulationParameters(Clock.System.now(), 35.0, 5.0, 5.0)
        val simulatedLocationDataSource = SimulatedLocationDataSource().apply {
            setLocationsWithPolyline(routeGeometry, simulationParameters)
        }

        // set up a RouteTracker for navigation along the calculated route
        val reroutingParameters = ReroutingParameters(routeTask, routeParameters)
        val routeTracker = RouteTracker(routeResult, 0, true)
        lifecycleScope.launch {
            routeTracker.enableRerouting(reroutingParameters)
        }

        // create a route tracker location data source to snap the location display to the route
        val routeTrackerLocationDataSource =
            RouteTrackerLocationDataSource(routeTracker, simulatedLocationDataSource)
        // get the map view's location display and set it up
        mapView.locationDisplay.apply {
            // set the simulated location data source as the location data source for this app
            dataSource = routeTrackerLocationDataSource
            setAutoPanMode(LocationDisplayAutoPanMode.Navigation)

            // if the user navigates the map view away from the
            // location display, activate the recenter button
            lifecycleScope.launch {
                autoPanMode.collect {
                    recenterButton.isEnabled = true
                }
            }

            //start the location display
            lifecycleScope.launch {
                dataSource.start().getOrElse {
                    showMessage("Error starting LocationDataSource: ${it.message} ")
                }
            }
        }

        // listen for changes in location
        lifecycleScope.launch {
            mapView.locationDisplay.location.collect {
                // get the route's tracking status
                val trackingStatus = routeTracker.trackingStatus.value
                    ?: return@collect showMessage("Tracking status is null")
                // set geometries for the route ahead and the remaining route
                routeAheadGraphic.geometry = trackingStatus.routeProgress.remainingGeometry
                routeTraveledGraphic.geometry = trackingStatus.routeProgress.traversedGeometry

                // get remaining distance information
                val remainingDistance = trackingStatus.destinationProgress.remainingDistance
                // covert remaining minutes to hours:minutes:seconds
                val remainingTimeString = DateUtils
                    .formatElapsedTime((trackingStatus.destinationProgress.remainingTime * 60).toLong())

                // update text views
                distanceRemainingTextView.text = getString(
                    R.string.distance_remaining, remainingDistance.displayText,
                    remainingDistance.displayTextUnits.pluralDisplayName
                )
                timeRemainingTextView.text = getString(R.string.time_remaining, remainingTimeString)

                // listen for new voice guidance events
                lifecycleScope.launch {
                    routeTracker.newVoiceGuidance.collect { voiceGuidance ->
                        // use Android's text to speech to speak the voice guidance
                        speakVoiceGuidance(voiceGuidance.text)
                        nextDirectionTextView.text = getString(
                            R.string.next_direction,
                            voiceGuidance.text
                        )
                    }
                }

                // if a destination has been reached
                if (trackingStatus.destinationStatus == DestinationStatus.Reached) {
                    // if there are more destinations to visit. Greater than 1 because the start point is considered a "stop"
                    if (trackingStatus.remainingDestinationCount > 1) {
                        // switch to the next destination
                        routeTracker.switchToNextDestination().getOrElse {
                            return@collect showMessage("Error retrieving next destination: ${it.message}")
                        }

                        showMessage("Navigating to the second stop, the Fleet Science Center.")
                    } else {
                        // the final destination has been reached, stop the simulated location data source
                        simulatedLocationDataSource.stop()
                        routeTrackerLocationDataSource.stop()
                        showMessage("Arrived at the final destination.")
                    }
                }
            }
        }


        // start the LocationDisplay, which starts the RouteTrackerLocationDataSource and SimulatedLocationDataSource
        showMessage("Navigating to the first stop, the USS San Diego Memorial.")
    }


    /**
     * Uses Android's text to speak to say the latest voice guidance from the RouteTracker out loud.
     *
     * @param voiceGuidanceText to be converted to speech
     */
    private fun speakVoiceGuidance(voiceGuidanceText: String) {
        if (isTextToSpeechInitialized && textToSpeech?.isSpeaking == false) {
            textToSpeech?.speak(voiceGuidanceText, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun showMessage(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }

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
}

