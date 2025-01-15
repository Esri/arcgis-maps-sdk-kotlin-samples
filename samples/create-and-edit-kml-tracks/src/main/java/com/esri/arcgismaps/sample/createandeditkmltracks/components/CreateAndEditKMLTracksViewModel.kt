/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.createandeditkmltracks.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.location.SimulatedLocationDataSource
import com.arcgismaps.location.SimulationParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.createandeditkmltracks.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant

class CreateAndEditKMLTracksViewModel(application: Application) : AndroidViewModel(application) {

    val mapViewProxy = MapViewProxy()

    // create a center point for the data in West Los Angeles, California
    val center = Point(-13185535.98, 4037766.28, SpatialReference(102100))

    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISNavigationNight).apply {
            initialViewpoint = Viewpoint(center, 7000.0)
        }
    )

    // create a graphics overlay for the points and use a red circle for the symbols
    private val locationSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.red, 10f)
    val locationHistoryOverlay = GraphicsOverlay().apply {
        renderer = SimpleRenderer(locationSymbol)
    }

    // create a graphics overlay for the lines connecting the points and use a blue line for the symbol
    private val locationLineSymbol =
        SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.green, 2.0f)
    val locationHistoryLineOverlay = GraphicsOverlay().apply {
        renderer = SimpleRenderer(locationLineSymbol)
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    var isTrackLocation by mutableStateOf(false)

    // keep track of the the location display job when navigation is enabled
    private var locationDisplayJob: Job? = null

    // default location display object, which is updated by rememberLocationDisplay
    private var locationDisplay: LocationDisplay = LocationDisplay()

    fun setLocationDisplay(locationDisplay: LocationDisplay) {
        this.locationDisplay = locationDisplay
    }

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map",
                    error.message.toString()
                )
            }
        }

        // create a polyline builder to connect the location points
        val polylineBuilder = PolylineBuilder(SpatialReference(102100))

        // create a simulated location data source from json data with simulation parameters to set a consistent velocity
        val simulatedLocationDataSource = SimulatedLocationDataSource(
            Geometry.fromJsonOrNull(application.getString(R.string.polyline_data)) as Polyline,
            SimulationParameters(Instant.now(), 30.0, 0.0, 0.0)
        )

        // coroutine scope to collect data source location changes
        viewModelScope.launch {
            simulatedLocationDataSource.locationChanged.collect { location ->
                // if location tracking is turned off, do not add to the polyline
                if (!isTrackLocation) {
                    return@collect
                }
                // get the point from the location
                val nextPoint = location.position
                // add the new point to the polyline builder
                polylineBuilder.addPoint(nextPoint)
                // add the new point to the two graphics overlays and reset the line connecting the points
                locationHistoryOverlay.graphics.add(Graphic(nextPoint))
                locationHistoryLineOverlay.graphics.apply {
                    clear()
                    add((Graphic(polylineBuilder.toGeometry())))
                }
            }
        }

        // configure the map view's location display to follow the simulated location data source
        locationDisplay.apply {
            dataSource = simulatedLocationDataSource
            setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
            initialZoomScale = 7000.0
        }

        // coroutine scope to start the simulated location data source
        viewModelScope.launch {
            simulatedLocationDataSource.start()

            // set the auto pan to navigation mode
            locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
        }
    }
}
