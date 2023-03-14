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

package com.esri.arcgismaps.sample.showlabelsonlayer

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.Color
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.arcgisservices.LabelingPlacement
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.labeling.ArcadeLabelExpression
import com.arcgismaps.mapping.labeling.LabelDefinition
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.TextSymbol
import com.esri.arcgismaps.sample.showlabelsonlayer.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

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

        // create a map with a light gray map style
        val map = ArcGISMap(BasemapStyle.ArcGISLightGray)
        // create a service feature table from an online feature service of
        // US Congressional Districts
        val serviceFeatureTable =
            ServiceFeatureTable(getString(R.string.congressional_districts_url))
        // create the feature layer from the service feature table
        val featureLayer = FeatureLayer(serviceFeatureTable)
        // add this feature layer to the map
        map.operationalLayers.add(featureLayer)
        // add the map to the mapview
        mapView.map = map

        lifecycleScope.launch {
            // if the map load failed show an error and return
            map.load().onFailure {
                return@onFailure showError("Error loading map:${it.message}")
            }
            // if the feature layer load failed show an error and return
            featureLayer.load().onFailure {
                return@onFailure showError("Error loading feature layer:${it.message}")
            }
            // zoom to the layer when it's done loading
            featureLayer.fullExtent?.let { extent ->
                mapView.setViewpoint(Viewpoint(extent))
            }
        }

        // create label definitions for the different party type attribute in the feature service
        val republicanLabelDefinition = createLabelDefinition("Republican", Color.red)
        val democraticLabelDefinition = createLabelDefinition("Democrat", Color.blue)

        featureLayer.apply {
            // add the label definitions to the feature layer
            labelDefinitions.addAll(
                listOf(
                    republicanLabelDefinition,
                    democraticLabelDefinition
                )
            )
            // enable the labels
            labelsEnabled = true
        }
    }

    /**
     * Creates and returns a [LabelDefinition] for the given party [name] attribute
     * with the [labelColor]
     */
    private fun createLabelDefinition(
        name: String,
        labelColor: Color
    ): LabelDefinition {
        // create a text symbol for styling the label
        val textSymbol = TextSymbol().apply {
            color = labelColor
            size = 12f
            haloColor = Color.white
            haloWidth = 2f
        }
        // create a arcade label expression for the label text
        val arcadeLabelExpression =
            ArcadeLabelExpression(
                "\$feature.NAME + \" (\" + left(\$feature.PARTY,1) " +
                    "+ \")\\nDistrict \" + \$feature.CDFIPS"
            )
        // create and return a new label definition with the arcadeLabelExpression and textSymbol
        return LabelDefinition(arcadeLabelExpression, textSymbol).apply {
            // set the label placement
            placement = LabelingPlacement.PolygonAlwaysHorizontal
            // set the attribute name for which this label will be generated
            whereClause = String.format("PARTY = '%s'", name)
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}

/**
 * Simple extension property that represents a blue color
 */
private val Color.Companion.blue get() = fromRgba(0, 0, 255)
