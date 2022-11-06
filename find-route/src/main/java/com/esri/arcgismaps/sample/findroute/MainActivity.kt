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

package com.esri.arcgismaps.sample.findroute

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.tasks.networkanalysis.DirectionManeuver
import com.arcgismaps.tasks.networkanalysis.Route
import com.arcgismaps.tasks.networkanalysis.RouteParameters
import com.arcgismaps.tasks.networkanalysis.RouteResult
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import com.esri.arcgismaps.sample.findroute.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val graphicsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }

    private val mapView: MapView by lazy {
        activityMainBinding.mapView
    }

    private val mainContainer: ConstraintLayout by lazy {
        activityMainBinding.mainContainer
    }

    private val mainProgressBar: ProgressBar by lazy {
        activityMainBinding.mainProgressBar
    }

    private val directionFab: FloatingActionButton by lazy {
        activityMainBinding.directionFab
    }

    private val bottomSheet: LinearLayout by lazy {
        activityMainBinding.bottomSheet.bottomSheetLayout
    }

    private val header: ConstraintLayout by lazy {
        activityMainBinding.bottomSheet.header
    }

    private val imageView: ImageView by lazy {
        activityMainBinding.bottomSheet.imageView
    }

    private val directionsListView: ListView by lazy {
        activityMainBinding.bottomSheet.directionsListView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(activityMainBinding.mapView)

        mapView.apply {
            // set the map to a new map with the navigation base map
            map = ArcGISMap(BasemapStyle.ArcGISNavigation)
            // set initial viewpoint to San Diego
            setViewpoint(Viewpoint(32.7157, -117.1611, 200000.0))
            mapView.graphicsOverlays.add(graphicsOverlay)
        }

        // create the symbols for the route
        setupSymbols()

        // hide the bottom sheet and make the map view span the whole screen
        bottomSheet.visibility = View.INVISIBLE
        (mainContainer.layoutParams as CoordinatorLayout.LayoutParams).bottomMargin = 0

        // solve the route and display the bottom sheet when the FAB is clicked
        directionFab.setOnClickListener { lifecycleScope.launch { solveRoute() } }
    }

    /**
     * Solves the route using a Route Task, populates the navigation drawer with the directions,
     * and displays a graphic of the route on the map.
     */
    private suspend fun solveRoute() {
        // create a route task instance
        val routeTask =
            RouteTask(
                "https://route-api.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World"
            )

        // show the progress bar
        mainProgressBar.visibility = View.VISIBLE
        routeTask.createDefaultParameters().onSuccess { routeParams ->
            // create stops
            val stops = arrayListOf(
                Stop(Point(-117.1508, 32.7411, SpatialReference.wgs84())),
                Stop(Point(-117.1555, 32.7033, SpatialReference.wgs84()))
            )

            routeParams.apply {
                setStops(stops)
                // set return directions as true to return turn-by-turn directions in the route's directionManeuvers
                returnDirections = true
            }

            // solve the route
            val routeResult = routeTask.solveRoute(routeParams).getOrElse {
                showError(it.message.toString())
            } as RouteResult
            val route = routeResult.routes[0]
            // create a simple line symbol for the route
            val routeSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.blue, 5f)

            // create a graphic for the route and add it to the graphics overlay
            graphicsOverlay.graphics.add(Graphic(route.routeGeometry, routeSymbol))
            // get the list of direction maneuvers and display it
            // NOTE: to get turn-by-turn directions the route parameters
            //  must have the isReturnDirections parameter set to true.
            val directions = route.directionManeuvers
            setupBottomSheet(directions)

            // when the route is solved, hide the FAB and the progress bar
            directionFab.visibility = View.GONE
            mainProgressBar.visibility = View.GONE
        }.onFailure {
            showError(it.message.toString())
            mainProgressBar.visibility = View.GONE
        }
    }

    /** Creates a bottom sheet to display a list of direction maneuvers.
     *  [directions] a list of DirectionManeuver which represents the route
     */
    private fun setupBottomSheet(directions: List<DirectionManeuver>) {
        // create a bottom sheet behavior from the bottom sheet view in the main layout
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
        }

        directionsListView.apply {
            // Set the adapter for the list view
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_list_item_1,
                directions.map { it.directionText }
            )
            // when the user taps a maneuver, set the viewpoint to that portion of the route
            onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ ->
                    // remove any graphics that are not the two stops and the route graphic
                    if (graphicsOverlay.graphics.size > 3) {
                        graphicsOverlay.graphics.removeAt(graphicsOverlay.graphics.size - 1)
                    }
                    // set the viewpoint to the selected maneuver
                    val geometry = directions[position].geometry
                    if (geometry != null) {
                        mapView.setViewpoint(
                            Viewpoint(geometry.extent, 20.0)
                        )
                    }
                    // create a graphic with a symbol for the maneuver and add it to the graphics overlay
                    val selectedRouteSymbol = SimpleLineSymbol(
                        SimpleLineSymbolStyle.Solid,
                        Color.green, 5f
                    )
                    graphicsOverlay.graphics.add(Graphic(geometry, selectedRouteSymbol))
                    // collapse the bottom sheet
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
        }

        // shrink the map view so it is not hidden under the bottom sheet header
        (mainContainer.layoutParams as CoordinatorLayout.LayoutParams).bottomMargin =
            header.height
    }

    /**
     * Set up the source, destination and route symbols.
     */
    private fun setupSymbols() {
        val startDrawable =
            ContextCompat.getDrawable(this, R.drawable.ic_source) as BitmapDrawable
        val pinSourceSymbol = PictureMarkerSymbol(startDrawable).apply {
            // make the graphic smaller
            width = 30f
            height = 30f
            offsetY = 20f
        }
        // create a point for the new graphic
        val sourcePoint = Point(
            -117.1508, 32.7411, SpatialReference.wgs84()
        )
        // create a graphic and it to the graphics overlay
        graphicsOverlay.graphics.add(Graphic(sourcePoint, pinSourceSymbol))

        val endDrawable =
            ContextCompat.getDrawable(this, R.drawable.ic_destination) as BitmapDrawable

        endDrawable.let {
            val pinDestinationSymbol =
                PictureMarkerSymbol(endDrawable).apply {
                    // make the graphic smaller
                    width = 30f
                    height = 30f
                    offsetY = 20f
                }
            // create a point for the new graphic
            val destinationPoint = Point(-117.1555, 32.7033, SpatialReference.wgs84())
            // create a graphic and add it to the graphics overlay
            graphicsOverlay.graphics.add(Graphic(destinationPoint, pinDestinationSymbol))
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }

    private val Color.Companion.blue: Color
        get() {
            return fromRgba(0, 0, 255, 255)
        }

}
