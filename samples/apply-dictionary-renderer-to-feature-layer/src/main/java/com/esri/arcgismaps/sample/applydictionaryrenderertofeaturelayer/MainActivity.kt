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

package com.esri.arcgismaps.sample.applydictionaryrenderertofeaturelayer

import android.os.Bundle
import android.util.Log
import com.esri.arcgismaps.sample.sampleslib.BaseEdgeToEdgeActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.DictionaryRenderer
import com.arcgismaps.mapping.symbology.DictionarySymbolStyle
import com.esri.arcgismaps.sample.applydictionaryrenderertofeaturelayer.databinding.ApplyDictionaryRendererToFeatureLayerActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : BaseEdgeToEdgeActivity() {

    // set up data binding for the activity
    private val activityMainBinding: ApplyDictionaryRendererToFeatureLayerActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.apply_dictionary_renderer_to_feature_layer_activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val provisionPath: String by lazy {
        getExternalFilesDir(null)?.path.toString() + File.separator + getString(R.string.apply_dictionary_renderer_to_feature_layer_app_name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)
        lifecycle.addObserver(mapView)

        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
        mapView.map = map

        // locate the .stylx file in the device
        val styleFile = File(provisionPath, getString(R.string.mil2525d_stylx))
        // instantiate the dictionarySymbolStyle using the file path
        val dictionarySymbolStyle = DictionarySymbolStyle.createFromFile(styleFile.absolutePath)

        // locate the .geodatabase file in the device
        val geodatabaseFile = File(provisionPath, getString(R.string.militaryoverlay_geodatabase))
        // instantiate the geodatabase with the file path
        val geodatabase = Geodatabase(geodatabaseFile.path)

        lifecycleScope.launch {
            // load the dictionary symbol style
            dictionarySymbolStyle.load().getOrElse {
                return@launch showError("Error loading DictionarySymbolStyle: ${it.message}")
            }

            // load the geodatabase
            geodatabase.load().getOrElse {
                showError("Error loading Geodatabase: ${it.message}")
            }

            geodatabase.featureTables.forEach { geodatabaseFeatureTable ->
                // load each geodatabaseFeatureTable and create featureLayer from it
                geodatabaseFeatureTable.load().getOrElse {
                    return@launch showError("Error loading GeodatabaseFeatureTable: ${it.message}")
                }
                val featureLayer = FeatureLayer.createWithFeatureTable(geodatabaseFeatureTable)
                featureLayer.load().getOrElse {
                    return@launch showError("Error loading FeatureLayer: ${it.message}")
                }
                // add featureLayer to the map's operational layer
                mapView.map?.operationalLayers?.add(featureLayer)

                // create dictionaryRenderer using the dictionarySymbolStyle and apply it to the featureLayer's renderer
                val dictionaryRenderer = DictionaryRenderer(dictionarySymbolStyle)
                featureLayer.renderer = dictionaryRenderer
                // get the featureLayer's envelope to set the map viewpoint
                val extent = featureLayer.fullExtent
                    ?: return@launch showError("Error retrieving extent of the feature layer")
                mapView.setViewpoint(Viewpoint(extent))
            }
        }
    }

    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}


