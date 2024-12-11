/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.queryfeatureswitharcadeexpression.components

import android.app.Application
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.arcade.ArcadeEvaluator
import com.arcgismaps.arcade.ArcadeExpression
import com.arcgismaps.arcade.ArcadeProfile
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.queryfeatureswitharcadeexpression.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class QueryFeaturesWithArcadeExpressionViewModel(application: Application) :
    AndroidViewModel(application) {

    // setup the red pin marker image as a bitmap drawable
    private val markerDrawable: BitmapDrawable by lazy {
        val bitmap = BitmapFactory.decodeResource(application.resources, R.drawable.map_pin_symbol)
        BitmapDrawable(application.resources, bitmap)
    }

    // setup the red pin marker as a Graphic
    private val markerGraphic: Graphic by lazy {
        val markerSymbol = PictureMarkerSymbol.createWithImage(markerDrawable).apply {
            width = 30f
            height = 30f
            offsetY = 25f
        }

        Graphic(symbol = markerSymbol)
    }

    // data layer to be loaded from portal item
    private var policeBeatsLayer: Layer? = null

    // state flow to expose query results and status to UI
    private val _queryStateFlow = MutableStateFlow(QueryState())
    val queryStateFlow = _queryStateFlow.asStateFlow()

    val graphicsOverlay = GraphicsOverlay()

    // create a portal item with the itemId of the web map
    val portal = Portal("https://www.arcgis.com/")
    val portalItem = PortalItem(portal, "539d93de54c7422f88f69bfac2aebf7d")

    // create and add a map with with portal item
    val arcGISMap = ArcGISMap(portalItem)

    // create a map view proxy for handling interactions with the map view
    val mapViewProxy = MapViewProxy()

    // create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map",
                    error.message.toString()
                )
            }
        }

        // add the marker graphic to the graphics overlay
        graphicsOverlay.graphics.add(markerGraphic)

        // get the RPD Beats layer from the map's operational layers
        policeBeatsLayer = arcGISMap.operationalLayers.firstOrNull { layer ->
            layer.id == "RPD_Reorg_9254"
        }
    }

    /**
     * Handle a tap on the map view from the user
     */
    fun handleTap(point: Point, screenCoordinate: ScreenCoordinate) {
        // update the marker location to where the user tapped on the map
        markerGraphic.geometry = point
        viewModelScope.launch {
            // centre the viewpoint on where the user tapped on the map
            mapViewProxy.setViewpointCenter(point)

            // evaluate an Arcade expression on the tapped screen coordinate
            evaluateArcadeExpression(screenCoordinate)
        }
    }

    /**
     * Evaluates an Arcade expression that returns crime in the last 60 days at the tapped
     * [screenCoordinate] on the [arcGISMap] with the [policeBeatsLayer] and displays the result
     * in a textview
     */
    private suspend fun evaluateArcadeExpression(screenCoordinate: ScreenCoordinate) {
        // show the loading spinner as the Arcade evaluation can take time to complete
        _queryStateFlow.value = QueryState(loadState = LoadState.LOADING)

        policeBeatsLayer?.let { layer ->
            // identify the layer and its elements based on the position tapped on the mapView and
            // get the result
            val result = mapViewProxy.identify(
                layer = layer,
                screenCoordinate = screenCoordinate,
                tolerance = 12.dp,
                returnPopupsOnly = false
            )

            // get the result as an IdentifyLayerResult
            val identifyLayerResult = result.getOrElse { error ->
                // if the identifyLayer operation failed show an error and return
                messageDialogVM.showMessageDialog(
                    "Error identifying layer:",
                    error.message.toString()
                )
                // reset the query results and loading indicator
                _queryStateFlow.value = QueryState()
                return
            }

            if (identifyLayerResult.geoElements.isEmpty()) {
                _queryStateFlow.value = QueryState(loadState = LoadState.LOADED)
                return
            }

            // get the first identified GeoElement as an ArcGISFeature
            val identifiedFeature = identifyLayerResult.geoElements.first() as ArcGISFeature
            // create a string containing the Arcade expression
            val expressionValue = "var crimes = FeatureSetByName(\$map, 'Crime in the last 60 days');\n" +
                    "return Count(Intersects(\$feature, crimes));"

            // create an arcade expression from the string and configure an arcade evaluator
            val arcadeExpression = ArcadeExpression(expressionValue)
            val arcadeEvaluator = ArcadeEvaluator(arcadeExpression, ArcadeProfile.FormCalculation)

            // map profile variables with the feature
            val profileVariables = mapOf<String, Any>("\$feature" to identifiedFeature, "\$map" to arcGISMap)
            val evaluationResult = arcadeEvaluator.evaluate(profileVariables)
            val arcadeEvaluationResult = evaluationResult.getOrElse { error ->
                messageDialogVM.showMessageDialog("Error", error.message.toString())
                _queryStateFlow.value = QueryState()
                return
            }

            _queryStateFlow.value = QueryState(arcadeEvaluationResult.result as Double, LoadState.LOADED)
        }
    }
}

data class QueryState(val crimes: Double? = null, val loadState: LoadState = LoadState.READY_TO_START)

enum class LoadState {
    READY_TO_START,
    LOADING,
    LOADED
}
