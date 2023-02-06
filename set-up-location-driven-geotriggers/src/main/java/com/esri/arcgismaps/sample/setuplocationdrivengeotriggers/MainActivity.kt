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

package com.esri.arcgismaps.sample.setuplocationdrivengeotriggers

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.arcade.ArcadeExpression
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geotriggers.FeatureFenceParameters
import com.arcgismaps.geotriggers.FenceGeotrigger
import com.arcgismaps.geotriggers.FenceRuleType
import com.arcgismaps.geotriggers.GeotriggerMonitor
import com.arcgismaps.geotriggers.GeotriggerNotificationInfo
import com.arcgismaps.geotriggers.LocationGeotriggerFeed
import com.arcgismaps.location.LocationDataSourceStatus
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.location.SimulatedLocationDataSource
import com.arcgismaps.location.SimulationParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.portal.Portal
import com.arcgismaps.portal.PortalItem
import com.esri.arcgismaps.sample.setuplocationdrivengeotriggers.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val playPauseFAB: FloatingActionButton by lazy {
        activityMainBinding.playPauseFAB
    }

    private val simulatedLocationDataSource: SimulatedLocationDataSource by lazy {
        val simulatedLocationDataSource = SimulatedLocationDataSource()
        // Create SimulationParameters starting at the current time, a velocity of 10 m/s, and a horizontal and vertical accuracy of 0.0
        val simulationParameters = SimulationParameters(Clock.System.now(), 3.0, 0.0, 0.0)
        // Use the polyline as defined above or from this ArcGIS Online GeoJSON to define the path. retrieved
        // from https://https://arcgisruntime.maps.arcgis.com/home/item.html?id=2a346cf1668d4564b8413382ae98a956
        simulatedLocationDataSource.setLocationsWithPolyline(
            Geometry.fromJson(getString(R.string.polyline_json)) as Polyline,
            simulationParameters
        )
        simulatedLocationDataSource
    }

    // Make monitors properties to prevent garbage collection
    private lateinit var sectionGeotriggerMonitor: GeotriggerMonitor
    private lateinit var poiGeotriggerMonitor: GeotriggerMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        val portal = Portal("https://www.arcgis.com")
        // This sample uses a web map with a predefined tile basemap, feature styles, and labels
        mapView.map = ArcGISMap(PortalItem(portal, "6ab0e91dc39e478cae4f408e1a36a308"))
        // Set up the location display and start the simulated location data source
        mapView.locationDisplay.apply {
            dataSource = simulatedLocationDataSource
            setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
            initialZoomScale = 1000.0
        }
        lifecycleScope.launch { simulatedLocationDataSource.start() }

        // Play or pause the simulation data source when the FAB is clicked
        playPauseFAB.setOnClickListener {
            lifecycleScope.launch {
                if (simulatedLocationDataSource.status.value == LocationDataSourceStatus.Started) {
                    simulatedLocationDataSource.stop()
                    Toast.makeText(this@MainActivity, "Stopped Simulation", Toast.LENGTH_SHORT).show()
                    playPauseFAB.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                } else {
                    simulatedLocationDataSource.start()
                    mapView.locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
                    Toast.makeText(this@MainActivity, "Resumed Simulation", Toast.LENGTH_SHORT).show()
                    playPauseFAB.setImageResource(R.drawable.ic_baseline_pause_24)
                }
            }
        }

        // Instantiate the service feature tables to later create GeotriggerMonitors for
        val gardenSections =
            ServiceFeatureTable(PortalItem(portal, "1ba816341ea04243832136379b8951d9"), 0)
        val gardenPOIs =
            ServiceFeatureTable(PortalItem(portal, "7c6280c290c34ae8aeb6b5c4ec841167"), 0)
        // Create Geotriggers for each of the service feature tables
        sectionGeotriggerMonitor =
            createGeotriggerMonitor(gardenSections, 0.0, "Section Geotrigger")
        poiGeotriggerMonitor =
            createGeotriggerMonitor(gardenPOIs, 10.0, "POI Geotrigger")
        lifecycleScope.launch {
            sectionGeotriggerMonitor.start().onFailure {
                showError("Section Geotrigger Monitor failed to start: ${it.message}")
            }
            poiGeotriggerMonitor.start().onFailure {
                showError("POI Geotrigger Monitor failed to start: ${it.message}")
            }
        }
    }

    private fun createGeotriggerMonitor(
        serviceFeatureTable: ServiceFeatureTable,
        bufferSize: Double,
        geotriggerName: String
    ): GeotriggerMonitor {
        // Create a LocationGeotriggerFeed that uses the SimulatedLocationDataSource
        val geotriggerFeed = LocationGeotriggerFeed(simulatedLocationDataSource)
        // Initialize FeatureFenceParameters with the service feature table and a buffer of 0 meters
        // to display the exact garden section the user has entered
        val featureFenceParameters = FeatureFenceParameters(serviceFeatureTable, bufferSize)
        val fenceGeotrigger = FenceGeotrigger(
            geotriggerFeed,
            FenceRuleType.EnterOrExit,
            featureFenceParameters,
            ArcadeExpression("\$fenceFeature.name"),
            geotriggerName
        )

        // Handles Geotrigger notification based on the FenceRuleType
        // Hence, triggers on fence enter/exit.
        val geotriggerMonitor = GeotriggerMonitor(fenceGeotrigger)
        lifecycleScope.launch {
            geotriggerMonitor.geotriggerNotificationEvent.collect {
                handleGeotriggerNotification(it)
            }
        }
        return geotriggerMonitor
    }

    private fun handleGeotriggerNotification(geotriggerNotificationInfo: GeotriggerNotificationInfo) {
        showError(geotriggerNotificationInfo.message)
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
