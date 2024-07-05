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

package com.esri.arcgismaps.sample.findclosestfacilityfrompoint.components

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.tasks.networkanalysis.ClosestFacilityParameters
import com.arcgismaps.tasks.networkanalysis.ClosestFacilityResult
import com.arcgismaps.tasks.networkanalysis.ClosestFacilityTask
import com.arcgismaps.tasks.networkanalysis.Facility
import com.arcgismaps.tasks.networkanalysis.Incident
import com.esri.arcgismaps.sample.findclosestfacilityfrompoint.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class MapViewModel(private var application: Application) : AndroidViewModel(application) {

    val currentJob = mutableStateOf<Job?>(null)
    val map = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
        initialViewpoint = Viewpoint(
            latitude = 32.727, longitude = -117.1750, scale = 100000.0
        )
    }

    // Hardcoded list of facilities
    private var facilitiesList = listOf(
        Facility(Point(-1.3042129900625112E7, 3860127.9479775648, SpatialReference.webMercator())),
        Facility(Point(-1.3042193400557665E7, 3862448.873041752, SpatialReference.webMercator())),
        Facility(Point(-1.3046882875518233E7, 3862704.9896770366, SpatialReference.webMercator())),
        Facility(Point(-1.3040539754780494E7, 3862924.5938606677, SpatialReference.webMercator())),
        Facility(Point(-1.3042571225655518E7, 3858981.773018156, SpatialReference.webMercator())),
        Facility(Point(-1.3039784633928463E7, 3856692.5980474586, SpatialReference.webMercator())),
        Facility(Point(-1.3049023883956768E7, 3861993.789732541, SpatialReference.webMercator()))
    )

    // Create graphic overlay of facilities to display them on the map when user opens app
    private val facilityGraphicsOverlay = GraphicsOverlay()

    // Create graphic overlay of incident to display the incident once user taps on map
    val incidentGraphicsOverlay = GraphicsOverlay()

    val graphicsOverlays = listOf(facilityGraphicsOverlay, incidentGraphicsOverlay)


    init {
        createFacilitiesAndGraphics()
    }

    /**
     * Retrieve the incident point and initiate the process to finding the route between the incident and closest facility
     *
     */
    fun onSingleTapConfirmed(
        currentJob: MutableState<Job?>,
        event: SingleTapConfirmedEvent,
        incidentGraphicsOverlay: GraphicsOverlay
    ) {

        // Cancel previous job
        currentJob.value?.cancel()

        // Retrieve the tapped map point from the SingleTapConfirmedEvent
        val mapPoint: Point =
            event.mapPoint ?: return showMessage("No map point retrieved from tap.")

        val incidentPoint = Incident(mapPoint)

        clearIncident(incidentGraphicsOverlay)
        addIncident(incidentPoint, incidentGraphicsOverlay)
        currentJob.value = viewModelScope.launch {
            findRoute(Incident(mapPoint), incidentGraphicsOverlay)
        }
    }

    /**
     * Facility markers are drawn on the map right when the user opens app
     *
     */
    private fun createFacilitiesAndGraphics() {

        // Add all facility centers on the map
        val facilityUrl = application.getString(R.string.hospital_symbol_url)
        val facilitySymbol = PictureMarkerSymbol(facilityUrl)
        facilitySymbol.height = 30F
        facilitySymbol.width = 30F
        for (facility in facilitiesList) {
            val facilityGraphic = Graphic(
                geometry = facility.geometry, symbol = facilitySymbol
            )
            facilityGraphicsOverlay.graphics.add(facilityGraphic)
        }

    }

    /**
     * Clear the incident marker from the map
     *
     */
    private fun clearIncident(
        incidentGraphicsOverlay: GraphicsOverlay
    ) {
        incidentGraphicsOverlay.graphics.clear()
    }

    /**
     * Add the incident marker on map
     *
     */
    private fun addIncident(
        incidentPoint: Incident, incidentGraphicsOverlay: GraphicsOverlay
    ) {

        // Create a cross symbol for the incident
        val incidentMarker = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Cross, color = Color.black, size = 20.0F
        )

        // Get the incident's geometry
        val incidentStopGeometry = incidentPoint.geometry

        // Add graphic to incident graphic overlays
        incidentGraphicsOverlay.graphics.add(
            Graphic(
                geometry = incidentStopGeometry, symbol = incidentMarker
            )
        )

    }

    /**
     * Find the route between the given incident point the closest facility
     *
     */
    private suspend fun findRoute(
        incidentPoint: Incident, incidentGraphicsOverlay: GraphicsOverlay
    ) {
        val closestFacilityTask = ClosestFacilityTask(
            url = application.getString(R.string.san_diego_network_service_url)
        )

        // Create a job to find the route
        try {
            val closestFacilityParameters: ClosestFacilityParameters =
                closestFacilityTask.createDefaultParameters().getOrThrow()
            closestFacilityParameters.setFacilities(facilitiesList)
            closestFacilityParameters.setIncidents(listOf(incidentPoint))


            // Solve a route using the route parameters created
            val closestFacilityResult: ClosestFacilityResult =
                closestFacilityTask.solveClosestFacility(closestFacilityParameters).getOrThrow()
            val rankedFacilitiesList = closestFacilityResult.getRankedFacilityIndexes(0)

            // If we got a list of facilities
            if (rankedFacilitiesList.isNotEmpty()) {
                val closestFacilityIndex = rankedFacilitiesList[0]
                val route = closestFacilityResult.getRoute(closestFacilityIndex, 0)
                val routeSymbol =
                    SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.fromRgba(0, 0, 255), 2.0f)
                val routeGraphic = Graphic(
                    geometry = route?.routeGeometry, symbol = routeSymbol
                )

                // Add the route graphic to the incident graphic overlay
                incidentGraphicsOverlay.graphics.add(routeGraphic)
            }
        } catch (e: Exception) {

            showMessage(
                if (e.message?.contains("Unable to complete operation") == true) "Incident not within the San Diego Area!" else "Error getting closest facility result: " + e.message
            )
        }
    }

    private fun showMessage(
        message: String
    ) {
        Toast.makeText(application, message, Toast.LENGTH_LONG).show()
    }

}
