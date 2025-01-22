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

package com.esri.arcgismaps.sample.createkmlmultitrack.components

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.Location
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.location.SimulatedLocationDataSource
import com.arcgismaps.location.SimulationParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.kml.KmlAltitudeMode
import com.arcgismaps.mapping.kml.KmlDataset
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
import com.esri.arcgismaps.sample.createkmlmultitrack.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

class CreateKMLMultiTrackViewModel(application: Application) : AndroidViewModel(application) {
    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() +
                File.separator + application.getString(R.string.create_kml_multi_track_app_name)
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // This should be passed to the composable MapView
    val mapViewProxy = MapViewProxy()

    // Display a map with a street basemap style
    val arcGISMap by mutableStateOf(ArcGISMap(BasemapStyle.ArcGISStreets))

    // Overlay to display kml tracks and location points
    val graphicsOverlay = GraphicsOverlay()

    // Marker symbol to display KmlTrackElements
    private val locationSymbol = SimpleMarkerSymbol(
        style = SimpleMarkerSymbolStyle.Circle,
        color = Color.red,
        size = 10f
    )

    // Line symbol to display KmlTrack
    private val lineSymbol = SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Solid,
        color = Color.black,
        width = 3f
    )

    // Observe the list of KML track elements being added when recording a KML track
    private var _kmlTrackElements = MutableStateFlow<List<KmlTrackElement>>(listOf())
    val kmlTrackElements = _kmlTrackElements.asStateFlow()

    // Observe the list of KML tracks being added for the multi track
    private var _kmlTracks = MutableStateFlow<List<KmlTrack>>(listOf())
    val kmlTracks = _kmlTracks.asStateFlow()

    // Enables the recenter button when not in navigation autopan
    var isRecenterButtonEnabled by mutableStateOf(false)
        private set

    // Updates UI to reflect recording buttons and text information
    var isRecordingTrack by mutableStateOf(false)
        private set

    // Updates the UI to display the tracks of the local .kmz file
    var isShowTracksFromFileEnabled by mutableStateOf(false)
        private set

    // Runs the location display simulation and is canceled when completed.
    private var locationDisplayJob: Job? = null

    // Default location display object, which is updated by rememberLocationDisplay
    private var locationDisplay: LocationDisplay = LocationDisplay()

    // Sets the location display
    fun setLocationDisplay(locationDisplay: LocationDisplay) {
        this.locationDisplay = locationDisplay
    }

    /**
     * Loads the hiking path polyline and starts a simulation down a path.
     * Updates the navigation autopan buttons based on state, and calls [addTrackElement]
     * with the current simulation [Location] when [isRecordingTrack] is enabled.
     */
    suspend fun startNavigation() {
        // Get the hiking path geometry
        val routeGeometry = Geometry.fromJsonOrNull(
            json = getApplication<Application>().getString(R.string.Coastal_Trail)
        ) as Polyline
        // Create a simulated location data source from json data
        // with simulation parameters to set a consistent velocity
        val simulatedLocationDataSource = SimulatedLocationDataSource(
            polyline = routeGeometry,
            parameters = SimulationParameters(
                startTime = Instant.now(),
                velocity = 25.0,
                horizontalAccuracy = 0.0,
                verticalAccuracy = 0.0
            )
        )
        // Set the map's initial viewpoint
        mapViewProxy.setViewpointGeometry(routeGeometry, 25.0)
        // Update sample UI state
        isShowTracksFromFileEnabled = false
        // Create a new job with the following coroutines
        locationDisplayJob = with(viewModelScope) {
            launch {
                // Set the simulated location data source as the location data source for this app
                locationDisplay.dataSource = simulatedLocationDataSource

                // Start the location data source
                locationDisplay.dataSource.start().onFailure {
                    messageDialogVM.showMessageDialog(
                        title = "Error starting location data source",
                        description = it.message.toString()
                    )
                }

                // Set the auto pan to navigation mode
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
            }
            launch {
                // Automatically enable recenter button when navigation pan is disabled
                locationDisplay.autoPanMode.collect {
                    when (it) {
                        LocationDisplayAutoPanMode.Off -> isRecenterButtonEnabled = true
                        LocationDisplayAutoPanMode.Navigation -> isRecenterButtonEnabled = false
                        else -> {}
                    }
                }
            }
            launch {
                // Listen for changes in location
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

    /**
     * When recording is enabled, add the given [location] to the list
     * of [kmlTrackElements] and display the graphic on the map.
     */
    private fun addTrackElement(location: Location) {
        viewModelScope.launch {
            // Convert to WGS_84 as per KML spec
            val positionPoint = GeometryEngine.projectOrNull(
                geometry = location.position,
                spatialReference = SpatialReference.wgs84()
            )
            // Add a new element to the state flow
            _kmlTrackElements.value += KmlTrackElement(
                coordinate = positionPoint,
                instant = Instant.now(),
                angle = null
            )
            // Add a graphic at the location's position
            graphicsOverlay.graphics.add(
                Graphic(
                    geometry = positionPoint,
                    symbol = locationSymbol
                )
            )
        }
    }

    /**
     * Enables recording state to collect position values from the location data source.
     */
    fun startRecordingKmlTrack() {
        isRecordingTrack = true
        _kmlTrackElements.value = listOf()
    }

    /**
     * Disables recording to create a new [KmlTrack] and display the track graphic on the map.
     */
    fun stopRecordingKmlTrack() {
        _kmlTracks.value += KmlTrack(
            elements = _kmlTrackElements.value,
            altitudeMode = KmlAltitudeMode.RelativeToGround
        )

        displayKmlTracks()
        isRecordingTrack = false
    }

    /**
     * Display polylines on the map representing all the recorded KML tracks.
     */
    private fun displayKmlTracks() {
        graphicsOverlay.graphics.clear()
        _kmlTracks.value.forEach { kmlTrack ->
            // Get the map's spacial reference
            val mapSpatialReference = arcGISMap.spatialReference
                ?: return messageDialogVM.showMessageDialog("Error retrieving spacial reference")
            // Set the KML geometry to use the same projection
            val multipoint = (kmlTrack.geometry as Multipoint)
            val polyline = GeometryEngine.projectOrNull(
                geometry = Polyline(multipoint.points),
                spatialReference = mapSpatialReference
            ) ?: return messageDialogVM.showMessageDialog("Error converting geometry spacial reference")
            // Add the polyline graphic to the map
            graphicsOverlay.graphics.add(Graphic(geometry = polyline, symbol = lineSymbol))
        }
    }

    /**
     * Exports the [KmlMultiTrack] as a kmz file to local storage.
     */
    fun exportKmlMultiTrack() {
        // Create a default KML document which will export file to device
        val kmlDocument = KmlDocument()
        // Create a KML multi track using the current list of tracks
        val multiTrack = KmlMultiTrack(_kmlTracks.value)
        // Add the multi track as a placemark KML node to the KML document
        kmlDocument.childNodes.add(KmlPlacemark(geometry = multiTrack))
        // Define the save file path
        val localKmlFile = File(provisionPath, "HikingTracks.kmz").apply {
            if (exists()) delete()
        }
        // Save KML file to local storage
        viewModelScope.launch {
            kmlDocument.saveAs(localKmlFile.canonicalPath).onSuccess {
                Toast.makeText(
                    getApplication(),
                    "Saved KmlMultiTrack: ${localKmlFile.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                messageDialogVM.showMessageDialog(
                    title = it.message.toString(),
                    description = it.cause.toString()
                )
            }
            stopNavigation()
        }
    }

    /**
     * Stop the [locationDisplay] and [locationDisplayJob]. Updates UI to display results.
     */
    private fun stopNavigation() {
        viewModelScope.launch {
            if (locationDisplayJob?.isActive == true) {
                locationDisplay.dataSource.stop()
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Off)
                locationDisplayJob?.cancelAndJoin()
            }
            isShowTracksFromFileEnabled = true
        }
    }

    /**
     * Called from screen to load the KML file from local storage.
     * Once loaded, [onLocalKmlFileLoaded] is invoked with the KML multi track contents.
     */
    suspend fun loadLocalKmlFile(onLocalKmlFileLoaded: (List<Geometry>) -> Unit) {
        // Create the file path for the local KML file
        val localKmlFile = File(provisionPath, "HikingTracks.kmz")
        // Check if file exists
        if (!localKmlFile.exists())
            return messageDialogVM.showMessageDialog("Error locating KML file")
        // Create a KML dataset using the local file path
        val localKmlDataset = KmlDataset(localKmlFile.canonicalPath)
        // Load the KML dataset
        localKmlDataset.load().onFailure {
            return messageDialogVM.showMessageDialog("Error parsing KML file")
        }
        // Get the document's node which contains the placemark
        val kmlDocument = localKmlDataset.rootNodes.first() as KmlDocument
        val kmlPlacemark = kmlDocument.childNodes.first() as KmlPlacemark
        // Get the multi track geometry from the placemark
        val kmlMultiTrack = kmlPlacemark.kmlGeometry as KmlMultiTrack
        // Calculate the union of all the KML tracks
        val allTracksGeometry = GeometryEngine.unionOrNull(kmlMultiTrack.tracks.map { it.geometry })
            ?: return messageDialogVM.showMessageDialog("KmlMultiTrack has no geometry")
        // Set the viewpoint to the union geometry
        mapViewProxy.setViewpointGeometry(
            boundingGeometry = allTracksGeometry,
            paddingInDips = 25.0
        )
        // Add the other individual track geometry as well
        val trackGeometries = mutableListOf(allTracksGeometry).apply {
            addAll(kmlMultiTrack.tracks.map { it.geometry })
        }
        // Invoke UI to display the list of geometry tracks
        onLocalKmlFileLoaded(trackGeometries)
    }

    /**
     * Update viewpoint to display [kmlTrackGeometry].
     */
    fun previewKmlTrack(kmlTrackGeometry: Geometry) {
        viewModelScope.launch {
            mapViewProxy.setViewpointGeometry(
                boundingGeometry = kmlTrackGeometry,
                paddingInDips = 25.0
            )
        }
    }

    /**
     * Sets the autopan mode to navigation, and update UI.
     */
    fun recenter() {
        locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
        isRecenterButtonEnabled = false
    }

    /**
     * Resets UI and map graphics.
     */
    fun reset() {
        _kmlTracks.value = listOf()
        graphicsOverlay.graphics.clear()
        isShowTracksFromFileEnabled = false
    }
}
