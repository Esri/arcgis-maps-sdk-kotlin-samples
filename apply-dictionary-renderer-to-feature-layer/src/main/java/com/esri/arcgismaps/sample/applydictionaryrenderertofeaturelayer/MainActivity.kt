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

package com.esri.arcgismaps.sample.applydictionaryrenderertofeaturelayer

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISVectorTiledLayer
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.DictionaryRenderer
import com.arcgismaps.mapping.symbology.DictionarySymbolStyle
import com.esri.arcgismaps.sample.applydictionaryrenderertofeaturelayer.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
        mapView.map = map

        // locate the .geodatabase file in the device
        val geoDatabaseFile = File(provisionPath, getString(R.string.militaryoverlay_geodatabase))
        // instantiate the geodatabase with the file path
        val geoDatabase = Geodatabase(geoDatabaseFile.path)

        val styleFile = File(provisionPath, getString(R.string.mil2525d_stylx))
        val dictionarySymbolStyle = DictionarySymbolStyle.createFromFile(styleFile.path)

        lifecycleScope.launch {
            // load the dictionary symbol style
            dictionarySymbolStyle.load().getOrElse {
                return@launch showError("Error loading DictionarySymbolStyle: ${it.message}")
            }
        }

        lifecycleScope.launch {
            geoDatabase.load().getOrElse {
                showError("Error loading GeoDatabase: ${it.message}")
            }

            geoDatabase.featureTables.forEach { geoDatabaseFeatureTable ->
                geoDatabaseFeatureTable.load().getOrElse {
                    return@launch showError("Error loading GeoDatabaseFeatureTable: ${it.message}")
                }
                val featureLayer = FeatureLayer(geoDatabaseFeatureTable)
                featureLayer.load().getOrElse {
                    return@launch showError("Error loading FeatureLayer: ${it.message}")
                }
                mapView.map?.operationalLayers?.add(featureLayer)

                val dictionaryRenderer = DictionaryRenderer(dictionarySymbolStyle)
                featureLayer.renderer = dictionaryRenderer
                // get the envelop to set the viewpoint
                val extent = featureLayer.fullExtent
                    ?: return@launch showError("Error retrieving extent of the feature layer")
                mapView.setViewpoint(Viewpoint(extent))
            }
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }

}

