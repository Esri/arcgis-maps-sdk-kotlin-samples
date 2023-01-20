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

package com.esri.arcgismaps.sample.findrouteintransportnetwork

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.layers.ArcGISTiledLayer
import com.arcgismaps.mapping.layers.TileCache
import com.arcgismaps.mapping.symbology.CompositeSymbol
import com.arcgismaps.mapping.symbology.HorizontalAlignment
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.symbology.VerticalAlignment
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.tasks.networkanalysis.RouteParameters
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import com.arcgismaps.tasks.networkanalysis.TravelMode
import com.esri.arcgismaps.sample.findrouteintransportnetwork.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val provisionPath: String by lazy {
        getExternalFilesDir(null)?.path.toString() + File.separator + getString(R.string.app_name)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val toggleButtons by lazy {
        activityMainBinding.toggleButtons
    }

    private val clearButton by lazy {
        activityMainBinding.clearButton
    }

    private val distanceTimeTextView by lazy {
        activityMainBinding.distanceTimeTextView
    }

    private val stopsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }
    private val routeOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }

    // create a route task to calculate routes
    private var routeTask: RouteTask? = null

    private var routeParameters: RouteParameters? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        // some parts of the API require an Android Context to properly interact with Android system
        // features, such as LocationProvider and application resources
        ArcGISEnvironment.applicationContext = applicationContext
        lifecycle.addObserver(mapView)


        // create a tile cache from the .tpkx file
        val tileCache = TileCache(provisionPath + getString(R.string.tpkx_path))
        val tiledLayer = ArcGISTiledLayer(tileCache)
        // make a basemap with the tiled layer and add it to the mapview as an ArcGISMap
        mapView.map = ArcGISMap(Basemap(tiledLayer))

        // add the graphics overlays to the map view
        mapView.graphicsOverlays.addAll(listOf(routeOverlay, stopsOverlay))

        val temp = File(provisionPath + getString(R.string.geodatabase_path))
        routeTask = RouteTask(temp.path, "Streets_ND")

        // load the route task
        lifecycleScope.launch {
            routeTask?.load()?.onFailure {
                showError(it.message.toString())
            }?.onSuccess {
                // use the default parameters for the route calculation
                routeParameters = routeTask?.createDefaultParameters()?.getOrThrow()
            }
        }

        toggleButtons.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.fastestButton -> {
                        // calculate fastest route
                        routeParameters?.travelMode =
                            routeTask?.getRouteTaskInfo()?.travelModes?.get(0)

                        // update route based on selection
                        updateRoute()
                    }
                    R.id.shortestButton -> {
                        // calculate shortest route
                        routeParameters?.travelMode =
                            routeTask?.getRouteTaskInfo()?.travelModes?.get(1)

                        // update route based on selection
                        updateRoute()
                    }
                }
            }
        }

        // make a clear button to reset the stops and routes
        clearButton.setOnClickListener {
            stopsOverlay.graphics.clear()
            routeOverlay.graphics.clear()
            clearButton.isEnabled = false
        }

        // set up the touch listeners on the map view
        createMapGestures()
    }

    /**
     * Sets up the onTouchListener for the mapView.
     * For single taps, graphics will be selected.
     * For double touch drags, graphics will be moved.
     * */
    private fun createMapGestures() {
        with(lifecycleScope) {
            launch {
                val envelope = Envelope(
                    Point(-13046352.223196, 3864910.900750, 0.0, SpatialReference.webMercator()),
                    Point(-13024588.857198, 3838880.505604, 0.0, SpatialReference.webMercator())
                )
                mapView.setViewpointGeometry(envelope)
            }

            // add graphic at the tapped coordinate
            launch {
                mapView.onSingleTapConfirmed.collect { tapEvent ->
                    val screenCoordinate = tapEvent.screenCoordinate
                    addOrSelectGraphic(screenCoordinate)
                    clearButton.isEnabled = true
                }
            }
            // move the graphic to a different coordinate
            launch {
                //TODO
            }
        }
    }

    /**
     * Updates the calculated route by calling routeTask.solveRoute().
     * Creates a graphic to display the route.
     * */
    private fun updateRoute() = lifecycleScope.launch {
        // get a list of stops from the graphics currently on the graphics overlay.
        val stops = stopsOverlay.graphics.map {
            Stop(it.geometry as Point)
        }

        // do not calculate a route if there is only one stop
        if (stops.size <= 1) return@launch

        routeParameters?.setStops(stops)

        // solve the route
        val results = routeParameters?.let { routeTask?.solveRoute(it) }
        if (results != null) {
            results.onFailure {
                showError("No route solution. ${it.message}")
                routeOverlay.graphics.clear()
            }.onSuccess { routeResult ->
                // get the first solved route result
                val route = routeResult.routes[0]

                // create graphic for route
                val graphic = Graphic(
                    route.routeGeometry, SimpleLineSymbol(
                        SimpleLineSymbolStyle.Solid,
                        Color.black, 3F
                    )
                )
                routeOverlay.graphics.clear()
                routeOverlay.graphics.add(graphic)
            }
        }
    }

    /**
     * Selects a graphic if there is one at the
     * provided [screenCoordinate] or, if there is
     * none, creates a new graphic.
     * */
    private suspend fun addOrSelectGraphic(screenCoordinate: ScreenCoordinate) {
        // identify the selected graphic
        val result =
            mapView.identifyGraphicsOverlay(stopsOverlay, screenCoordinate, 10.0, false)

        result.onFailure {
            showError(it.message.toString())
        }.onSuccess { identifyGraphicsOverlayResult ->
            val graphics = identifyGraphicsOverlayResult.graphics

            // unselect everything
            if (stopsOverlay.selectedGraphics.isNotEmpty()) {
                stopsOverlay.unselectGraphics(stopsOverlay.selectedGraphics)
            }

            // if the user tapped on something, select it
            if (graphics.isNotEmpty()) {
                val firstGraphic = graphics[0]
                firstGraphic.isSelected = true
            } else {
                // there is no graphic at this location
                // make a new graphic at the tapped location
                val locationPoint = mapView.screenToLocation(screenCoordinate)
                createStopSymbol(stopsOverlay.graphics.size + 1, locationPoint)
            }
        }
    }

    /**
     * Creates a composite symbol to represent a numbered stop.
     * The [stopNumber] is the ordinal number of this stop and the
     * symbol will be placed at the [locationPoint].
     */
    private fun createStopSymbol(stopNumber: Int, locationPoint: Point?) {
        // create a orange pin PictureMarkerSymbol
        val pinSymbol = PictureMarkerSymbol(
            ContextCompat.getDrawable(
                this,
                R.drawable.pin_symbol
            ) as BitmapDrawable
        ).apply {
            // set the scale of the symbol
            width = 24f
            height = 24f
            // set in pin "drop" to be offset to the point on map
            offsetY = 10f
        }

        // create black stop number TextSymbol
        val stopNumberSymbol = TextSymbol(
            stopNumber.toString(),
            Color.black,
            12f,
            HorizontalAlignment.Center,
            VerticalAlignment.Bottom
        ).apply {
            offsetY = 4f
        }

        // create a composite symbol and add the picture marker symbol and text symbol
        val compositeSymbol = CompositeSymbol()
        compositeSymbol.symbols.addAll(listOf(pinSymbol, stopNumberSymbol))

        // create a graphic to add to the overlay and update the route
        val graphic = Graphic(locationPoint, compositeSymbol)
        stopsOverlay.graphics.add(graphic)

        updateRoute()
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
