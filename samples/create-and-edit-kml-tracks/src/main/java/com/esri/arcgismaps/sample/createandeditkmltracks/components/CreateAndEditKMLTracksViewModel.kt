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
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.Location
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.location.SimulatedLocationDataSource
import com.arcgismaps.location.SimulationParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.kml.KmlAltitudeMode
import com.arcgismaps.mapping.kml.KmlDocument
import com.arcgismaps.mapping.kml.KmlMultiTrack
import com.arcgismaps.mapping.kml.KmlPlacemark
import com.arcgismaps.mapping.kml.KmlTrack
import com.arcgismaps.mapping.kml.KmlTrackElement
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.createandeditkmltracks.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.io.File
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

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    var isTrackLocation by mutableStateOf(false)

    var isRecenterButtonEnabled by mutableStateOf(false)
    var isRecordingTrack by mutableStateOf(false)

    val graphicsOverlay = GraphicsOverlay()

    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() +
                File.separator +
                application.getString(R.string.create_and_edit_kml_tracks_app_name)
    }

    private val lineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.green, 3f)

    // keep track of the the location display job when navigation is enabled
    private var locationDisplayJob: Job? = null

    // default location display object, which is updated by rememberLocationDisplay
    private var locationDisplay: LocationDisplay = LocationDisplay()

    // private var simulatedLocationDataSource: SimulatedLocationDataSource? = null

    fun setLocationDisplay(locationDisplay: LocationDisplay) {
        this.locationDisplay = locationDisplay
    }

    private val kmlDocument = KmlDocument()
    val kmlTrackElements = mutableListOf<KmlTrackElement>()
    val kmlTracks = mutableListOf<KmlTrack>()

    init {
        // get the route geometry
        val routeGeometry = Geometry.fromJsonOrNull(
            json = application.getString(R.string.polyline_data)
        ) as Polyline
        // create a simulated location data source from json data with simulation parameters to set a consistent velocity
        val simulatedLocationDataSource = SimulatedLocationDataSource(
            polyline = routeGeometry,
            parameters = SimulationParameters(
                startTime = Instant.now(),
                velocity = 30.0,
                horizontalAccuracy = 0.0,
                verticalAccuracy = 0.0
            )
        )


        viewModelScope.launch {
            arcGISMap.load().getOrElse { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map",
                    error.message.toString()
                )
            }

            startNavigation(simulatedLocationDataSource)
        }
    }

    private fun startNavigation(simulationDataSource: SimulatedLocationDataSource) {
        locationDisplayJob = with(viewModelScope) {
            launch {
                // automatically enable recenter button when navigation pan is disabled
                locationDisplay.autoPanMode.filter { it == LocationDisplayAutoPanMode.Off }
                    .collect {
                        isRecenterButtonEnabled = true
                    }
            }
            launch {
                // set the simulated location data source as the location data source for this app
                locationDisplay.dataSource = simulationDataSource

                // start the location data source
                locationDisplay.dataSource.start().getOrElse {
                    messageDialogVM.showMessageDialog(
                        title = "Error starting location data source",
                        description = it.message.toString()
                    )
                }

                // set the auto pan to navigation mode
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
            }

            launch {
                // listen for changes in location
                locationDisplay.location.collect {
                    it?.let { locationPoint ->
                        if (isRecordingTrack) {
                            addTrackElement(locationPoint)
                        }
                    }
                }
            }
        }
    }

    private fun addTrackElement(locationPoint: Location) {
        kmlTrackElements.add(
            KmlTrackElement(
                instant = Instant.now(),
                coordinate = locationPoint.position,
                angle = null
            )
        )

        graphicsOverlay.graphics.add(Graphic(locationPoint.position, locationSymbol))
    }

    fun startRecordingKmlTrack() {
        isRecordingTrack = true
        kmlTrackElements.clear()
        graphicsOverlay.graphics.clear()
    }

    fun stopRecordingKmlTrack() {
        kmlTracks.add(
            KmlTrack(
                elements = kmlTrackElements,
                altitudeMode = KmlAltitudeMode.RelativeToGround,
                isExtruded = true,
                isTessellated = true,
                model = null
            )
        )

        displayKmlTracks()

        isRecordingTrack = false
    }

    fun exportKmlMultiTrack() {
        val multiTrack = KmlMultiTrack(kmlTracks)
        kmlDocument.childNodes.add(KmlPlacemark(geometry = multiTrack))

        val savedKmzFile = File(provisionPath, "HikingTracks.kmz").apply {
            if (exists()) delete()
        }

        viewModelScope.launch {
            kmlDocument.saveAs(savedKmzFile.canonicalPath).onSuccess {
                messageDialogVM.showMessageDialog(
                    title = "Saved KmlMultiTrack",
                    description = "Path: " + savedKmzFile.canonicalPath
                )
            }.onFailure {
                messageDialogVM.showMessageDialog(
                    title = it.message.toString(),
                    description = it.cause.toString()
                )
            }
        }

        resetKmlTrack()
    }

    private fun resetKmlTrack() {

    }

    private fun displayKmlTracks() {
        graphicsOverlay.graphics.clear()
        kmlTracks.forEach {
            val multipoint = it.geometry as Multipoint
            graphicsOverlay.graphics.add(Graphic(Polyline(multipoint.points), symbol = lineSymbol))
        }
    }

    fun recenter() {
        locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
        isRecenterButtonEnabled = false
    }

}
