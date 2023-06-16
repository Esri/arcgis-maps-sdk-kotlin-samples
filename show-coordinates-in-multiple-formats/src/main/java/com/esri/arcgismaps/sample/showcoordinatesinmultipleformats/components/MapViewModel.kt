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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.showcoordinatesinmultipleformats.R

class MapViewModel(application: Application) : AndroidViewModel(application) {
    // set the MapView state
    val mapViewState by mutableStateOf(MapViewState())

    var decimalDegrees by mutableStateOf("")
        private set
    var degreesMinutesSeconds by mutableStateOf("")
        private set

    var utm by mutableStateOf("")
        private set

    var usng by mutableStateOf("")
        private set

    // set up a graphic to indicate where the coordinates relate to, with an initial location
    private val initialPoint = Point(0.0, 0.0, SpatialReference.wgs84())

    private val coordinateLocation = Graphic(
        geometry = initialPoint,
        symbol = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Cross,
            color = Color.fromRgba(255, 255, 0, 255),
            size = 20f
        )
    )

    // create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    init {
        // create a map that has the WGS 84 coordinate system and set this into the map
        val basemapLayer = ArcGISTiledLayer(application.getString(R.string.basemap_url))
        val map = ArcGISMap(Basemap(basemapLayer))
        mapViewState.arcGISMap = map
        mapViewState.graphicsOverlay.graphics.add(coordinateLocation)

        // update the coordinate notations using the initial point
        toCoordinateNotationFromPoint(initialPoint)
    }

    /**
     * Updates the tapped graphic and coordinate notations using the [tappedPoint]
     */
    fun onMapTapped(tappedPoint: Point?) {
        if (tappedPoint != null) {
            // update the tapped location graphic
            coordinateLocation.geometry = tappedPoint
            mapViewState.graphicsOverlay.graphics.apply {
                clear()
                add(coordinateLocation)
            }
            // update the coordinate notations using the tapped point
            toCoordinateNotationFromPoint(tappedPoint)
        }
    }

    /**
     * Uses CoordinateFormatter to update the UI with coordinate notation strings based on the
     * given [newLocation] point to convert to coordinate notations
     */
    private fun toCoordinateNotationFromPoint(newLocation: Point) {
        coordinateLocation.geometry = newLocation
        // use CoordinateFormatter to convert to Latitude Longitude, formatted as Decimal Degrees
        decimalDegrees = CoordinateFormatter.toLatitudeLongitudeOrNull(
            point = newLocation,
            format = LatitudeLongitudeFormat.DecimalDegrees,
            decimalPlaces = 4
        ) ?: return messageDialogVM.showErrorDialog("Failed to convert from point DD coordinate")

        // use CoordinateFormatter to convert to Latitude Longitude, formatted as Degrees, Minutes, Seconds
        degreesMinutesSeconds = CoordinateFormatter.toLatitudeLongitudeOrNull(
            point = newLocation,
            format = LatitudeLongitudeFormat.DegreesMinutesSeconds,
            decimalPlaces = 4
        ) ?: return messageDialogVM.showErrorDialog("Failed to convert from point DMS coordinate")

        // use CoordinateFormatter to convert to Universal Transverse Mercator, using latitudinal bands indicator
        utm = CoordinateFormatter.toUtmOrNull(
            point = newLocation,
            utmConversionMode = UtmConversionMode.LatitudeBandIndicators,
            addSpaces = true
        ) ?: return messageDialogVM.showErrorDialog("Failed to convert from point UTM coordinate")

        // use CoordinateFormatter to convert to United States National Grid (USNG)
        usng = CoordinateFormatter.toUsngOrNull(
            point = newLocation,
            precision = 4,
            addSpaces = true,
        ) ?: return messageDialogVM.showErrorDialog("Failed to convert from point USNG coordinate")
    }

    /**
     * Uses CoordinateFormatter to update the graphic in the map from the given [coordinateNotation]
     * string entered by the user. Also calls corresponding method to update all the remaining
     * [coordinateNotation] strings using the notation [notationType].
     */
    fun fromCoordinateNotationToPoint(notationType: NotationType, coordinateNotation: String) {
        // ignore empty input coordinate notation strings, do not update UI
        if (coordinateNotation.isEmpty()) return
        val convertedPoint: Point = when (notationType) {
            NotationType.DMS, NotationType.DD -> {
                // use CoordinateFormatter to parse Latitude Longitude - different numeric notations (Decimal Degrees;
                // Degrees, Minutes, Seconds; Degrees, Decimal Minutes) can all be passed to this same method
                CoordinateFormatter.fromLatitudeLongitudeOrNull(
                    coordinates = coordinateNotation,
                    spatialReference = null
                ) ?: return messageDialogVM.showErrorDialog("Failed to convert DMS/DD coordinate to point")
            }

            NotationType.UTM -> {
                // use CoordinateFormatter to parse UTM coordinates
                CoordinateFormatter.fromUtmOrNull(
                    coordinates = coordinateNotation,
                    utmConversionMode = UtmConversionMode.LatitudeBandIndicators,
                    spatialReference = null
                ) ?: return messageDialogVM.showErrorDialog("Failed to convert UTM coordinate to point")
            }

            NotationType.USNG -> {
                // use CoordinateFormatter to parse US National Grid coordinates
                CoordinateFormatter.fromUsngOrNull(
                    coordinates = coordinateNotation,
                    spatialReference = null
                ) ?: return messageDialogVM.showErrorDialog("Failed to convert USNG coordinate to point")
            }
        }

        // update the location shown in the map
        toCoordinateNotationFromPoint(convertedPoint)
    }

    /**
     * Coordinate notations supported by this sample
     */
    enum class NotationType {
        DMS, DD, UTM, USNG
    }

    /**
     * Set's [decimalDegrees] entered in the text field to the [inputString]
     */
    fun setDecimalDegreesCoordinate(inputString: String) {
        decimalDegrees = inputString
    }

    /**
     * Set's [degreesMinutesSeconds] entered in the text field to the [inputString]
     */
    fun degreesMinutesSecondsCoordinate(inputString: String) {
        degreesMinutesSeconds = inputString
    }

    /**
     * Set's [utm] entered in the text field to the [inputString]
     */
    fun setUTMCoordinate(inputString: String) {
        utm = inputString

    }

    /**
     * Set's [usng] entered in the text field to the [inputString]
     */
    fun setUSNGDegreesCoordinate(inputString: String) {
        usng = inputString
    }
}

/**
 * Data class that represents the MapView state
 */
data class MapViewState(
    var arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight),
    var graphicsOverlay: GraphicsOverlay = GraphicsOverlay()
)
