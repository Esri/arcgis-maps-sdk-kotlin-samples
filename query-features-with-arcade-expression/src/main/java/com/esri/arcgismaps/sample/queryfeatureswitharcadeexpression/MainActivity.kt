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

package com.esri.arcgismaps.sample.queryfeatureswitharcadeexpression

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scaleMatrix
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.arcade.ArcadeEvaluator
import com.arcgismaps.arcade.ArcadeExpression
import com.arcgismaps.arcade.ArcadeProfile
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.portal.Portal
import com.arcgismaps.portal.PortalItem
import com.esri.arcgismaps.sample.queryfeatureswitharcadeexpression.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
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

    private val infoTextView by lazy {
        activityMainBinding.infoTextView
    }

    private val progressBar by lazy {
        activityMainBinding.progressBar
    }

    // setup the red pin marker image as a bitmap drawable
    private val markerDrawable: BitmapDrawable by lazy {
        // load the bitmap from resources and create a drawable
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.map_pin_symbol)
        BitmapDrawable(resources, bitmap)
    }

    // setup the red pin marker as a Graphic
    private val markerGraphic: Graphic by lazy {
        // creates a symbol from the marker drawable
        val markerSymbol = PictureMarkerSymbol(markerDrawable).apply {
            // resize the symbol into a smaller size
            width = 30f
            height = 30f
            // offset in +y axis so the marker spawned is right on the touch point
            offsetY = 25f
        }
        // create the graphic from the symbol
        Graphic(symbol = markerSymbol)
    }

    // creates a graphic overlay
    private val graphicsOverlay = GraphicsOverlay()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create a portal item with the itemId of the web map
        val portal = Portal("https://www.arcgis.com/")
        val portalItem = PortalItem(portal, "539d93de54c7422f88f69bfac2aebf7d")
        // create and add a map with with portal item
        val map = ArcGISMap(portalItem)
        // add our marker overlay to the graphics overlay
        graphicsOverlay.graphics.add(markerGraphic)
        // apply mapview assignments
        mapView.apply {
            this.map = map
            graphicsOverlays.add(graphicsOverlay)
        }

        lifecycleScope.launch {
            // show an error and return if the map load failed
            map.load().onFailure {
                return@launch showError("Error loading map:${it.message}")
            }

            // get the RPD Beats layer from the map's operational layers
            val policeBeatsLayer =
                map.operationalLayers.firstOrNull { layer ->
                    layer.id == "RPD_Reorg_9254"
                } ?: return@launch showError("Error finding RPD Beats layer")

            mapView.onSingleTapConfirmed.collect { event ->
                markerGraphic.geometry = event.mapPoint
                evaluateArcadeExpression(event.screenCoordinate, policeBeatsLayer)
            }
        }
    }

    private suspend fun evaluateArcadeExpression(
        screenCoordinate: ScreenCoordinate,
        policeBeatsLayer: Layer
    ) {
        progressBar.visibility = View.VISIBLE
        infoTextView.text = ""

        val result = mapView.identifyLayer(
            layer = policeBeatsLayer,
            screenCoordinate = screenCoordinate,
            tolerance = 12.0,
            returnPopupsOnly = false
        )

        val identifyLayerResult = result.getOrElse { error ->
            showError("Error identifying layer:${error.message}")
            progressBar.visibility = View.GONE
            return
        }

        if (identifyLayerResult.geoElements.isEmpty()) {
            infoTextView.text =  "No layers found"
            progressBar.visibility = View.GONE
            return
        }

        val feature = identifyLayerResult.geoElements.first() as ArcGISFeature

        val expressionValue =
            "var crimes = FeatureSetByName(\$map, 'Crime in the last 60 days');\n" +
                "return Count(Intersects(\$feature, crimes));"

        val arcadeExpression = ArcadeExpression(expressionValue)
        val arcadeEvaluator = ArcadeEvaluator(arcadeExpression, ArcadeProfile.FormCalculation)
        val profileVariables = mapOf<String, Any>("\$feature" to feature, "\$map" to mapView.map!!)

        val evaluationResult = arcadeEvaluator.evaluate(profileVariables)

        val arcadeEvaluationResult = evaluationResult.getOrElse {
            progressBar.visibility = View.GONE
            return
        }

        val crimesCount = (arcadeEvaluationResult.result as Double).toInt()

        infoTextView.text = "Crimes in the last 60 days: $crimesCount"
        progressBar.visibility = View.GONE
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
