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
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.Color
import com.arcgismaps.data.FeatureQueryResult
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Geometry
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
import com.esri.arcgismaps.sample.queryfeaturetable.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class MapViewModel(
    private val application: Application,
    private val sampleCoroutineScope: CoroutineScope
) : AndroidViewModel(application) {

    // set the MapView mutable stateflow
    val mapViewState = MutableStateFlow(MapViewState())

    // create a service feature table and a feature layer from it
    private val serviceFeatureTable: ServiceFeatureTable by lazy {
        ServiceFeatureTable(application.getString(R.string.us_daytime_population_url))
    }

    // create the feature layer using the service feature table
    private val featureLayer: FeatureLayer by lazy {
        FeatureLayer.createWithFeatureTable(serviceFeatureTable)
    }

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

        featureLayer.apply {
            // set renderer for the feature layer
            renderer = SimpleRenderer(fillSymbol)
            opacity = 0.8f
            maxScale = 10000.0
        }
        // add the feature layer to the map's operational layers
        mapViewState.value.arcGISMap.operationalLayers.add(featureLayer)
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
                showErrorDialog(it.message.toString(), it.cause.toString())
            } as FeatureQueryResult

            val resultIterator = featureQueryResult.iterator()
            if (resultIterator.hasNext()) {
                resultIterator.next().run {
                    // select the feature
                    featureLayer.selectFeature(this)
                    // get the extent of the first feature in the result to zoom to
                    val envelope = geometry?.extent
                        ?: return@launch showErrorDialog("Error retrieving geometry extent")
                    // update the map view to set the viewpoint to the state geometry
                    mapViewState.update { it.copy(stateGeometry = envelope) }
                }
            } else {
                showErrorDialog("No states found with name: $searchQuery")
            }
        }
    }

    // error dialog status
    val errorDialogStatus = mutableStateOf(false)
    var errorTitle = ""
    var errorDescription = ""

    /**
     * Displays an error dialog with [title] and optional [description]
     */
    private fun showErrorDialog(title: String, description: String = "") {
        errorTitle = title
        errorDescription = description
        errorDialogStatus.value = true
    }
}


/**
 * Data class that represents the MapView state
 */
data class MapViewState(
    var arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic),
    var stateGeometry: Geometry? = null,
    // set an initial viewpoint over the USA
    var viewpoint: Viewpoint = Viewpoint(
        center = Point(-11e6, 5e6, SpatialReference.webMercator()),
        scale = 1e8
    ),
)
