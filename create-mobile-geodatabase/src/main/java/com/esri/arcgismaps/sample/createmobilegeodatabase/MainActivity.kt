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

package com.esri.arcgismaps.sample.createmobilegeodatabase

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.LoadStatus
import com.arcgismaps.data.FieldDescription
import com.arcgismaps.data.FieldType
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.data.GeodatabaseFeatureTable
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.TableDescription
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.esri.arcgismaps.sample.createmobilegeodatabase.databinding.ActivityMainBinding
import com.esri.arcgismaps.sample.createmobilegeodatabase.databinding.TableLayoutBinding
import com.esri.arcgismaps.sample.createmobilegeodatabase.databinding.TableRowBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private val provisionPath: String by lazy {
        getExternalFilesDir(null)?.path.toString() + File.separator + getString(R.string.app_name)
    }

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val createButton: Button by lazy {
        activityMainBinding.createButton
    }

    private val viewTableButton: Button by lazy {
        activityMainBinding.viewTableButton
    }

    private val featureCount: TextView by lazy {
        activityMainBinding.featureCount
    }

    // feature table created using mobile geodatabase and added to the MapView
    private var featureTable: GeodatabaseFeatureTable? = null

    // mobile geodatabase used to create and store the feature attributes (LocationHistory.geodatabase)
    private var geodatabase: Geodatabase? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISTopographic)

        mapView.setViewpoint(Viewpoint(34.056295, -117.195800, 10000.0))
        mapView.map = map

        lifecycleScope.launch {
            mapView.onSingleTapConfirmed.collect { tapConfirmedEvent ->
                val mapPoint = tapConfirmedEvent.mapPoint
                val screenCoordinate = tapConfirmedEvent.screenCoordinate

                // create a point from where the user clicked
                if(mapPoint != null)
                    addFeature(mapPoint)
            }
        }

        viewTableButton.setOnClickListener {
            lifecycleScope.launch {
                // displays table dialog with the values in the feature table
                displayTable()
            }
        }

        // opens a share-sheet with the "LocationHistory.geodatabase" file
        createButton.setOnClickListener {
            try {
                // close the mobile geodatabase before sharing
                geodatabase?.close()
                // get the URI of the geodatabase file using FileProvider
                val geodatabaseURI = FileProvider.getUriForFile(
                    this, getString(R.string.file_provider_package), File(
                        geodatabase?.path.toString()
                    )
                )
                // set up the sharing intent with the geodatabase URI
                val geodatabaseIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, geodatabaseURI)
                }
                // open the Android share sheet
                startActivity(geodatabaseIntent)
            } catch (e: Exception) {
                showError("Error sharing file: ${e.message}")
            }
        }

        resetMap()
    }


    /**
     * Create and load a new geodatabase file with TableDescription fields
     */
    private fun createGeodatabase() {
        // define the path and name of the geodatabase file
        // note: the path defined must be non-empty, available,
        // allow read/write access, and end in ".geodatabase"
        val file = File(getExternalFilesDir(null)?.path + "/LocationHistory.geodatabase")
        if (file.exists()) {
            file.delete()
        }
        // close the existing geodatabase
        geodatabase?.close()
        lifecycleScope.launch {
            // create a geodatabase file at the file path
            Geodatabase.create(file.path).onSuccess { geodatabase ->
                // construct a table description which stores features as points on map
                val tableDescription =
                    TableDescription(
                        "LocationHistory",
                        SpatialReference.wgs84(),
                        GeometryType.Point
                    )
                // set up the fields to the table,
                // Field.Type.OID is the primary key of the SQLite table
                // Field.Type.DATE is a date column used to store a Calendar date
                // FieldDescriptions can be a SHORT, INTEGER, GUID, FLOAT, DOUBLE, DATE, TEXT, OID, GLOBALID, BLOB, GEOMETRY, RASTER, or XML.
                tableDescription.fieldDescriptions.addAll(
                    listOf(
                        FieldDescription("oid", FieldType.Oid),
                        FieldDescription("collection_timestamp", FieldType.Date)
                    )
                )

                // set any properties not needed to false
                tableDescription.apply {
                    hasAttachments = false
                    hasM = false
                    hasZ = false
                }

                // add a new table to the geodatabase by creating one from the tableDescription
                val tableFuture = geodatabase.createTable(tableDescription)
                setupMapFromGeodatabase(tableFuture)
            }.onFailure {
                showError(it.message.toString())
            }

        }
    }

    /**
     * Set up the MapView to display the Feature layer
     * using the loaded [tableResult] GeodatabaseFeatureTable
     */
    private suspend fun setupMapFromGeodatabase(tableResult: Result<GeodatabaseFeatureTable>) {
        tableResult.onSuccess { featureTable ->
            // get the result of the loaded "LocationHistory" table
            this.featureTable = featureTable
            // create a feature layer for the map using the GeodatabaseFeatureTable
            val featureLayer = FeatureLayer(featureTable)
            mapView.map?.operationalLayers?.add(featureLayer)
            // display the current count of features in the FeatureTable
            featureCount.text = "Number of features added: ${featureTable.getAddedFeaturesCount().getOrNull() }}"
        }.onFailure {
            showError(it.message.toString())
        }
    }

    /**
     * Create a feature with attributes on map click and it to the [featureTable]
     * Also, updates the TotalFeatureCount on the screen
     */
    private fun addFeature(mapPoint: Point) {
        // set up the feature attributes
        val featureAttributes = mutableMapOf<String, Any>()
        featureAttributes["collection_timestamp"] = Clock.System.now()

        // create a new feature at the mapPoint
        val feature = featureTable?.createFeature(featureAttributes, mapPoint) ?: return

        lifecycleScope.launch {
            // add the feature to the feature table
            featureTable?.addFeature(feature)?.onSuccess {
                // feature added successfully, update count
                featureCount.text = "Number of features added: ${featureTable?.getAddedFeaturesCount()
                    ?.getOrNull()}"
                // enable table button since at least 1 feature loaded on the GeodatabaseFeatureTable
                viewTableButton.isEnabled = true
            }?.onFailure {
                showError(it.message.toString())
            }
        }
    }

    /**
     * Displays a dialog with the table of features
     * added to the GeodatabaseFeatureTable [featureTable]
     */
    private suspend fun displayTable() {
        // query all the features loaded to the table
        featureTable?.queryFeatures(QueryParameters())?.onSuccess{ queryResults ->
            // inflate the table layout
            val tableLayoutBinding = TableLayoutBinding.inflate(layoutInflater)
            // set up a dialog to be displayed
            Dialog(this).apply {
                setContentView(tableLayoutBinding.root)
                setCancelable(true)
                // grab the instance of the TableLayout
                val table = tableLayoutBinding.tableLayout
                // iterate through each feature to add to the TableLayout
                queryResults.forEach { feature ->
                    // prepare the table row
                    val tableRowBinding = TableRowBinding.inflate(layoutInflater).apply {
                        oid.text = feature.attributes["oid"].toString()
                        collectionTimestamp.text = (feature.attributes["collection_timestamp"] as Instant).toString()
                    }
                    // add the row to the TableLayout
                    table.addView(tableRowBinding.root)
                }
            }.show()
        }?.onFailure {
            showError(it.message.toString())
        }
    }

    /**
     * Called on app launch or when Android share sheet is closed
     */
    private fun resetMap() {
        lifecycleScope.launch{
            mapView.map?.loadStatus?.collect{ loadStatus ->
                if(loadStatus == LoadStatus.Loaded){
                    // clear any feature layers displayed on the map
                    mapView.map?.operationalLayers?.clear()
                    // disable the button since no features are displayed
                    viewTableButton.isEnabled = false
                    // create a new geodatabase file to add features into the feature table
                    createGeodatabase()
                } else if (loadStatus is LoadStatus.FailedToLoad){
                    showError("Error loading MapView: ${loadStatus.error.message}")
                }
            }
        }
    }


    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
