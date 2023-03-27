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

package com.esri.arcgismaps.sample.addwmslayer

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.WmsLayer
import com.esri.arcgismaps.sample.addwmslayer.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create and add a map with a light gray basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISLightGray)

        // apply mapView assignments
        mapView.apply {
            this.map = map
            // set an initial viewpoint to a zoomed out view of North America
            setViewpoint(Viewpoint(39.8, -98.6, 10e7))

        }

        lifecycleScope.launch {
            // if the map load fails, show an error and return
            map.load().onFailure {
                return@launch showError("Error loading map")
            }
            // create a list representing names of layers to load from the WMS service
            val wmsLayerNames = listOf("1")
            // create a new WmsLayer with the WMS service url and the layers name list
            val wmsLayer = WmsLayer(getString(R.string.wms_layer_url), wmsLayerNames)
            // add the wmsLayer to the map as an operational layer
            map.operationalLayers.add(wmsLayer)
            // if loading the layer fails show an error
            wmsLayer.load().onFailure {
                showError("Error loading WmsLayer")
            }
        }
    }

    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
