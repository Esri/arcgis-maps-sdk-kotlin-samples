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

package com.esri.arcgismaps.sample.queryfeaturetable.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.Color
import com.arcgismaps.data.FeatureQueryResult
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.toolkit.geocompose.MapViewpointOperation
import com.esri.arcgismaps.sample.queryfeaturetable.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

class MapViewModel(
    private val application: Application,
    private val sampleCoroutineScope: CoroutineScope
) : AndroidViewModel(application) {

    // create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    // create a service feature table and a feature layer from it
    private val serviceFeatureTable: ServiceFeatureTable = ServiceFeatureTable(
        application.getString(R.string.us_daytime_population_url)
    )

    // create the feature layer using the service feature table
    private val featureLayer: FeatureLayer by lazy {
        FeatureLayer.createWithFeatureTable(serviceFeatureTable)
    }

    // map used to display the feature layer
    val map = ArcGISMap(BasemapStyle.ArcGISTopographic)

    private var usaViewpoint = Viewpoint(
        center = Point(-11e6, 5e6, SpatialReference.webMercator()),
        scale = 1e8
    )

    // define a MapViewpointOperation to set the initial viewpoint and update it when the viewpoint changes
    var mapViewpointOperation: MapViewpointOperation? by mutableStateOf(null)

    init {
        // use symbols to show U.S. states with a black outline and yellow fill
        val lineSymbol = SimpleLineSymbol(
            style = SimpleLineSymbolStyle.Solid,
            color = Color.black,
            width = 1.0f
        )
        val fillSymbol = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = Color.fromRgba(255, 255, 0, 255),
            outline = lineSymbol
        )

        // set featurelayer properties
        featureLayer.apply {
            // set renderer for the feature layer
            renderer = SimpleRenderer(fillSymbol)
            opacity = 0.8f
            maxScale = 10000.0
        }
        // add the feature layer to the map's operational layers
        map.operationalLayers.add(featureLayer)
        // set initial mapView viewpoint using MapViewpointOperation class
        mapViewpointOperation = MapViewpointOperation.Set(usaViewpoint)
    }

    /**
     * Search for a U.S. state using [searchQuery] in the feature table, and if found add it
     * to the featureLayer, zoom to it, and select it.
     */
    fun searchForState(searchQuery: String) {
        // clear any previous selections
        featureLayer.clearSelection()
        // create a query for the state that was entered
        val queryParameters = QueryParameters().apply {
            // make search case insensitive
            whereClause = ("upper(STATE_NAME) LIKE '%" + searchQuery.uppercase(Locale.US) + "%'")
        }

        sampleCoroutineScope.launch {
            // call select features
            val featureQueryResult = serviceFeatureTable.queryFeatures(queryParameters).getOrElse {
                messageDialogVM.showMessageDialog(it.message.toString(), it.cause.toString())
            } as FeatureQueryResult

            val feature = featureQueryResult.firstOrNull()
            if (feature != null) {
                // select the feature
                featureLayer.selectFeature(feature)
                // get the extent of the first feature in the result to zoom to
                val envelope = feature.geometry?.extent
                    ?: return@launch messageDialogVM.showMessageDialog("Error retrieving geometry extent")
                // update the MapViewpointOperation to set the geometry of the returned feature
                mapViewpointOperation = MapViewpointOperation.SetBoundingGeometry(envelope)
            } else {
                messageDialogVM.showMessageDialog("No states found with name: $searchQuery")
            }
        }
    }
}


