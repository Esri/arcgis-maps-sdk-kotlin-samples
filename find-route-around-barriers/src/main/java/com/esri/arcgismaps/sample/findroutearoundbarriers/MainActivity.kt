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
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
import com.esri.arcgismaps.sample.findroutearoundbarriers.databinding.DirectionSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
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

    private val toggleButtons by lazy {
        activityMainBinding.toggleButtons
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

    private val mainContainer: ConstraintLayout by lazy {
        activityMainBinding.mainContainer
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

        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISStreets)
        mapView.map = map
        mapView.setViewpoint(Viewpoint(32.7270, -117.1750, 40000.0))
        mapView.graphicsOverlays.addAll(listOf(stopsOverlay, barriersOverlay, routeOverlay))

        // set an on touch listener on the map view
        lifecycleScope.launch {
            mapView.onSingleTapConfirmed.collect { event ->
                event.mapPoint?.let { mapPoint -> addStopOrBarrier(mapPoint) }
                resetButton.isEnabled = true
            }
        }

        // create route task from San Diego service
        routeTask = RouteTask("https://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route")
        lifecycleScope.launch {
            routeTask?.load()?.onSuccess {
                // use the default parameters for the route calculation
                routeParameters = routeTask?.createDefaultParameters()?.getOrThrow()
                routeParameters?.apply {
                    returnStops = true
                    returnDirections = true
                }
            }?.onFailure {
                showError(it.message.toString())
            }
        }

        // make a clear button to reset the stops and routes
        resetButton.setOnClickListener {
            stopsOverlay.graphics.clear()
            barriersOverlay.graphics.clear()
            routeOverlay.graphics.clear()
            resetButton.isEnabled = false

        }

        optionsButton.setOnClickListener {
            displayOptionsDialog()
        }

        // solve the route and display the bottom sheet when the FAB is clicked
        directionsButton.setOnClickListener {
            setupBottomSheet(directionsList)
        }

        // hide the bottom sheet and make the map view span the whole screen
        bottomSheet.visibility = View.INVISIBLE
        (mainContainer.layoutParams as CoordinatorLayout.LayoutParams).bottomMargin = 0

    }

    private fun displayOptionsDialog() {
        val dialogView: View = layoutInflater.inflate(R.layout.options_dialog, null)
        val dialogBuilder = AlertDialog.Builder(this).apply {
            setView(dialogView)
            create()
            show()
        }
        val reorderSwitch = dialogView.findViewById<SwitchMaterial>(R.id.reorderSwitch)
        val firstStopSwitch = dialogView.findViewById<SwitchMaterial>(R.id.firstStopSwitch)
        val lastStopSwitch = dialogView.findViewById<SwitchMaterial>(R.id.lastStopSwitch)
        reorderSwitch.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                firstStopSwitch.apply {
                    isEnabled = true
                    setChecked(true)
                }

                lastStopSwitch.apply {
                    isEnabled = true
                    setChecked(true)
                }
            }else{
                firstStopSwitch.isEnabled = false
                lastStopSwitch.isEnabled = false
            }
        }
    }

    /**
     * Add a stop or a point to the correct graphics overlay depending on which button is currently
     * checked.
     *
     * @param mapPoint at which to create a stop or point
     */
    private fun addStopOrBarrier(mapPoint: Point) {

        // clear the displayed route, if it exists, since it might not be up to date any more
        routeOverlay.graphics.clear()

        if (addStopsButton.isChecked) {
            // normalize the geometry - needed if the user crosses the international date line.
            val normalizedPoint = GeometryEngine.normalizeCentralMeridian(mapPoint) as Point
            // use the mapPoint to create a stop
            val stop = Stop(Point(normalizedPoint.x, normalizedPoint.y, mapPoint.spatialReference))
            // add the new stop to the list of stops
            stopList.add(stop)
            // create a marker symbol and graphics, and add the graphics to the graphics overlay
            stopsOverlay.graphics.add(Graphic(mapPoint, createStopSymbol(stopList.size)))
        }

        else if (addBarriersButton.isChecked) {
            // create a buffered polygon around the clicked point
            val barrierBufferPolygon = GeometryEngine.buffer(mapPoint, 200.0)
            // create a polygon barrier for the routing task, and add it to the list of barriers
            barrierBufferPolygon?.let {
                barriersList.add(PolygonBarrier(it)) }
            barriersOverlay.graphics.add(Graphic(barrierBufferPolygon, barrierSymbol))
        }
        createAndDisplayRoute()
    }

    /**
     * Create route parameters and a route task from them. Display the route result geometry as a
     * graphic and call showDirectionsInBottomSheet which shows directions in a list view.
     */
    private fun createAndDisplayRoute() = lifecycleScope.launch {
        if (stopList.size <=1) return@launch

        routeParameters?.apply {
            // add the existing stops and barriers to the route parameters
            setStops(stopList)
            setPolygonBarriers(barriersList)
        }

        // solve the route task
        val results = routeParameters?.let { routeTask?.solveRoute(it) }

        if (results != null) {
            results.onSuccess { routeResult ->
                // get the first solved route
                val route = routeResult.routes[0]

                // create Graphic for route
                val graphic = Graphic(
                    route.routeGeometry,
                    SimpleLineSymbol (SimpleLineSymbolStyle.Solid, Color.black, 3f)
                )
                routeOverlay.graphics.clear()
                routeOverlay.graphics.add(graphic)

                directionsList.addAll(route.directionManeuvers)

            }.onFailure {
                showError("No route solution. ${it.message}")
                routeOverlay.graphics.clear()
            }
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
                // Set the adapter for the list view
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
                        geometry?.let { Viewpoint(it.extent, 20.0) }
                            ?.let { mapView.setViewpoint(it) }
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
            cancelTV.setOnClickListener {
                bottomSheet.visibility = View.INVISIBLE
            }
        }

    }
    /**
     * Create a composite symbol consisting of a pin graphic overlaid with a particular stop number.
     *
     * @param stopNumber to overlay the pin symbol
     * @return a composite symbol consisting of the pin graphic overlaid with an the stop number
     */
    private fun createStopSymbol(stopNumber: Int): CompositeSymbol  {
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
