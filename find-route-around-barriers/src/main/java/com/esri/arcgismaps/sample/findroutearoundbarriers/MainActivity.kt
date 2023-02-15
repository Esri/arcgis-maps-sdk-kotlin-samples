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

package com.esri.arcgismaps.sample.findroutearoundbarriers

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.CompositeSymbol
import com.arcgismaps.mapping.symbology.HorizontalAlignment
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.symbology.VerticalAlignment
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.tasks.networkanalysis.DirectionManeuver
import com.arcgismaps.tasks.networkanalysis.PolygonBarrier
import com.arcgismaps.tasks.networkanalysis.RouteParameters
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import com.esri.arcgismaps.sample.findroutearoundbarriers.databinding.ActivityMainBinding
import com.esri.arcgismaps.sample.findroutearoundbarriers.databinding.OptionsDialogBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    // show the options dialog
    private val optionsDialogBinding by lazy {
        OptionsDialogBinding.inflate(layoutInflater)
    }

    // set up the dialog UI views
    private val findBestSequenceSwitch by lazy {
        optionsDialogBinding.findBestSequenceSwitch
    }
    private val firstStopSwitch by lazy {
        optionsDialogBinding.firstStopSwitch
    }
    private val lastStopSwitch by lazy {
        optionsDialogBinding.lastStopSwitch
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val mainContainer: ConstraintLayout by lazy {
        activityMainBinding.mainContainer
    }

    private val addStopsButton: MaterialButton by lazy {
        activityMainBinding.addStopsButton
    }

    private val addBarriersButton: MaterialButton by lazy {
        activityMainBinding.addBarriersButton
    }

    private val resetButton by lazy {
        activityMainBinding.resetButton
    }

    private val optionsButton by lazy {
        activityMainBinding.optionsButton
    }

    private val directionsButton by lazy {
        activityMainBinding.directionsButton
    }

    private val bottomSheet by lazy {
        activityMainBinding.directionSheet.directionSheetLayout
    }

    private val header: ConstraintLayout by lazy {
        activityMainBinding.directionSheet.header
    }

    private val imageView: ImageView by lazy {
        activityMainBinding.directionSheet.imageView
    }

    private val cancelTV: TextView by lazy {
        activityMainBinding.directionSheet.cancelTv
    }

    private val directionsLV: ListView by lazy {
        activityMainBinding.directionSheet.directionsLV
    }

    private val stopList by lazy { mutableListOf<Stop>() }

    private val barriersList by lazy { mutableListOf<PolygonBarrier>() }

    private val directionsList by lazy { mutableListOf<DirectionManeuver>() }

    private val stopsOverlay by lazy { GraphicsOverlay() }

    private val barriersOverlay by lazy { GraphicsOverlay() }

    private val routeOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }

    private val barrierSymbol by lazy {
        SimpleFillSymbol(SimpleFillSymbolStyle.DiagonalCross, Color.red, null)
    }

    // create route task from San Diego service
    private val routeTask by lazy {
        RouteTask(getString(R.string.routing_service_url))
    }

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

        // create and add a map with a navigation night basemap style
        mapView.apply {
            map = ArcGISMap(BasemapStyle.ArcGISStreets)
            setViewpoint(Viewpoint(32.7270, -117.1750, 40000.0))
            graphicsOverlays.addAll(listOf(stopsOverlay, barriersOverlay, routeOverlay))
        }

        // set an on touch listener on the map view
        lifecycleScope.launch {
            mapView.onSingleTapConfirmed.collect { event ->
                // add stop or barriers graphics to overlay
                event.mapPoint?.let { mapPoint -> addStopOrBarrier(mapPoint) }
                resetButton.isEnabled = true
            }
        }

        // coroutine scope to use the default parameters for the route calculation
        lifecycleScope.launch {
            routeTask.load().onSuccess {
                routeParameters = routeTask.createDefaultParameters().getOrThrow().apply {
                    returnStops = true
                    returnDirections = true
                }
            }.onFailure {
                showError(it.message.toString())
            }
        }

        // make a clear button to reset the stops and routes
        resetButton.setOnClickListener {
            // clear stops from route parameters and stops list
            routeParameters?.clearStops()
            stopList.clear()
            // clear barriers from route parameters and barriers list
            routeParameters?.clearPolygonBarriers()
            barriersList.clear()
            // clear the directions list
            directionsList.clear()
            // clear all graphics overlays
            mapView.graphicsOverlays.forEach { it.graphics.clear() }
            resetButton.isEnabled = false
        }

        // display the options dialog having the route finding parameters
        optionsButton.setOnClickListener {
            displayOptionsDialog()
        }

        // display the bottom sheet with directions when the button is clicked
        directionsButton.setOnClickListener {
            setupBottomSheet(directionsList)
        }

        // hide the bottom sheet and make the map view span the whole screen
        bottomSheet.visibility = View.INVISIBLE
        (mainContainer.layoutParams as CoordinatorLayout.LayoutParams).bottomMargin = 0
    }

    /**
     * Create options dialog with the route finding parameters to reorder stops to find the optimized route
     */
    private fun displayOptionsDialog() {
        // removes parent of the progressDialog layout, if previously assigned
        optionsDialogBinding.root.parent?.let { parent ->
            (parent as ViewGroup).removeAllViews()
        }

        // set up the dialog builder
        MaterialAlertDialogBuilder(this).apply {
            setView(optionsDialogBinding.root)
            create()
            show()
        }

        // set the best sequence toggle state
        findBestSequenceSwitch.isChecked = routeParameters?.findBestSequence ?: false

        // solve route on each state change
        findBestSequenceSwitch.setOnCheckedChangeListener { _, _ ->
            // update route params if the switch is toggled
            routeParameters?.findBestSequence = findBestSequenceSwitch.isChecked
            createAndDisplayRoute()

            // if best sequence switch is enabled, then enable the options
            if (findBestSequenceSwitch.isChecked) {
                firstStopSwitch.isEnabled = true
                lastStopSwitch.isEnabled = true

            } else {
                firstStopSwitch.apply {
                    isChecked = false
                    isEnabled = false
                }
                lastStopSwitch. apply {
                    isChecked = false
                    isEnabled = false
                }
            }
        }
        firstStopSwitch.setOnCheckedChangeListener { _, _ ->
            routeParameters?.preserveFirstStop = firstStopSwitch.isChecked
            createAndDisplayRoute()
        }
        lastStopSwitch.setOnCheckedChangeListener { _, _ ->
            routeParameters?.preserveLastStop = lastStopSwitch.isChecked
            createAndDisplayRoute()
        }
    }

    /**
     * Add a stop or a barrier at the selected [mapPoint] to the correct graphics
     * overlay depending on which button is currently checked.
     */
    private fun addStopOrBarrier(mapPoint: Point) {
        if (addStopsButton.isChecked) {
            // normalize the geometry - needed if the user crosses the international date line.
            val normalizedPoint = GeometryEngine.normalizeCentralMeridian(mapPoint) as Point
            // use the mapPoint to create a stop
            val stop = Stop(Point(normalizedPoint.x, normalizedPoint.y, mapPoint.spatialReference))
            // add the new stop to the list of stops
            stopList.add(stop)
            // create a marker symbol and graphics, and add the graphics to the graphics overlay
            stopsOverlay.graphics.add(Graphic(mapPoint, createStopSymbol(stopList.size)))
        } else if (addBarriersButton.isChecked) {
            // create a buffered polygon around the clicked point
            val barrierBufferPolygon = GeometryEngine.buffer(mapPoint, 200.0)
            // create a polygon barrier for the routing task, and add it to the list of barriers
            barrierBufferPolygon?.let {
                barriersList.add(PolygonBarrier(it))
            }
            barriersOverlay.graphics.add(Graphic(barrierBufferPolygon, barrierSymbol))
        }
        // solve the route once the graphics are created
        createAndDisplayRoute()
    }

    /**
     * Create route parameters and a route task from them. Display the route result geometry as a
     * graphic and call showDirectionsInBottomSheet which shows directions in a list view.
     */
    private fun createAndDisplayRoute() = lifecycleScope.launch {

        // clear the previous route from the graphics overlay, if it exists
        routeOverlay.graphics.clear()
        // clear the directions list from the directions list view, if they exist
        directionsList.clear()

        val routeParameters = routeParameters ?: return@launch

        if (stopList.size <= 1) return@launch

        routeParameters.apply {
            // add the existing stops and barriers to the route parameters
            setStops(stopList)
            setPolygonBarriers(barriersList)
        }

        // solve the route task
        val routeResults = routeParameters.let { routeTask.solveRoute(it) }

        routeResults.onSuccess { routeResult ->
            // get the first solved route
            val firstRoute = routeResult.routes[0]

            // create Graphic for route
            val graphic = Graphic(
                firstRoute.routeGeometry,
                SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 3f)
            )
            routeOverlay.graphics.add(graphic)
            // get the direction text for each maneuver and add them to the list to display
            directionsList.addAll(firstRoute.directionManeuvers)
        }.onFailure {
            showError("No route solution. ${it.message}")
        }

    }

    /** Creates a bottom sheet to display a list of direction maneuvers.
     *  [directions] a list of DirectionManeuver which represents the route
     */
    private fun setupBottomSheet(directions: List<DirectionManeuver>) {
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet).apply {
            // expand the bottom sheet, and ensure it is displayed on the screen when collapsed
            state = BottomSheetBehavior.STATE_EXPANDED
            peekHeight = header.height
            // animate the arrow when the bottom sheet slides
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    imageView.rotation = slideOffset * 180f
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    imageView.rotation = when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> 180f
                        else -> imageView.rotation
                    }
                }
            })
        }

        bottomSheet.apply {
            visibility = View.VISIBLE
            // expand or collapse the bottom sheet when the header is clicked
            header.setOnClickListener {
                bottomSheetBehavior.state = when (bottomSheetBehavior.state) {
                    BottomSheetBehavior.STATE_COLLAPSED -> BottomSheetBehavior.STATE_EXPANDED
                    else -> BottomSheetBehavior.STATE_COLLAPSED
                }

            }
            // rotate the arrow so it starts off in the correct rotation
            imageView.rotation = 180f

            directionsLV.apply {
                // set the adapter for the list view
                adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_list_item_1,
                    directions.map { it.directionText }
                )

                // when the user taps a maneuver, set the viewpoint to that portion of the route
                onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, position, _ ->
                        // remove any graphics that are not the original (blue) route graphic
                        if (routeOverlay.graphics.size > 1) {
                            routeOverlay.graphics.removeAt(routeOverlay.graphics.size - 1)
                        }
                        // set the viewpoint to the selected maneuver
                        val geometry = directionsList[position].geometry
                        geometry?.let { mapView.setViewpoint(Viewpoint(it.extent, 20.0)) }
                        // create a graphic with a symbol for the maneuver and add it to the graphics overlay
                        val selectedRouteSymbol = SimpleLineSymbol(
                            SimpleLineSymbolStyle.Solid,
                            Color.green, 3f
                        )
                        routeOverlay.graphics.add(Graphic(geometry, selectedRouteSymbol))
                        // collapse the bottom sheet
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
            }
            // hide the bottom sheet when cancel button is clicked
            cancelTV.setOnClickListener {
                bottomSheet.visibility = View.INVISIBLE
            }
        }

    }

    /**
     * Create a composite symbol consisting of a pin graphic overlaid with a particular [stopNumber].
     * Returns a [CompositeSymbol] consisting of the pin graphic overlaid with the stop number
     */
    private fun createStopSymbol(stopNumber: Int): CompositeSymbol {
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

        // create a new picture marker from a pin drawable
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

        // create a composite symbol and add the picture marker symbol and text symbol
        val compositeSymbol = CompositeSymbol()
        compositeSymbol.symbols.addAll(listOf(pinSymbol, stopNumberSymbol))

        return compositeSymbol
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
