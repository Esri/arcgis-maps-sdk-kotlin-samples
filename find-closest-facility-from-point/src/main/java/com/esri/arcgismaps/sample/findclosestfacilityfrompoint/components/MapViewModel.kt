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
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class MapViewModel(private var application: Application) : AndroidViewModel(application) {

    // List of hospital facilities in the San Diego area
    private var facilitiesList = listOf(
        Facility(Point(-1.3042129900625112E7, 3860127.9479775648, SpatialReference.webMercator())),
        Facility(Point(-1.3042193400557665E7, 3862448.873041752, SpatialReference.webMercator())),
        Facility(Point(-1.3046882875518233E7, 3862704.9896770366, SpatialReference.webMercator())),
        Facility(Point(-1.3040539754780494E7, 3862924.5938606677, SpatialReference.webMercator())),
        Facility(Point(-1.3042571225655518E7, 3858981.773018156, SpatialReference.webMercator())),
        Facility(Point(-1.3039784633928463E7, 3856692.5980474586, SpatialReference.webMercator())),
        Facility(Point(-1.3049023883956768E7, 3861993.789732541, SpatialReference.webMercator()))
    )

    // Create graphic overlay of facilities to display on the map
    private val facilityGraphicsOverlay = GraphicsOverlay()

    // Create graphic overlay of incident to display on map tapped
    private val incidentGraphicsOverlay = GraphicsOverlay()

    // Create a ViewModel to handle dialog interactions
    private val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    // Add the graphics overlays to be used by the composable MapView
    val graphicsOverlays = listOf(facilityGraphicsOverlay, incidentGraphicsOverlay)

    // Create a streets basemap layer and change the position of map to center around San Diego
    val map = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
        initialViewpoint = Viewpoint(
            latitude = 32.727,
            longitude = -117.1750,
            scale = 100000.0
        )
    }

    init {
        // Create the facility symbol
        val facilityUrl = application.getString(R.string.hospital_symbol_url)
        val facilitySymbol = PictureMarkerSymbol(facilityUrl).apply {
            height = 30F
            width = 30F
        }

        // Create a graphic for each facility and add them to the graphics overlay
        for (facility in facilitiesList) {
            val facilityGraphic = Graphic(
                geometry = facility.geometry,
                symbol = facilitySymbol
            )
            facilityGraphicsOverlay.graphics.add(facilityGraphic)
        }
    }

    /**
     * Retrieve the tapped point [event], update the incident, and initiate the process
     * to find the route between the incident and closest facility.
     */
    fun onSingleTapConfirmed(event: SingleTapConfirmedEvent) {

        // Retrieve the tapped map point from the SingleTapConfirmedEvent
        val mapPoint: Point = event.mapPoint ?: return messageDialogVM.showMessageDialog(
            title = "No map point retrieved from tap."
        )

        // Create an incident on the tapped point
        val incidentPoint = Incident(mapPoint)

        // Clear and update the incident graphic on the map
        updateIncidentGraphicPosition(incidentPoint)

        // Find the closest facility to the incident
        viewModelScope.launch {
            findRoute(incidentPoint)
        }
    }

    /**
     * Updates the [incidentGraphicsOverlay] to display a graphic on the [incidentPoint]
     */
    private fun updateIncidentGraphicPosition(incidentPoint: Incident) {

        incidentGraphicsOverlay.graphics.clear()

        // Create a cross symbol for the incident
        val incidentMarker = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Cross,
            color = Color.black,
            size = 20.0F
        )

        // Get the incident's geometry
        val incidentStopGeometry = incidentPoint.geometry

        // Add graphic to incident graphic overlays
        incidentGraphicsOverlay.graphics.add(
            Graphic(
                geometry = incidentStopGeometry,
                symbol = incidentMarker
            )
        )

    }

    /**
     * Find the route between the given [incidentPoint] and the closest available facility
     *
     */
    private suspend fun findRoute(incidentPoint: Incident) {
        val closestFacilityTask = ClosestFacilityTask(
            url = application.getString(R.string.san_diego_network_service_url)
        )

        // Create a job to find the route
        val closestFacilityParameters: ClosestFacilityParameters =
            closestFacilityTask.createDefaultParameters().getOrThrow()
        // Add the facilities and incident points
        closestFacilityParameters.apply {
            setFacilities(facilitiesList)
            setIncidents(listOf(incidentPoint))
        }

        // Solve a route using the facility task with the created parameters.
        val closestFacilityResult =
            closestFacilityTask.solveClosestFacility(closestFacilityParameters).getOrElse { error ->
                messageDialogVM.showMessageDialog(
                    title = "Error solving route: ${error.message.toString()}",
                    description = error.cause.toString()
                )
            } as ClosestFacilityResult
        val rankedFacilitiesList = closestFacilityResult.getRankedFacilityIndexes(incidentIndex = 0)

        // If the result contains a facility, solve the route from incident to facility. 
        if (rankedFacilitiesList.isNotEmpty()) {
            val closestFacilityIndex = rankedFacilitiesList[0]
            val route = closestFacilityResult.getRoute(
                facilityIndex = closestFacilityIndex,
                incidentIndex = 0
            )
            val routeSymbol = SimpleLineSymbol(
                style = SimpleLineSymbolStyle.Solid,
                color = Color.fromRgba(0, 0, 255),
                width = 2.0f
            )
            val routeGraphic = Graphic(
                geometry = route?.routeGeometry,
                symbol = routeSymbol
            )

            // Add the route graphic to the incident graphic overlay
            incidentGraphicsOverlay.graphics.add(routeGraphic)
        }

    }
}
