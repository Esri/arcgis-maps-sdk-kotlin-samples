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
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.esri.arcgismaps.sample.displaydevicelocationwithnmeadatasources.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
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

    // list of nmea location sentences
    private var nmeaSentences: List<String>? = emptyList()

    // index of nmea location sentence
    private var locationIndex = 0

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
                Point(-117.191, 34.0306, SpatialReference.wgs84()), 100000.0
            )
        )

        mapView.locationDisplay.apply {
            // set the map view's location display to use the nmea location data source
            dataSource = nmeaLocationDataSource
            // set the map view to recenter on location changed events
            setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
        }

        // disable map view interaction, the location display will automatically center on the mock device location
        mapView.interactionOptions.apply {
            isPanEnabled = false
            isZoomEnabled = false
            isRotateEnabled = false
        }

        // read nmea location sentences from file
        nmeaSentences = getNMEASentenceList()
        // collects the accuracy for each location change
        collectLocationChanges()
        // collects satellite changes and display satellite information
        collectSatelliteChanges()
    }

    /**
     * Reads NMEA location sentences from the .nmea file and
     * returns it as a [MutableList]
     */
    private fun getNMEASentenceList(): List<String>? {
        val simulatedNmeaDataFile = File("$provisionPath/Redlands.nmea")
        if (!simulatedNmeaDataFile.exists()) {
            showError("NMEA file does not exist")
            return null
        }
        // create list of nmea location sentences
        var nmeaSentences: List<String> = emptyList()
        // create a buffered reader using the .nmea file
        val bufferedReader = File(simulatedNmeaDataFile.path).bufferedReader()
        // read the nmea file contents using a buffered reader and store the mock data sentences in a list
        bufferedReader.useLines { bufferReaderLines ->
            // add carriage return for nmea location data source parser
            nmeaSentences = bufferReaderLines.map { it + "\n" }.toList()
        }
        return nmeaSentences
    }

    /**
     * Control the start/stop status of the NMEA location data source
     */
    fun playPauseClick(view: View) = lifecycleScope.launch {
        if (nmeaLocationDataSource.status.value != LocationDataSourceStatus.Started) {
            // initialize the location data source and prepare to begin receiving location updates when data is pushed
            // as updates are received, they will be displayed on the map
            nmeaLocationDataSource.start().onFailure {
                showError("NmeaLocationDataSource failed to start: ${it.message}")
                return@launch
            }
            // starts the NMEA mock data sentences
            nmeaSentences?.let { startNMEAMockData(it) }
            setButtonStatus(true)
        } else {
            // stop receiving and displaying location data
            nmeaLocationDataSource.stop()
            // cancel up the timer task
            timer.cancel()
            setButtonStatus(false)
            clearUI()
        }
    }

    /**
     * Initializes the location data source, reads the mock data NMEA sentences, and displays location updates from that file
     * on the location display. Data is pushed to the data source using a timeline to simulate live updates, as they would
     * appear if using real-time data from a GPS dongle
     */

    /**
     * Push the mock data NMEA sentences into the data source every 250 ms
     */
    private fun startNMEAMockData(nmeaSentences: List<String>) {
        timer = Timer()
        timer.schedule(timerTask {
            // only push data when started
            if (nmeaLocationDataSource.status.value == LocationDataSourceStatus.Started)
                nmeaLocationDataSource.pushData(
                    nmeaSentences[locationIndex++].toByteArray(StandardCharsets.UTF_8)
                )
            // reset the location index after the last data point is reached
            if (locationIndex == nmeaSentences.size) locationIndex = 0
        }, 250, 250)
    }

    /**
     * Sets the FAB button to "Start"/"Stop" based on [isShowingLocation]
     */
    private fun setButtonStatus(isShowingLocation: Boolean) = if (isShowingLocation) {
        playPauseFAB.setImageDrawable(
            AppCompatResources.getDrawable(
                this, R.drawable.ic_round_pause_24
            )
        )
    } else {
        playPauseFAB.setImageDrawable(
            AppCompatResources.getDrawable(
                this, R.drawable.ic_round_play_arrow_24
            )
        )
    }

    /**
     * Collects location changes of the NMEA location data source,
     * and displays the location accuracy
     */
    private fun collectLocationChanges() = lifecycleScope.launch {
        nmeaLocationDataSource.locationChanged.collect { nmeaLocation ->
            // convert from meters to foot
            val horizontalAccuracy = nmeaLocation.horizontalAccuracy * 3.28084
            val verticalAccuracy = nmeaLocation.verticalAccuracy * 3.28084
            accuracyTV.text =
                getString(R.string.accuracy) + "Horizontal-%.1fft, Vertical-%.1fft".format(
                    horizontalAccuracy, verticalAccuracy
                )
        }
    }

    /**
     * Obtains NMEA satellite information from the NMEA location data source,
     * and displays satellite information on the app
     */
    private fun collectSatelliteChanges() = lifecycleScope.launch {
        nmeaLocationDataSource.satellitesChanged.collect { nmeaSatelliteInfoList ->
            // set the text of the satellite count label
            satelliteCountTV.text = getString(R.string.satellite_count) + nmeaSatelliteInfoList.size
            // get the system of the first satellite
            val satelliteSystems = when (nmeaSatelliteInfoList.first().system) {
                NmeaGnssSystem.Bds -> "BDS"
                NmeaGnssSystem.Galileo -> "Galileo"
                NmeaGnssSystem.Glonass -> "Glonass"
                NmeaGnssSystem.Gps -> "GPS"
                NmeaGnssSystem.NavIc -> "NavIc"
                NmeaGnssSystem.Qzss -> "Qzss"
                NmeaGnssSystem.Unknown -> "Unknown"
            }
            // get the satellite IDs from the info list
            val uniqueSatelliteIDs = nmeaSatelliteInfoList.map { it.id }
            // display the satellite system and id information
            systemTypeTV.text = getString(R.string.system) + satelliteSystems
            satelliteIDsTV.text = getString(R.string.satellite_ids) + uniqueSatelliteIDs
        }
    }

    /**
     * Clears out the info messages when LocationDataSource is paused.
     */
    private fun clearUI() {
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
