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

package com.esri.arcgisruntime.sample.showdevicelocation

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import arcgisruntime.ApiKey
import arcgisruntime.ArcGISRuntimeEnvironment
import arcgisruntime.location.LocationDisplayAutoPanMode
import arcgisruntime.mapping.ArcGISMap
import arcgisruntime.mapping.BasemapStyle
import arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.sample.showdevicelocation.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISRuntimeEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        // some parts of the API require an Android Context to properly interact with Android system
        // features, such as LocationProvider and application resources
        ArcGISRuntimeEnvironment.applicationContext = applicationContext

        // set up data binding for the activity
        val activityMainBinding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)
        val mapView = activityMainBinding.mapView
        lifecycle.addObserver(mapView)

        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISNavigationNight)
        mapView.map = map

        val locationDisplay = mapView.locationDisplay

        lifecycleScope.launch {
            // listen to changes in the status of the location data source
            locationDisplay.dataSource.start().also {
                // check permissions to see if failure may be due to lack of permissions
                runOnUiThread {
                    requestPermissions(mapView)
                    locationDisplay.dataSource.status
                }
            }
        }
        // populate the list for the location display options for the spinner's adapter
        val list = arrayListOf(
            ItemData("Stop", R.drawable.locationdisplaydisabled),
            ItemData("On", R.drawable.locationdisplayon),
            ItemData("Re-Center", R.drawable.locationdisplayrecenter),
            ItemData("Navigation", R.drawable.locationdisplaynavigation),
            ItemData("Compass", R.drawable.locationdisplayheading)
        )

        activityMainBinding.spinner.apply {
            adapter = SpinnerAdapter(this@MainActivity, R.id.locationTextView, list)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    when (position) {
                        0 ->  // stop location display
                            lifecycleScope.launch {
                                locationDisplay.dataSource.stop()
                            }
                        1 ->  // start location display
                            lifecycleScope.launch {
                                locationDisplay.dataSource.start()
                            }
                        2 -> {
                            // re-center MapView on location
                            locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
                        }
                        3 -> {
                            // start navigation mode
                            locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
                        }
                        4 -> {
                            // start compass navigation mode
                            locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.CompassNavigation)
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    /**
     * Request fine and coarse location permissions for API level 23+.
     */
    private fun requestPermissions(mapView: MapView) {
        // coarse location permission
        val permissionCheckCoarseLocation =
            ContextCompat.checkSelfPermission(this@MainActivity, ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        // fine location permission
        val permissionCheckFineLocation =
            ContextCompat.checkSelfPermission(this@MainActivity, ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        if (!(permissionCheckCoarseLocation && permissionCheckFineLocation)) { // if permissions are not already granted, request permission from the user
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION), 2)
        } else {
            Snackbar.make(mapView, "Location permissions required to run this sample!", Snackbar.LENGTH_LONG).show()
            // update UI to reflect that the location display did not actually start
            spinner.setSelection(0, true)
        }
    }
}
