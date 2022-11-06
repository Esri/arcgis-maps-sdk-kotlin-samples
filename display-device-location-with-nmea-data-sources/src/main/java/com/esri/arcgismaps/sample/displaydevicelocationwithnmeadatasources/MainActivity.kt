/* Copyright 2022 Esri
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

package com.esri.arcgismaps.sample.displaydevicelocationwithnmeadatasources

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.LocationDataSourceStatus
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.location.NmeaLocationDataSource
import com.arcgismaps.location.NmeaSatelliteInfo
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.MapView
import com.esri.arcgismaps.sample.displaydevicelocationwithnmeadatasources.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.concurrent.timerTask

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // Create a new NMEA location data source
    private val nmeaLocationDataSource: NmeaLocationDataSource =
        NmeaLocationDataSource(SpatialReference.wgs84())

    private val provisionPath: String by lazy {
        getExternalFilesDir(null)?.path.toString() + File.separator + getString(R.string.app_name)
    }

    // Create a timer to simulate a stream of NMEA data
    private var timer = Timer()

    // Keeps track of the timer during play/pause
    private var count = 0

    private val activityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val mapView: MapView by lazy {
        activityMainBinding.mapView
    }

    private val playPauseFAB: FloatingActionButton by lazy {
        activityMainBinding.playPauseFAB
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        // set up data binding for the activity
        val activityMainBinding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)
        val mapView = activityMainBinding.mapView
        lifecycle.addObserver(mapView)

        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISNavigationNight)
        mapView.map = map

        // Set a viewpoint on the map view centered on Redlands, California
        mapView.setViewpoint(
            Viewpoint(
                Point(-117.191, 34.0306, SpatialReference.wgs84()),
                100000.0
            )
        )

        // Set the NMEA location data source onto the map view's location display
        val locationDisplay = mapView.locationDisplay
        locationDisplay.dataSource = nmeaLocationDataSource
        locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)

        // Disable map view interaction, the location display will automatically center on the mock device location
        mapView.interactionOptions.isPanEnabled = false
        mapView.interactionOptions.isZoomEnabled = false
    }

    /**
     * Control the start/stop status of the NMEA location data source
     */
    fun playPauseClick(view: View) {
        lifecycleScope.launch {
            if (nmeaLocationDataSource.status.value != LocationDataSourceStatus.Started) {
                // Start location data source
                displayDeviceLocation()
                setLocationStatus(true)
            } else {
                // Stop receiving and displaying location data
                nmeaLocationDataSource.stop()
                setLocationStatus(false)
            }
        }
    }

    /**
     * Sets the FAB button to "Start"/"Stop" based on the argument [isShowingLocation]
     */
    private fun setLocationStatus(isShowingLocation: Boolean) = if (isShowingLocation) {
        playPauseFAB.setImageDrawable(
            AppCompatResources.getDrawable(
                this,
                R.drawable.ic_round_pause_24
            )
        )
    } else {
        playPauseFAB.setImageDrawable(
            AppCompatResources.getDrawable(
                this,
                R.drawable.ic_round_play_arrow_24
            )
        )
    }

    /**
     * Initializes the location data source, reads the mock data NMEA sentences, and displays location updates from that file
     * on the location display. Data is pushed to the data source using a timeline to simulate live updates, as they would
     * appear if using real-time data from a GPS dongle
     */
    private fun displayDeviceLocation() {
        val simulatedNmeaDataFile = File("$provisionPath/Redlands.nmea")
        if (simulatedNmeaDataFile.exists()) {
            try {
                // Read the nmea file contents using a buffered reader and store the mock data sentences in a list
                val bufferedReader = BufferedReader(FileReader(simulatedNmeaDataFile.path))
                // Add carriage return for NMEA location data source parser
                val nmeaSentences: MutableList<String> = mutableListOf()
                var line = bufferedReader.readLine()
                while (line != null) {
                    nmeaSentences.add(line + "\n")
                    line = bufferedReader.readLine()
                }
                bufferedReader.close()

                lifecycleScope.apply {
                    // Initialize the location data source and prepare to begin receiving location updates when data is pushed
                    // As updates are received, they will be displayed on the map
                    launch {
                        nmeaLocationDataSource.start()
                    }
                    launch {
                        // Set up the accuracy for each location change
                        nmeaLocationDataSource.locationChanged.collect {
                            //Convert from Meters to Foot
                            val horizontalAccuracy = it.horizontalAccuracy * 3.28084
                            val verticalAccuracy = it.verticalAccuracy * 3.28084
                            activityMainBinding.accuracyTV.text =
                                "Accuracy- Horizontal: %.1fft, Vertical: %.1fft".format(
                                    horizontalAccuracy,
                                    verticalAccuracy
                                )
                        }
                    }
                    launch {
                        // Handle when LocationDataSource status is changed
                        nmeaLocationDataSource.status.collect {
                            if (it == LocationDataSourceStatus.Started) {
                                // Add a satellite changed listener to the NMEA location data source and display satellite information
                                setupSatelliteChangedListener()

                                timer = Timer()

                                // Push the mock data NMEA sentences into the data source every 250 ms
                                timer.schedule(timerTask {
                                    // Only push data when started
                                    if (it == LocationDataSourceStatus.Started)
                                        nmeaLocationDataSource.pushData(
                                            nmeaSentences[count++].toByteArray(
                                                StandardCharsets.UTF_8
                                            )
                                        )
                                    // Reset the count after the last data point is reached
                                    if (count == nmeaSentences.size)
                                        count = 0
                                }, 250, 250)

                                setLocationStatus(true)
                            }
                            if (it == LocationDataSourceStatus.Stopped) {
                                timer.cancel()
                                setLocationStatus(false)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                showError("Error while setting up NmeaLocationDataSource: " + e.message)
            }
        } else {
            showError("NMEA File not found")
        }
    }

    /**
     * Obtains NMEA satellite information from the NMEA location data source, and displays satellite information on the app
     */
    private fun setupSatelliteChangedListener() {
        lifecycleScope.launch {
            nmeaLocationDataSource.satellitesChanged.collect {
                val uniqueSatelliteIDs: HashSet<Int> = hashSetOf()
                // Get satellite information from the NMEA location data source every time the satellites change
                val nmeaSatelliteInfoList: List<NmeaSatelliteInfo> = it
                // Set the text of the satellite count label
                activityMainBinding.satelliteCountTV.text = "Satellite count- " + nmeaSatelliteInfoList.size

                for (satInfo in nmeaSatelliteInfoList) {
                    // Collect unique satellite ids
                    uniqueSatelliteIDs.add(satInfo.id)
                    // Sort the ids numerically
                    val sortedIds: MutableList<Int> = ArrayList(uniqueSatelliteIDs)
                    sortedIds.sort()
                    // Display the satellite system and id information
                    activityMainBinding.apply {
                        systemTypeTV.text = "System- " + satInfo.system.toString()
                        satelliteIDsTV.text = "Satellite IDs- $sortedIds"
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
