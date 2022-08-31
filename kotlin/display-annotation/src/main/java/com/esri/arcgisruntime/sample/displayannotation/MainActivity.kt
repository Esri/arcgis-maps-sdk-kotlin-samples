/*
 *  Copyright 2019 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.esri.arcgisruntime.sample.displayannotation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import arcgisruntime.ApiKey
import arcgisruntime.ArcGISRuntimeEnvironment
import arcgisruntime.data.ServiceFeatureTable
import arcgisruntime.mapping.ArcGISMap
import arcgisruntime.mapping.BasemapStyle
import arcgisruntime.mapping.Viewpoint
import arcgisruntime.mapping.layers.AnnotationLayer
import arcgisruntime.mapping.layers.FeatureLayer
import com.esri.arcgisruntime.sample.displayannotation.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISRuntimeEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        lifecycle.addObserver(activityMainBinding.mapView)

        val operationalLayerList = mutableListOf(
            // add a feature layer from a feature service
            FeatureLayer(ServiceFeatureTable(getString(R.string.river_feature_service_url))),
            // add an annotation layer from a feature service
            AnnotationLayer(getString(R.string.river_annotation_feature_service_url))
        )

        // create a map with a light gray basemap
        val lightGrayMap = ArcGISMap(BasemapStyle.ArcGISLightGray)
        // add the list of operational layers to the map
        lightGrayMap.operationalLayers.addAll(operationalLayerList)

        activityMainBinding.mapView.apply {
            // set the map to the mapView
            map = lightGrayMap
            // set the map view's initial view point
            setViewpoint(Viewpoint(55.882436, -2.725610, 75000.0))
        }
    }
}
