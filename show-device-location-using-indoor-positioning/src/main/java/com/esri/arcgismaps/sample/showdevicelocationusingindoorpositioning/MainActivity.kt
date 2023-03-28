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

package com.esri.arcgismaps.sample.showdevicelocationusingindoorpositioning

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Guid
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.data.FeatureTable
import com.arcgismaps.data.Field
import com.arcgismaps.data.OrderBy
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.data.SortOrder
import com.arcgismaps.location.IndoorsLocationDataSource
import com.arcgismaps.location.Location
import com.arcgismaps.location.LocationDataSourceStatus
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.portal.Portal
import com.arcgismaps.mapping.PortalItem
import com.esri.arcgismaps.sample.showdevicelocationusingindoorpositioning.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val progressBar by lazy {
        activityMainBinding.progressBar
    }

    private val textView by lazy {
        activityMainBinding.textView
    }

    // keep track of the current floor in an indoor map, null if using GPS
    private var currentFloor: Int? = null

    // provides an indoor or outdoor position based on device sensor data (radio, GPS, motion sensors).
    private var indoorsLocationDataSource: IndoorsLocationDataSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(mapView)
        // some parts of the API require an Android Context to properly interact with Android system
        // features, such as LocationProvider and application resources
        ArcGISEnvironment.applicationContext = applicationContext
        // check for location permissions
        // if permissions is allowed, the device's current location is shown
        checkPermissions()
    }

    /**
     * Check for location permissions, if not received then request for one
     */
    private fun checkPermissions() {
        val requestCode = 1
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
        ) {
            val requestPermissions = mutableListOf( Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
            // Android 12 required permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            ActivityCompat.requestPermissions(this, requestPermissions.toTypedArray(), requestCode)
        } else {
            // permission already given, so no need to request
            setUpMap()
        }
    }

    /**
     * Set up the [mapView] to load a floor-aware web map.
     */
    private fun setUpMap() {
        // load the portal and create a map from the portal item
        val portalItem = PortalItem(
            Portal("https://www.arcgis.com/"),
            "8fa941613b4b4b2b8a34ad4cdc3e4bba"
        )
        val map = ArcGISMap(portalItem)
        mapView.map = map
        lifecycleScope.launch {
            map.load().onSuccess {
                val featureTables = map.tables
                // check if the portalItem contains featureTables
                if (featureTables.isNotEmpty()) {
                    setUpLoadTables(featureTables)
                } else {
                    showError("Map does not contain feature tables")
                }
            }.onFailure {
                // if map load failed, show the error
                showError("Error Loading Map: {it.message}")
            }
        }
    }

    /**
     * Load each feature table and setup the IndoorsLocationDataSource for each loaded table
     */
    private suspend fun setUpLoadTables(featureTables: MutableList<FeatureTable>) {
            featureTables.forEach { featureTable ->
                // load each FeatureTable
                featureTable.load().onSuccess {
                    setupIndoorsLocationDataSource(featureTable)
                }.onFailure {
                    showError("Error loading FeatureTable: ${it.message}")
                }
            }
    }

    /**
     * Sets up the [indoorsLocationDataSource] using the IPS_Positioning [featureTable]
     */
    private fun setupIndoorsLocationDataSource(featureTable: FeatureTable) {
        if (featureTable.tableName != "IPS_Positioning") {
            showError("Positioning Table not found in FeatureTables")
            return
        } // IPS_Positioning table needs to be present
        else if (featureTable.tableName == "IPS_Positioning") {
            // cast the featureTable to a ServiceFeatureTable to get the globalIdField which identifies a row in the positioning table
            // and used as a parameter to setup IndoorsLocationDataSource
            val positioningFeatureTable = featureTable as ServiceFeatureTable
            // when multiple entries are available, IndoorsLocationDataSource constructor function
            // looks up the entry with the most recent date and takes this positioning data
            // set up queryParameters to grab one result.
            val dateCreatedFieldName = getDateCreatedFieldName(positioningFeatureTable.fields)
                ?: return showError("The service table does not contain \"DateCreated\" fields.")
            val queryParameters = QueryParameters().apply {
                // set a limit of 1 on the number of returned features per request
                maxFeatures = 1
                // 1=1 is a true where clause which will result in all matching records being returned
                whereClause = "1 = 1"
                // find and sort out the orderByFields by most recent first
                orderByFields.add(
                    OrderBy(
                        dateCreatedFieldName,
                        sortOrder = SortOrder.Descending
                    )
                )
            }
            lifecycleScope.launch {
                positioningFeatureTable.queryFeatures(queryParameters)
                    .onSuccess { queryResults ->
                        val featureResult = queryResults.first()
                        // perform search query using the queryParameters
                        // check if serviceFeatureTable contains positioning data
                        // The ID that identifies a row in the positioning table.
                        val globalID =
                            featureResult.attributes[positioningFeatureTable.globalIdField].toString()
                        val positioningId = Guid(globalID)
                        // Setting up IndoorsLocationDataSource with positioning, pathways tables and positioning ID.
                        // positioningTable - the "ips_positioning" feature table from an IPS-enabled map.
                        // pathwaysTable - An ArcGISFeatureTable that contains pathways as per the ArcGIS Indoors Information Model.
                        // Setting this property enables path snapping of locations provided by the IndoorsLocationDataSource.
                        // levelsTable - An ArcGISFeatureTable that contains floor levels in accordance with the ArcGIS Indoors Information Model.
                        // Providing this table enables the retrieval of a location's floor level ID.
                        // positioningID - an ID which identifies a specific row in the positioningTable that should be used for setting up IPS.
                        indoorsLocationDataSource = IndoorsLocationDataSource(
                            positioningFeatureTable,
                            getFeatureTable("Pathways"),
                            getFeatureTable("Levels"),
                            positioningId
                        )
                        // start the location display (blue dot)
                        startLocationDisplay()
                    }.onFailure {
                        showError("The positioning table contain no data")
                    }
            }
        }
    }

    /**
     * Find the exact formatting of the name "DateCreated" in the list of ServiceFeatureTable [fields].
     */
    private fun getDateCreatedFieldName(fields: List<Field>): String? {
        val field = fields.find {
            it.name.equals(
                "DateCreated",
                ignoreCase = true
            ) || it.name.equals("Date_Created", ignoreCase = true)
        }
        return field?.name
    }

    /**
     * Retrieves the "Pathways" or the "levels" table depending on the string passed.
     */
    private fun getFeatureTable(name: String): ArcGISFeatureTable? {
        return mapView.map?.operationalLayers?.firstOrNull { operationalLayer ->
            operationalLayer.name == name
        }?.let { layer ->
            if (layer is FeatureLayer) {
                layer.featureTable as ArcGISFeatureTable
            } else {
                null
            }
        } ?: run {
            showError("$name table not found")
            null
        }
    }

    /**
     * Sets up the location listeners, the navigation mode, displays the devices location as a blue dot,
     * collect data source location changes and handles its status changes
     */
    private fun startLocationDisplay() {
        val locationDisplay = mapView.locationDisplay.apply {
            setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
            dataSource = indoorsLocationDataSource
                ?: return showError("Error setting the IndoorsLocationDataSource value.")
        }

        // coroutine scope to start the location display, which will in-turn start IndoorsLocationDataSource to start receiving IPS updates.
        lifecycleScope.launch {
            locationDisplay.dataSource.start()
        }

        // coroutine scope to collect data source location changes like currentFloor, positionSource, transmitterCount, networkCount and horizontalAccuracy
        lifecycleScope.launch {
            locationDisplay.dataSource.locationChanged.collect { location ->
                // get the location properties of the LocationDataSource
                val locationProperties = location.additionalSourceProperties
                // retrieve information about the location of the device
                val floor = locationProperties["floor"]?.toString() ?: ""
                val positionSource = locationProperties["positionSource"]?.toString() ?: ""
                val transmitterCount = locationProperties["transmitterCount"]?.toString() ?: ""
                val satelliteCount = locationProperties["satelliteCount"]?.toString() ?: ""

                // check if current floor hasn't been set or if the floor has changed
                if (floor.isNotEmpty()) {
                    val newFloor = floor.toInt()
                    if (currentFloor == null || currentFloor != newFloor) {
                        currentFloor = newFloor
                        // update layer's definition express with the current floor
                        mapView.map?.operationalLayers?.forEach { layer ->
                            val name = layer.name
                            if (layer is FeatureLayer && name in listOf("Details", "Units", "Levels")) {
                                layer.definitionExpression = "VERTICAL_ORDER = $currentFloor"
                            }
                        }
                    }
                } else {
                    showError("Floors is empty.")
                }
                // set up the message with floor properties to be displayed to the textView
                val sb = StringBuilder()
                sb.append("Floor: $floor, ")
                sb.append("Position-source: $positionSource, ")
                val accuracy = DecimalFormat(".##").format(
                    location.horizontalAccuracy
                )
                sb.append("Horizontal-accuracy: ${accuracy}m, ")
                sb.append(when (positionSource) {
                    Location.SourceProperties.Values.POSITION_SOURCE_GNSS -> "Satellite-count: $satelliteCount"
                    "BLE" -> "Transmitter-count: $transmitterCount"
                    else -> ""
                })
                textView.text = sb.toString()
            }
        }

        lifecycleScope.launch {
            // Handle status changes of IndoorsLocationDataSource
            locationDisplay.dataSource.status.collect { status ->
                when (status) {
                    LocationDataSourceStatus.Starting -> progressBar.visibility = View.VISIBLE
                    LocationDataSourceStatus.Started -> progressBar.visibility = View.GONE
                    LocationDataSourceStatus.FailedToStart -> {
                        progressBar.visibility = View.GONE
                        showError("Failed to start IndoorsLocationDataSource")
                    }
                    LocationDataSourceStatus.Stopped -> {
                        progressBar.visibility = View.GONE
                        showError("IndoorsLocationDataSource stopped due to an internal error")
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Result of the user from location permissions request
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // if location permissions accepted, start setting up IndoorsLocationDataSource
            setUpMap()
        } else {
            val message = "Location permission is not granted"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            Log.e(localClassName, message)
            progressBar.visibility = View.GONE
        }
    }

    /**
     * Displays an error onscreen
     */
    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}

