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
import com.arcgismaps.location.NmeaGnssSystem
import com.arcgismaps.location.NmeaLocationDataSource
import com.arcgismaps.location.NmeaSatelliteInfo
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
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

    private val provisionPath: String by lazy {
        getExternalFilesDir(null)?.path.toString() + File.separator + getString(R.string.app_name)
    }

    // create a new NMEA location data source
    private val nmeaLocationDataSource: NmeaLocationDataSource =
        NmeaLocationDataSource(SpatialReference.wgs84())

    // create a timer to simulate a stream of NMEA data
    private var timer = Timer()

    // keeps track of the timer during play/pause
    private var count = 0

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val accuracyTV: TextView by lazy {
        activityMainBinding.accuracyTV
    }

    private val satelliteCountTV: TextView by lazy {
        activityMainBinding.satelliteCountTV
    }

    private val satelliteIDsTV: TextView by lazy {
        activityMainBinding.satelliteIDsTV
    }

    private val systemTypeTV: TextView by lazy {
        activityMainBinding.systemTypeTV
    }

    private val playPauseFAB: FloatingActionButton by lazy {
        activityMainBinding.playPauseFAB
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISNavigationNight)
        mapView.map = map

        // set a viewpoint on the map view centered on Redlands, California
        mapView.setViewpoint(
            Viewpoint(
                Point(-117.191, 34.0306, SpatialReference.wgs84()),
                100000.0
            )
        )

        // set the NMEA location data source onto the map view's location display
        val locationDisplay = mapView.locationDisplay
        locationDisplay.dataSource = nmeaLocationDataSource
        locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)

        // disable map view interaction, the location display will automatically center on the mock device location
        mapView.interactionOptions.isPanEnabled = false
        mapView.interactionOptions.isZoomEnabled = false
    }

    /**
     * Control the start/stop status of the NMEA location data source
     */
    fun playPauseClick(view: View) {
        lifecycleScope.launch {
            if (nmeaLocationDataSource.status.value != LocationDataSourceStatus.Started) {
                // start location data source
                displayDeviceLocation()
                setButtonStatus(true)
            } else {
                // stop receiving and displaying location data
                nmeaLocationDataSource.stop()
                setButtonStatus(false)
                clearInformation()
            }
        }
    }

    /**
     * Sets the FAB button to "Start"/"Stop" based on [isShowingLocation]
     */
    private fun setButtonStatus(isShowingLocation: Boolean) = if (isShowingLocation) {
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
                // read the nmea file contents using a buffered reader and store the mock data sentences in a list
                val bufferedReader = BufferedReader(FileReader(simulatedNmeaDataFile.path))
                // add carriage return for NMEA location data source parser
                val nmeaSentences: MutableList<String> = mutableListOf()
                var line = bufferedReader.readLine()
                while (line != null) {
                    nmeaSentences.add(line + "\n")
                    line = bufferedReader.readLine()
                }
                bufferedReader.close()

                lifecycleScope.apply {
                    launch {
                        // initialize the location data source and prepare to begin receiving location updates when data is pushed
                        // as updates are received, they will be displayed on the map
                        nmeaLocationDataSource.start()
                    }
                    launch {
                        // collect the accuracy for each location change
                        nmeaLocationDataSource.locationChanged.collect {
                            // convert from meters to foot
                            val horizontalAccuracy = it.horizontalAccuracy * 3.28084
                            val verticalAccuracy = it.verticalAccuracy * 3.28084
                            accuracyTV.text = getString(R.string.accuracy) +
                                    "Horizontal-%.1fft, Vertical-%.1fft".format(
                                        horizontalAccuracy,
                                        verticalAccuracy
                                    )
                        }
                    }
                    launch {
                        // handle when LocationDataSource status is changed
                        nmeaLocationDataSource.status.collect {
                            if (it == LocationDataSourceStatus.Started) {
                                // add a satellite changed listener to the NMEA location data source and display satellite information
                                setupSatelliteChangedListener()
                                // starts the NMEA mock data sentences
                                startNMEAMockData(nmeaSentences)
                            }
                            if (it == LocationDataSourceStatus.Stopped) {
                                timer.cancel()
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
     * Push the mock data NMEA sentences into the data source every 250 ms
     */
    private fun startNMEAMockData(nmeaSentences: MutableList<String>) {
        timer = Timer()
        timer.schedule(timerTask {
            // only push data when started
            if (nmeaLocationDataSource.status.value == LocationDataSourceStatus.Started)
                nmeaLocationDataSource.pushData(
                    nmeaSentences[count++].toByteArray(
                        StandardCharsets.UTF_8
                    )
                )
            // reset the count after the last data point is reached
            if (count == nmeaSentences.size)
                count = 0
        }, 250, 250)
    }

    /**
     * Obtains NMEA satellite information from the NMEA location data source, and displays satellite information on the app
     */
    private fun setupSatelliteChangedListener() {
        lifecycleScope.launch {
            nmeaLocationDataSource.satellitesChanged.collect {
                val uniqueSatelliteIDs: HashSet<Int> = hashSetOf()
                // get satellite information from the NMEA location data source every time the satellites change
                val nmeaSatelliteInfoList: List<NmeaSatelliteInfo> = it
                // set the text of the satellite count label
                satelliteCountTV.text =
                    getString(R.string.satellite_count) + nmeaSatelliteInfoList.size

                for (satInfo in nmeaSatelliteInfoList) {
                    // collect unique satellite ids
                    uniqueSatelliteIDs.add(satInfo.id)
                    // sort the ids numerically
                    val sortedIds: MutableList<Int> = ArrayList(uniqueSatelliteIDs)
                    sortedIds.sort()
                    // get the satellite system used
                    var systemText = getString(R.string.system)
                    when (satInfo.system) {
                        NmeaGnssSystem.Bds -> {
                            systemText += "BDS"
                        }
                        NmeaGnssSystem.Galileo -> {
                            systemText += "Galileo"
                        }
                        NmeaGnssSystem.Glonass -> {
                            systemText += "Glonass"
                        }
                        NmeaGnssSystem.Gps -> {
                            systemText += "GPS"
                        }
                        NmeaGnssSystem.NavIc -> {
                            systemText += "NavIc"
                        }
                        NmeaGnssSystem.Qzss -> {
                            systemText += "Qzss"
                        }
                        NmeaGnssSystem.Unknown -> {
                            systemText += "Unknown"
                        }
                    }
                    // display the satellite system and id information
                    systemTypeTV.text = systemText
                    satelliteIDsTV.text = getString(R.string.satellite_ids) + sortedIds

                }
            }
        }
    }

    /**
     * Clears out the info messages when LocationDataSource is paused.
     */
    private fun clearInformation() {
        accuracyTV.text = getString(R.string.accuracy)
        satelliteCountTV.text = getString(R.string.satellite_count)
        satelliteIDsTV.text = getString(R.string.satellite_ids)
        systemTypeTV.text = getString(R.string.system)
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
