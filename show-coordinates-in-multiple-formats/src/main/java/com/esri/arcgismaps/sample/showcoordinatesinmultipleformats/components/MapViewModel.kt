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

package com.esri.arcgismaps.sample.showcoordinatesinmultipleformats.components

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.Color
import com.arcgismaps.geometry.CoordinateFormatter
import com.arcgismaps.geometry.LatitudeLongitudeFormat
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.geometry.UtmConversionMode
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.ArcGISTiledLayer
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.esri.arcgismaps.sample.showcoordinatesinmultipleformats.R
import kotlinx.coroutines.flow.MutableStateFlow

class MapViewModel(application: Application) : AndroidViewModel(application) {
    // set the MapView mutable stateflow
    val mapViewState = MutableStateFlow(MapViewState())

    var decimalDegrees = mutableStateOf("")
    var degreesMinutesSeconds = mutableStateOf("")
    var utm = mutableStateOf("")
    var usng = mutableStateOf("")

    // set up a graphic to indicate where the coordinates relate to, with an initial location
    private val initialPoint by lazy {
        Point(0.0, 0.0, SpatialReference.wgs84())
    }
    private val coordinateLocation by lazy {
        Graphic(
            geometry = initialPoint,
            symbol = SimpleMarkerSymbol(
                style = SimpleMarkerSymbolStyle.Cross,
                color = Color.fromRgba(255, 255, 0, 255),
                size = 20f
            )
        )
    }

    init {
        // create a map that has the WGS 84 coordinate system and set this into the map
        val basemapLayer = ArcGISTiledLayer(application.getString(R.string.basemap_url))
        val map = ArcGISMap(Basemap(basemapLayer))
        mapViewState.value.arcGISMap = map
        mapViewState.value.graphicsOverlay.graphics.add(coordinateLocation)
        toCoordinateNotationFromPoint(initialPoint)
    }

    fun singleTapped(mapPoint: Point?) {
        if (mapPoint != null) {
            coordinateLocation.geometry = mapPoint
            mapViewState.value.graphicsOverlay.graphics.apply {
                clear()
                add(coordinateLocation)
            }

            toCoordinateNotationFromPoint(mapPoint)
        }
    }

    /**
     * Uses CoordinateFormatter to update the UI with coordinate notation strings based on the
     * given [newLocation] point to convert to coordinate notations
     */
    private fun toCoordinateNotationFromPoint(newLocation: Point) {
        coordinateLocation.geometry = newLocation
        // use CoordinateFormatter to convert to Latitude Longitude, formatted as Decimal Degrees
        decimalDegrees.value = CoordinateFormatter.toLatitudeLongitudeOrNull(
            point = newLocation,
            format = LatitudeLongitudeFormat.DecimalDegrees,
            decimalPlaces = 4
        ).toString()

        // use CoordinateFormatter to convert to Latitude Longitude, formatted as Degrees, Minutes, Seconds
        degreesMinutesSeconds.value = CoordinateFormatter.toLatitudeLongitudeOrNull(
            point = newLocation,
            format = LatitudeLongitudeFormat.DegreesMinutesSeconds,
            decimalPlaces = 4
        ).toString()

        // use CoordinateFormatter to convert to Universal Transverse Mercator, using latitudinal bands indicator
        utm.value = CoordinateFormatter.toUtmOrNull(
            point = newLocation,
            utmConversionMode = UtmConversionMode.LatitudeBandIndicators,
            addSpaces = true
        ).toString()

        // use CoordinateFormatter to convert to United States National Grid (USNG)
        usng.value = CoordinateFormatter.toUsngOrNull(
            point = newLocation,
            precision = 4,
            addSpaces = true,
        ).toString()
    }

    /**
     * Uses CoordinateFormatter to update the graphic in the map from the given coordinate notation string entered by the
     * user. Also calls corresponding method to update all the remaining coordinate notation strings.
     * [type] the given coordinate notation type
     * [coordinateNotation] a string containing the coordinate notation to convert to a point
     */
    fun fromCoordinateNotationToPoint(type: NotationType, coordinateNotation: String) {
        // ignore empty input coordinate notation strings, do not update UI
        if (coordinateNotation.isEmpty()) return
        var convertedPoint: Point? = null

        when (type) {
            NotationType.DMS, NotationType.DD -> {
                // use CoordinateFormatter to parse Latitude Longitude - different numeric notations (Decimal Degrees;
                // Degrees, Minutes, Seconds; Degrees, Decimal Minutes) can all be passed to this same method
                convertedPoint = CoordinateFormatter.fromLatitudeLongitudeOrNull(
                    coordinates = coordinateNotation,
                    spatialReference = null
                )
            }

            NotationType.UTM -> {
                // use CoordinateFormatter to parse UTM coordinates
                convertedPoint = CoordinateFormatter.fromUtmOrNull(
                    coordinates = coordinateNotation,
                    utmConversionMode = UtmConversionMode.LatitudeBandIndicators,
                    spatialReference = null,
                )
            }

            NotationType.USNG -> {
                // use CoordinateFormatter to parse US National Grid coordinates
                convertedPoint = CoordinateFormatter.fromUsngOrNull(
                    coordinates = coordinateNotation,
                    spatialReference = null
                )
            }
        }
        if (convertedPoint != null) {
            // update the location shown in the map
            toCoordinateNotationFromPoint(convertedPoint)
        }
    }

    /**
     * Coordinate notations supported by this sample
     */
    enum class NotationType {
        DMS, DD, UTM, USNG
    }
}


/**
 * Data class that represents the MapView state
 */
data class MapViewState(
    var arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight),
    var graphicsOverlay: GraphicsOverlay = GraphicsOverlay()
)
