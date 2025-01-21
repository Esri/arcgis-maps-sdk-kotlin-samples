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
import com.esri.arcgismaps.sample.createandeditkmltracks.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

class CreateAndEditKMLTracksViewModel(application: Application) : AndroidViewModel(application) {

    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(
            R.string.create_and_edit_kml_tracks_app_name
        )
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    val mapViewProxy = MapViewProxy()
    val graphicsOverlay = GraphicsOverlay()
    val arcGISMap by mutableStateOf(ArcGISMap(BasemapStyle.ArcGISStreets))

    private val locationSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.red, 10f)

    private val lineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 3f)

    // keep track of the the location display job when navigation is enabled
    private var locationDisplayJob: Job? = null

    // default location display object, which is updated by rememberLocationDisplay
    private var locationDisplay: LocationDisplay = LocationDisplay()

    fun setLocationDisplay(locationDisplay: LocationDisplay) {
        this.locationDisplay = locationDisplay
    }

    private val kmlDocument = KmlDocument()

    private var _kmlTrackElements = MutableStateFlow<List<KmlTrackElement>>(listOf())
    val kmlTrackElements = _kmlTrackElements.asStateFlow()

    private var _kmlTracks = MutableStateFlow<List<KmlTrack>>(listOf())
    val kmlTracks = _kmlTracks.asStateFlow()

    var isRecenterButtonEnabled by mutableStateOf(false)
        private set
    var isRecordingTrack by mutableStateOf(false)
        private set
    var isPreviewTracksEnabled by mutableStateOf(false)
        private set

    suspend fun startNavigation() {
        arcGISMap.load().getOrElse { error ->
            messageDialogVM.showMessageDialog(
                "Failed to load map",
                error.message.toString()
            )
        }

        // get the route geometry
        val routeGeometry = Geometry.fromJsonOrNull(
            json = getApplication<Application>().getString(R.string.Coastal_Trail)
        ) as Polyline

        // create a simulated location data source from json data with simulation parameters to set a consistent velocity
        val simulatedLocationDataSource = SimulatedLocationDataSource(
            polyline = routeGeometry,
            parameters = SimulationParameters(
                startTime = Instant.now(),
                velocity = 25.0,
                horizontalAccuracy = 0.0,
                verticalAccuracy = 0.0
            )
        )

        mapViewProxy.setViewpointGeometry(routeGeometry, 20.0)

        isPreviewTracksEnabled = false
        locationDisplayJob = with(viewModelScope) {
            launch {
                // automatically enable recenter button when navigation pan is disabled
                locationDisplay.autoPanMode.collect {
                    when (it) {
                        LocationDisplayAutoPanMode.Off -> {
                            isRecenterButtonEnabled = true
                        }

                        LocationDisplayAutoPanMode.Navigation -> {
                            isRecenterButtonEnabled = false
                        }

                        else -> {}
                    }
                }
            }
            launch {
                // set the simulated location data source as the location data source for this app
                locationDisplay.dataSource = simulatedLocationDataSource

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

    private fun addTrackElement(location: Location) {
        viewModelScope.launch(Dispatchers.IO) {
            // convert to map view's spacial reference
            val positionPoint = GeometryEngine.projectOrNull(
                geometry = location.position,
                spatialReference = SpatialReference.wgs84()
            )

            _kmlTrackElements.value += KmlTrackElement(
                coordinate = positionPoint,
                instant = Instant.now(),
                // coordinate = location.position,
                angle = null
            )

            graphicsOverlay.graphics.add(
                Graphic(
                    geometry = positionPoint,
                    // geometry = location.position,
                    symbol = locationSymbol
                )
            )
        }
    }

    fun startRecordingKmlTrack() {
        isRecordingTrack = true
        _kmlTrackElements.value = listOf()
    }

    fun stopRecordingKmlTrack() {
        _kmlTracks.value += KmlTrack(
            elements = _kmlTrackElements.value,
            altitudeMode = KmlAltitudeMode.RelativeToGround,
            isExtruded = true,
            isTessellated = true,
            model = null
        )

        displayKmlTracks()

        isRecordingTrack = false
    }

    fun exportKmlMultiTrack() {
        val multiTrack = KmlMultiTrack(_kmlTracks.value)
        kmlDocument.childNodes.add(KmlPlacemark(geometry = multiTrack))

        val localKmlFile = File(provisionPath, "HikingTracks.kmz").apply {
            if (exists()) delete()
        }

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

    private fun stopNavigation() {
        viewModelScope.launch {
            if (locationDisplayJob?.isActive == true) {
                locationDisplay.dataSource.stop()
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Off)
                locationDisplayJob?.cancelAndJoin()
            }
            isPreviewTracksEnabled = true
        }
    }

    private fun displayKmlTracks() {
        graphicsOverlay.graphics.clear()
        _kmlTracks.value.forEach { kmlTrack ->
            val mapSpatialReference = arcGISMap.spatialReference
                ?: return messageDialogVM.showMessageDialog("Error retrieving spacial-ref")

            val multipoint = (kmlTrack.geometry as Multipoint)
            val polyline = GeometryEngine.projectOrNull(
                geometry = Polyline(multipoint.points),
                spatialReference = mapSpatialReference
            ) ?: return messageDialogVM.showMessageDialog("Error converting geometry spacial-ref")

            graphicsOverlay.graphics.add(Graphic(geometry = polyline, symbol = lineSymbol))
        }
    }

    suspend fun loadLocalKmlFile(onLocalKmlFileLoaded: (KmlMultiTrack) -> Unit) {
        val localKmlFile = File(provisionPath, "HikingTracks.kmz")

        if (!localKmlFile.exists())
            return messageDialogVM.showMessageDialog("Error locating KML file")

        val localKmlDataset = KmlDataset(localKmlFile.canonicalPath)

        localKmlDataset.load().getOrElse {
            return messageDialogVM.showMessageDialog("Error parsing KML file")
        }

        val kmlDocument = localKmlDataset.rootNodes.first() as KmlDocument
        val kmlPlacemark = kmlDocument.childNodes.first() as KmlPlacemark
        val kmlMultiTrack = kmlPlacemark.kmlGeometry as KmlMultiTrack

        onLocalKmlFileLoaded(kmlMultiTrack)

        val unionGeometry = GeometryEngine.unionOrNull(kmlMultiTrack.tracks.map { it.geometry })
            ?: return messageDialogVM.showMessageDialog("KmlMultiTrack has no geometry")

        mapViewProxy.setViewpointGeometry(
            boundingGeometry = unionGeometry,
            paddingInDips = 20.0
        )
    }

    fun previewKmlTrack(kmlTrackGeometry: Geometry) {
        viewModelScope.launch {
            mapViewProxy.setViewpointGeometry(kmlTrackGeometry, 20.0)
        }
    }

    fun recenter() {
        locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
        isRecenterButtonEnabled = false
    }

    fun reset() {
        isPreviewTracksEnabled = false
        _kmlTracks.value = listOf()
        graphicsOverlay.graphics.clear()
    }
}
