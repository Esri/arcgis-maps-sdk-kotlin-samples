/*
 * Copyright 2023 Esri
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
import com.arcgismaps.geotriggers.FeatureFenceParameters
import com.arcgismaps.geotriggers.FenceGeotrigger
import com.arcgismaps.geotriggers.FenceRuleType
import com.arcgismaps.geotriggers.GeotriggerMonitor
import com.arcgismaps.geotriggers.GeotriggerNotificationInfo
import com.arcgismaps.geotriggers.LocationGeotriggerFeed
import com.arcgismaps.geotriggers.FenceGeotriggerNotificationInfo
import com.arcgismaps.geotriggers.FenceNotificationType
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

    // recycler list view to show the the points of interest
    private val poiListView by lazy {
        activityMainBinding.poiListView
    }

    // custom list adapter for the points of interest
    private val poiListAdapter by lazy {
        // create a new feature list adapter from the poiList
        FeatureListAdapter(poiList) { feature ->
            // set the item callback to show the feature view fragment
            showFeatureViewFragment(feature)
        }
    }

    private val simulatedLocationDataSource: SimulatedLocationDataSource by lazy {
        // create SimulationParameters starting at the current time,
        // a velocity of 10 m/s, and a horizontal and vertical accuracy of 0.0
        val simulationParameters = SimulationParameters(
            Clock.System.now(),
            3.0,
            0.0,
            0.0
        )
        SimulatedLocationDataSource().apply {
            // use the polyline as defined above or from this ArcGIS Online GeoJSON to define the path. retrieved
            // from https://https://arcgisruntime.maps.arcgis.com/home/item.html?id=2a346cf1668d4564b8413382ae98a956
            setLocationsWithPolyline(
                Geometry.fromJson(getString(R.string.polyline_json)) as Polyline,
                simulationParameters
            )
        }
    }

    // feature list to store the points of interest of a geotrigger
    private val poiList = mutableListOf<ArcGISFeature>()

    // geotrigger names for the geotrigger monitors
    private val sectionGeoTrigger: String = "Section Geotrigger"
    private val poiGeoTrigger: String = "POI Geotrigger"

    // make monitors properties to prevent garbage collection
    private lateinit var sectionGeotriggerMonitor: GeotriggerMonitor
    private lateinit var poiGeotriggerMonitor: GeotriggerMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        val portal = Portal("https://www.arcgis.com")
        // this sample uses a web map with a predefined tile basemap, feature styles, and labels
        val map = ArcGISMap(PortalItem(portal, "6ab0e91dc39e478cae4f408e1a36a308"))
        // set the mapview's map
        mapView.map = map
        // set the map to simulate the location data source
        mapView.locationDisplay.apply {
            dataSource = simulatedLocationDataSource
            setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
            initialZoomScale = 1000.0
        }

        // instantiate the service feature tables to later create GeotriggerMonitors for
        val gardenSections =
            ServiceFeatureTable(PortalItem(portal, "1ba816341ea04243832136379b8951d9"), 0)
        val gardenPOIs =
            ServiceFeatureTable(PortalItem(portal, "7c6280c290c34ae8aeb6b5c4ec841167"), 0)
        // create Geotriggers for each of the service feature tables
        sectionGeotriggerMonitor =
            createGeotriggerMonitor(gardenSections, 0.0, sectionGeoTrigger)
        poiGeotriggerMonitor =
            createGeotriggerMonitor(gardenPOIs, 10.0, poiGeoTrigger)

        // play or pause the simulation data source when the FAB is clicked
        playPauseFAB.setOnClickListener {
            when (simulatedLocationDataSource.status.value) {
                LocationDataSourceStatus.Started -> {
                    stopSimulatedDataSource()
                }
                LocationDataSourceStatus.Stopped -> {
                    startSimulatedDataSource()
                }
                else -> {
                    // show an error if the status is anything else
                    showError(
                        "Error modifying location data source state: " +
                            "${simulatedLocationDataSource.status.value}"
                    )
                }
            }
        }

        // set the recycler view layout to a vertical linear layout
        poiListView.layoutManager = LinearLayoutManager(this)
        // assign its adapter
        poiListView.adapter = poiListAdapter

        lifecycleScope.launch {
            // wait for the map load
            map.load().onFailure {
                // if the map load fails, show the error and return
                showError("Error loading map: ${it.message}")
                return@launch
            }
            // start the section geotrigger monitor
            sectionGeotriggerMonitor.start().onFailure {
                // if the monitor start fails, show the error and return
                showError("Section Geotrigger Monitor failed to start: ${it.message}")
                return@launch
            }
            // start the points of interest geotrigger monitor
            poiGeotriggerMonitor.start().onFailure {
                // if the monitor start fails, show the error and return
                showError("POI Geotrigger Monitor failed to start: ${it.message}")
                return@launch
            }
            // finally, start the simulated location data source
            simulatedLocationDataSource.start().onFailure {
                // if it fails, show the error and return
                showError("Simulated Location DataSource failed to start: ${it.message}")
            }
        }
    }

    /**
     * Creates and returns a geotrigger monitor with the [geotriggerName] name,
     * using the [serviceFeatureTable] and [bufferSize] to initialize
     * FeatureFenceParameters for the geotrigger
     */
    private fun createGeotriggerMonitor(
        serviceFeatureTable: ServiceFeatureTable,
        bufferSize: Double,
        geotriggerName: String
    ): GeotriggerMonitor {
        // create a LocationGeotriggerFeed that uses the SimulatedLocationDataSource
        val geotriggerFeed = LocationGeotriggerFeed(simulatedLocationDataSource)
        // initialize FeatureFenceParameters to display the section the user has entered
        val featureFenceParameters = FeatureFenceParameters(serviceFeatureTable, bufferSize)
        // create a fence geotrigger
        val fenceGeotrigger = FenceGeotrigger(
            geotriggerFeed,
            // triggers on enter/exit
            FenceRuleType.EnterOrExit,
            featureFenceParameters,
            // arcade expression to get the feature name
            ArcadeExpression("\$fenceFeature.name"),
            geotriggerName
        )

        // initialize a geotrigger monitor with the fence geotrigger
        val geotriggerMonitor = GeotriggerMonitor(fenceGeotrigger)
        lifecycleScope.launch {
            // capture and handle Geotrigger notification based on the FenceRuleType
            // hence, triggers on fence enter/exit.
            geotriggerMonitor.geotriggerNotificationEvent.collect {
                handleGeotriggerNotification(it)
            }
        }
        return geotriggerMonitor
    }

    /**
     * Handles the [geotriggerNotificationInfo] based on its geotrigger type
     * and FenceNotificationType
     */
    private fun handleGeotriggerNotification(geotriggerNotificationInfo: GeotriggerNotificationInfo) {
        // cast it to FenceGeotriggerNotificationInfo which provides
        // access to the feature that triggered the notification
        val fenceGeotriggerNotificationInfo =
            geotriggerNotificationInfo as FenceGeotriggerNotificationInfo
        //  name of the fence feature, returned from the set arcade expression
        val fenceFeatureName = fenceGeotriggerNotificationInfo.message
        // get the specific geotrigger name we set during initialization
        val geoTriggerType = fenceGeotriggerNotificationInfo.geotriggerMonitor.geotrigger.name
        // check for the type of notification
        when (fenceGeotriggerNotificationInfo.fenceNotificationType) {
            FenceNotificationType.Entered -> {
                // if the user location entered the geofence, add the feature information to the UI
                addFeatureInformation(
                    fenceFeatureName,
                    geoTriggerType,
                    fenceGeotriggerNotificationInfo.fenceGeoElement as ArcGISFeature
                )
            }
            FenceNotificationType.Exited -> {
                // if the user exits a given geofence, remove the feature's information from the UI
                removeFeatureInformation(fenceFeatureName, geoTriggerType)
            }
        }
    }

    /**
     * Adds the [fenceFeature] ArcGISFeature with the [fenceFeatureName] and [geoTriggerType] to the current UI state
     * and refreshes the UI
     */
    private fun addFeatureInformation(
        fenceFeatureName: String,
        geoTriggerType: String,
        fenceFeature: ArcGISFeature
    ) {
        // recenter the mapview
        mapView.locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)

        when (geoTriggerType) {
            // if it's a section geo trigger type
            sectionGeoTrigger -> {
                // update the section button's onClickListener
                // to show a new FeatureViewFragment
                sectionButton.setOnClickListener { showFeatureViewFragment(fenceFeature) }
                // update the section button text to the feature name
                sectionButton.text = fenceFeatureName
                // enable the button
                sectionButton.isEnabled = true
            }
            // or a point of interest geo trigger
            poiGeoTrigger -> {
                // add it to the stored list
                poiList.add(fenceFeature)
                // notify the list adapter to refresh its recycler views
                poiListAdapter.notifyItemInserted(poiList.lastIndex)
            }
        }
    }

    /**
     * Removes the ArcGISFeature with the given [fenceFeatureName] and corresponding
     * [geoTriggerType] from the current UI state and refreshes the UI.
     */
    private fun removeFeatureInformation(fenceFeatureName: String, geoTriggerType: String) {
        // check the type of geotrigger
        when (geoTriggerType) {
            sectionGeoTrigger -> {
                // if it's a section geo trigger,
                // remove the section information and disable the button
                sectionButton.text = "N/A"
                sectionButton.isEnabled = false
            }
            poiGeoTrigger -> {
                // if it's a point of interest geotrigger
                // find its index from the stored list
                val index = poiList.indexOfFirst { feature ->
                    feature.attributes["name"] == fenceFeatureName
                }
                if (index >= 0) {
                    // if the feature exists remove it
                    poiList.removeAt(index)
                    // notify the list adapter to refresh its recycler views
                    poiListAdapter.notifyItemRemoved(index)
                }
            }
        }
    }

    /**
     * Creates and shows a new FeatureViewFragment using the given [feature]
     */
    private fun showFeatureViewFragment(feature: ArcGISFeature) {
        // stop the simulated data source
        stopSimulatedDataSource(false)
        // create a new FeatureViewFragment
        val featureViewFragment = FeatureViewFragment(feature) {
            // set it's onDismissedListener to
            // resume the simulated data source
            startSimulatedDataSource(false)
        }
        // show the fragment
        featureViewFragment.show(supportFragmentManager, "FeatureViewFragment")
    }

    /**
     * Starts the simulated data source and shows a status toast if [showAlert] is true.
     * The data source is resumed from its previous location if stopped before.
     */
    private fun startSimulatedDataSource(showAlert: Boolean = true) = lifecycleScope.launch {
        // start the simulated location data source
        simulatedLocationDataSource.start()
        // recenter the map view
        mapView.locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
        // show a toast if true
        if (showAlert) Toast.makeText(this@MainActivity, "Resumed Simulation", Toast.LENGTH_SHORT)
            .show()
        // update the action button's drawable to a pause icon
        playPauseFAB.setImageResource(R.drawable.ic_baseline_pause_24)
    }

    /**
     * Stops the simulated data source and shows a status toast if [showAlert] is true.
     */
    private fun stopSimulatedDataSource(showAlert: Boolean = true) = lifecycleScope.launch {
        // stop the simulated location data source
        simulatedLocationDataSource.stop()
        // show a toast if true
        if (showAlert) Toast.makeText(this@MainActivity, "Stopped Simulation", Toast.LENGTH_SHORT)
            .show()
        // update the action button's drawable to a play icon
        playPauseFAB.setImageResource(R.drawable.ic_baseline_play_arrow_24)
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}