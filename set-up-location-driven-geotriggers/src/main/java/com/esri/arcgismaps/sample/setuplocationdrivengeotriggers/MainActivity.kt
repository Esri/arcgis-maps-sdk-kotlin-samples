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
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.arcade.ArcadeExpression
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geotriggers.*
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

    private val sectionButton: Button by lazy {
        activityMainBinding.sectionButton
    }

    private val poiListView by lazy {
        activityMainBinding.poiListView
    }

    private val poiListAdapter by lazy {
        FeatureListAdapter(poiList)
    }

    private val poiList = mutableListOf<ArcGISFeature>()

    private val visitedFeatures = mutableMapOf<String, ArcGISFeature>()

    private val simulatedLocationDataSource: SimulatedLocationDataSource by lazy {
        // Create SimulationParameters starting at the current time,
        // a velocity of 10 m/s, and a horizontal and vertical accuracy of 0.0
        val simulationParameters = SimulationParameters(
            Clock.System.now(),
            3.0,
            0.0,
            0.0
        )
        SimulatedLocationDataSource().apply {
            // Use the polyline as defined above or from this ArcGIS Online GeoJSON to define the path. retrieved
            // from https://https://arcgisruntime.maps.arcgis.com/home/item.html?id=2a346cf1668d4564b8413382ae98a956
            setLocationsWithPolyline(
                Geometry.fromJson(getString(R.string.polyline_json)) as Polyline,
                simulationParameters
            )
        }
    }

    private val sectionGeoTriggerName: String = "Section Geotrigger"
    private val poiGeoTriggerName: String = "POI Geotrigger"

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
        val map = ArcGISMap(PortalItem(portal, "6ab0e91dc39e478cae4f408e1a36a308"))
        mapView.map = map
        // Set up the location display and start the simulated location data source
        mapView.locationDisplay.apply {
            dataSource = simulatedLocationDataSource
            setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
            initialZoomScale = 1000.0
        }

        // Instantiate the service feature tables to later create GeotriggerMonitors for
        val gardenSections =
            ServiceFeatureTable(PortalItem(portal, "1ba816341ea04243832136379b8951d9"), 0)
        val gardenPOIs =
            ServiceFeatureTable(PortalItem(portal, "7c6280c290c34ae8aeb6b5c4ec841167"), 0)
        // Create Geotriggers for each of the service feature tables
        sectionGeotriggerMonitor =
            createGeotriggerMonitor(gardenSections, 0.0, sectionGeoTriggerName)
        poiGeotriggerMonitor =
            createGeotriggerMonitor(gardenPOIs, 10.0, poiGeoTriggerName)

        lifecycleScope.launch {
            map.load().onFailure {
                return@launch
            }
            sectionGeotriggerMonitor.start().onFailure {
                showError("Section Geotrigger Monitor failed to start: ${it.message}")
                return@launch
            }
            poiGeotriggerMonitor.start().onFailure {
                showError("POI Geotrigger Monitor failed to start: ${it.message}")
                return@launch
            }
            simulatedLocationDataSource.start().onFailure {
                showError("Simulated Location DataSource failed to start: ${it.message}")
                return@launch
            }
        }

        // Play or pause the simulation data source when the FAB is clicked
        playPauseFAB.setOnClickListener {
            if (simulatedLocationDataSource.status.value == LocationDataSourceStatus.Started) {
                stopSimulatedDataSource()
            } else {
                startSimulatedDataSource()
            }
        }

        poiListView.layoutManager = LinearLayoutManager(this)
        poiListAdapter.setOnItemClickListener { arcGISFeature ->
            stopSimulatedDataSource()
            FeatureViewFragment(arcGISFeature).apply {
                setOnDismissListener {
                    startSimulatedDataSource()
                }
                show(supportFragmentManager, "FeatureView")
            }
        }
        poiListView.adapter = poiListAdapter
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
        val fenceGeotriggerNotificationInfo =
            geotriggerNotificationInfo as FenceGeotriggerNotificationInfo
        // returned from arcade expression
        val fenceFeatureName = fenceGeotriggerNotificationInfo.message
        val geoTriggerType = fenceGeotriggerNotificationInfo.geotriggerMonitor.geotrigger.name
        when (fenceGeotriggerNotificationInfo.fenceNotificationType) {
            FenceNotificationType.Entered -> {
                addFeatureInformation(
                    fenceFeatureName,
                    geoTriggerType,
                    fenceGeotriggerNotificationInfo.fenceGeoElement as ArcGISFeature
                )
            }
            FenceNotificationType.Exited -> {
                removeFeatureInformation(fenceFeatureName, geoTriggerType)
            }
        }
    }

    private fun addFeatureInformation(
        fenceFeatureName: String,
        geoTriggerType: String,
        fenceFeature: ArcGISFeature
    ) {
        mapView.locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)

        if (geoTriggerType == sectionGeoTriggerName) {
            Log.d(TAG, "addFeatureInformation: ${fenceFeature.attributes.keys}")
            if (!visitedFeatures.containsKey(fenceFeatureName)) {
                // val featureSection = FeatureSection(fenceFeature)
                // lifecycleScope.launch { featureSection.load() }
                visitedFeatures[fenceFeatureName] = fenceFeature
            }

            // update the UI
            sectionButton.setOnClickListener {
                stopSimulatedDataSource()
                FeatureViewFragment(fenceFeature).apply {
                    setOnDismissListener {
                        startSimulatedDataSource()
                    }
                    show(supportFragmentManager, "FeatureView")
                }
            }
            sectionButton.text = fenceFeatureName
            sectionButton.isEnabled = true
        } else {
            // TO DO POI List
            poiList.add(fenceFeature)
            poiListAdapter.notifyDataSetChanged()
        }
    }

    private fun removeFeatureInformation(fenceFeatureName: String, geoTriggerType: String) {
        if (geoTriggerType == sectionGeoTriggerName) {
            sectionButton.text = "N/A"
            sectionButton.isEnabled = false
        } else {
            // TO DO POI List
            poiList.removeIf { it.attributes["name"] == fenceFeatureName }
            poiListAdapter.notifyDataSetChanged()
        }
    }

    private fun stopSimulatedDataSource() = lifecycleScope.launch {
        simulatedLocationDataSource.stop()
        Toast.makeText(this@MainActivity, "Stopped Simulation", Toast.LENGTH_SHORT)
            .show()
        playPauseFAB.setImageResource(R.drawable.ic_baseline_play_arrow_24)
    }

    private fun startSimulatedDataSource() = lifecycleScope.launch {
        simulatedLocationDataSource.start()
        mapView.locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
        Toast.makeText(this@MainActivity, "Resumed Simulation", Toast.LENGTH_SHORT)
            .show()
        playPauseFAB.setImageResource(R.drawable.ic_baseline_pause_24)
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}