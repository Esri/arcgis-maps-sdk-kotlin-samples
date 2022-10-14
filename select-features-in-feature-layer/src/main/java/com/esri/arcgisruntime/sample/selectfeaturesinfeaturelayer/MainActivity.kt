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

package com.esri.arcgisruntime.sample.selectfeaturesinfeaturelayer

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import arcgisruntime.ApiKey
import arcgisruntime.ArcGISRuntimeEnvironment
import arcgisruntime.data.Feature
import arcgisruntime.data.ServiceFeatureTable
import arcgisruntime.geometry.Envelope
import arcgisruntime.geometry.SpatialReference
import arcgisruntime.mapping.ArcGISMap
import arcgisruntime.mapping.BasemapStyle
import arcgisruntime.mapping.Viewpoint
import arcgisruntime.mapping.layers.FeatureLayer
import arcgisruntime.mapping.view.MapView
import arcgisruntime.mapping.view.ScreenCoordinate
import com.esri.arcgisruntime.sample.selectfeaturesinfeaturelayer.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private lateinit var activityMainBinding: ActivityMainBinding

    private val gdbPerCapitalURL =
        "https://services1.arcgis.com/4yjifSiIG17X0gW4/arcgis/rest/services/GDP_per_capita_1960_2016/FeatureServer/0"

    // create service feature table and a feature layer from it
    private val serviceFeatureTable = ServiceFeatureTable(gdbPerCapitalURL)
    private val featureLayer = FeatureLayer(serviceFeatureTable)

    private val mapView: MapView by lazy {
        activityMainBinding.mapView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISRuntimeEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        // set up data binding for the activity
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        lifecycle.addObserver(mapView)

        // create a map with the streets base map type
        val streetsMap = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
            // add the feature layer to the map's operational layers
            operationalLayers.add(featureLayer)
        }

        mapView.apply {
            // set the map to be displayed in the layout's map view
            map = streetsMap
            // set an initial view point
            setViewpoint(
                Viewpoint(
                    Envelope(
                        -1131596.019761,
                        3893114.069099,
                        3926705.982140,
                        7977912.461790
                    )
                )
            )
            // give any item selected on the map view a red selection halo
            selectionProperties.color = Color.RED
            // set an on touch listener on the map view
            lifecycleScope.launch {
                onSingleTapConfirmed.collect { tapEvent ->
                    // get the tapped coordinate
                    val screenCoordinate = tapEvent.screenCoordinate
                    getSelectedFeatureLayer(screenCoordinate)
                }
            }
        }
    }

    /**
     * Displays the number of features selected on the given [screenCoordinate]
     */
    private suspend fun getSelectedFeatureLayer(screenCoordinate: ScreenCoordinate) {
        // clear the previous selection
        featureLayer.clearSelection()
        // set a tolerance for accuracy of returned selections from point tapped
        val tolerance = 25.0
        // create a IdentifyLayerResult using the screen coordinate
        val identifyLayerResult =
            mapView.identifyLayer(featureLayer, screenCoordinate, tolerance, false, -1)
        // handle the result's onSuccess and onFailure
        identifyLayerResult.apply {
            onSuccess { identifyLayerResult ->
                // get the elements in the selection that are features
                val features = identifyLayerResult.geoElements.filterIsInstance<Feature>()
                // add the features to the current feature layer selection
                featureLayer.selectFeatures(features)
                Snackbar.make(mapView, "${features.size} features selected", Snackbar.LENGTH_SHORT).show()
            }
            onFailure {
                val errorMessage = "Select feature failed: " + it.message
                Log.e(TAG, errorMessage)
                Snackbar.make(mapView, errorMessage, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
