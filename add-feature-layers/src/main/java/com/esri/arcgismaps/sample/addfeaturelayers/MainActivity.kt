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

package com.esri.arcgismaps.sample.addfeaturelayers

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.GeoPackage
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.data.ShapefileFeatureTable
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.portal.Portal
import com.arcgismaps.portal.PortalItem
import com.esri.arcgismaps.sample.addfeaturelayers.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val provisionPath: String by lazy {
        getExternalFilesDir(null)?.path.toString() + File.separator + getString(R.string.app_name)
    }

    // enum to keep track of the selected source to display the feature layer
    enum class FeatureLayerSource(val menuPosition: Int) {
        SERVICE_FEATURE_TABLE(0),
        PORTAL_ITEM(1),
        GEODATABASE(2),
        GEOPACKAGE(3),
        SHAPEFILE(4)
    }

    // keeps track of the previously selected feature layer source
    private var previousSource: FeatureLayerSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)
        // set the map to be displayed in as the BasemapStyle topographic
        activityMainBinding.mapView.map = ArcGISMap(BasemapStyle.ArcGISTopographic)
        setUpBottomUI()
    }

    /**
     * Sets the map using the [layer] at the given [viewpoint]
     */
    private fun setFeatureLayer(layer: FeatureLayer, viewpoint: Viewpoint) {
        activityMainBinding.mapView.apply {
            // clears the existing layer on the map
            map?.operationalLayers?.clear()
            // adds the new layer to the map
            map?.operationalLayers?.add(layer)
            // updates the viewpoint to the given viewpoint
            setViewpoint(viewpoint)
        }
    }

    /**
     * Load a feature layer with a URL
     */
    private fun loadFeatureServiceURL() {
        // initialize the service feature table using a URL
        val serviceFeatureTable =
            ServiceFeatureTable(resources.getString(R.string.sample_service_url))
        // create a feature layer with the feature table
        val featureLayer = FeatureLayer.createWithFeatureTable(serviceFeatureTable)
        val viewpoint = Viewpoint(41.70, -88.20, 120000.0)
        // set the feature layer on the map
        setFeatureLayer(featureLayer, viewpoint)
    }


    /**
     * Load a feature layer with a portal item
     */
    private suspend fun loadPortalItem() {
        // set the portal
        val portal = Portal("https://www.arcgis.com")
        // create the portal item with the item ID for the Portland tree service data
        val portalItem = PortalItem(portal, "1759fd3e8a324358a0c58d9a687a8578")
        portalItem.load().onSuccess {
            // create the feature layer with the item
            val featureLayer = FeatureLayer.createWithItem(portalItem)
            // set the viewpoint to Portland, Oregon
            val viewpoint = Viewpoint(45.5266, -122.6219, 2500.0)
            // set the feature layer on the map
            setFeatureLayer(featureLayer, viewpoint)
        }.onFailure {
            showError("Error loading portal item: ${it.message}")
        }
    }

    /**
     * Load a feature layer with a local geodatabase file
     */
    private suspend fun loadGeodatabase() {
        // locate the .geodatabase file in the device
        val geodatabaseFile = File(provisionPath, getString(R.string.geodatabase_la_trails))
        // instantiate the geodatabase with the file path
        val geodatabase = Geodatabase(geodatabaseFile.path)
        // load the geodatabase
        geodatabase.load().onSuccess {
            // get the feature table with the name
            val geodatabaseFeatureTable =
                geodatabase.getFeatureTable("Trailheads")
            if (geodatabaseFeatureTable == null) {
                showError("Feature table name not found in geodatabase")
                return
            }
            // create a feature layer with the feature table
            val featureLayer = FeatureLayer.createWithFeatureTable(geodatabaseFeatureTable)
            // set the viewpoint to Malibu, California
            val viewpoint = Viewpoint(34.0772, -118.7989, 600000.0)
            // set the feature layer on the map
            setFeatureLayer(featureLayer, viewpoint)
        }.onFailure {
            showError("Error loading geodatabase: ${it.message}")
        }
    }

    /**
     * Load a feature layer with a local geopackage file
     */
    private suspend fun loadGeopackage() {
        // locate the .gpkg file in the device
        val geopackageFile = File(provisionPath, "/AuroraCO.gpkg")
        // instantiate the geopackage with the file path
        val geoPackage = GeoPackage(geopackageFile.path)
        // load the geopackage
        geoPackage.load().onSuccess {
            // get the first feature table in the geopackage
            val geoPackageFeatureTable = geoPackage.geoPackageFeatureTables.first()
            // create a feature layer with the feature table
            val featureLayer = FeatureLayer.createWithFeatureTable(geoPackageFeatureTable)
            // set the viewpoint to Denver, CO
            val viewpoint = Viewpoint(39.7294, -104.8319, 500000.0)
            // set the feature layer on the map
            setFeatureLayer(featureLayer, viewpoint)
        }.onFailure {
            showError("Error loading geopackage: ${it.message}")
        }
    }

    /**
     * Load a feature layer with a local shapefile file
     */
    private suspend fun loadShapefile() {
        // locate the shape file in device
        val file = File(
            provisionPath,
            "/ScottishWildlifeTrust_ReserveBoundaries_20201102.shp"
        )
        // create a shapefile feature table from a named bundle resource
        val shapeFileTable = ShapefileFeatureTable(file.path)
        shapeFileTable.load().onSuccess {
            // create a feature layer for the shapefile feature table
            val featureLayer = FeatureLayer.createWithFeatureTable(shapeFileTable)
            // set the viewpoint to Scotland
            val viewpoint = Viewpoint(56.641344, -3.889066, 6000000.0)
            // set the feature layer on the map
            setFeatureLayer(featureLayer, viewpoint)
        }.onFailure {
            showError("Error loading shapefile: ${it.message}")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        Log.e(TAG, message)
    }

    /**
     * Sets up the bottom UI selector to switch between
     * different ways to load a feature layers
     */
    private fun setUpBottomUI() {
        // create an adapter with the types of feature layer
        // sources to be displayed in menu
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.feature_layer_sources)
        )
        activityMainBinding.bottomListItems.apply {
            // populate the bottom list with the feature layer sources
            setAdapter(adapter)
            // click listener when feature layer source is selected
            setOnItemClickListener { _, _, i, _ ->
                // get the selected feature layer source
                val selectedSource = FeatureLayerSource.values().find { it.menuPosition == i }
                // check if the same feature is selected
                if (previousSource != null && (previousSource == selectedSource)) {
                    // same feature layer selected, return
                    return@setOnItemClickListener
                }
                lifecycleScope.launch {
                    // set the feature layer source using the selected source
                    when (selectedSource) {
                        FeatureLayerSource.SERVICE_FEATURE_TABLE -> loadFeatureServiceURL()
                        FeatureLayerSource.PORTAL_ITEM -> loadPortalItem()
                        FeatureLayerSource.GEODATABASE -> loadGeodatabase()
                        FeatureLayerSource.GEOPACKAGE -> loadGeopackage()
                        FeatureLayerSource.SHAPEFILE -> loadShapefile()
                        else -> {}
                    }
                }
                // update the previous feature layer source
                previousSource = selectedSource
            }
        }
    }
}
