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

package com.esri.arcgismaps.sample.addfeatureswithcontingentvalues

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.esri.arcgismaps.sample.addfeatureswithcontingentvalues.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File

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

    // graphic overlay instance to add the feature graphic to the map
    private val graphicsOverlay = GraphicsOverlay()

    // mobile database containing offline feature data. GeoDatabase is closed on app exit
    private var geoDatabase: Geodatabase? = null

    // instance of the contingent feature to be added to the map
    private var feature: ArcGISFeature? = null

    // instance of the feature table retrieved from the GeoDatabase, updates when new feature is added
    private var featureTable: ArcGISFeatureTable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISNavigationNight)
        mapView.map = map
    }

    private fun showError(message: String, view: View) {
        Log.e(TAG, message)
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }
}
