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
import com.arcgismaps.geometry.CoordinateFormatter
import com.arcgismaps.geometry.LatitudeLongitudeFormat
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.UtmConversionMode
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel

class MapViewModel(application: Application) : AndroidViewModel(application) {

    var decimalDegrees by mutableStateOf("")
        private set
    var degreesMinutesSeconds by mutableStateOf("")
        private set

    var utm by mutableStateOf("")
        private set

    var usng by mutableStateOf("")
        private set

    // create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    /**
     * Uses CoordinateFormatter to update the UI with coordinate notation strings based on the
     * given [newLocation] point to convert to coordinate notations
     */
    fun toCoordinateNotationFromPoint(newLocation: Point) {

        // use CoordinateFormatter to convert to Latitude Longitude, formatted as Decimal Degrees
        decimalDegrees = CoordinateFormatter.toLatitudeLongitudeOrNull(
            point = newLocation,
            format = LatitudeLongitudeFormat.DecimalDegrees,
            decimalPlaces = 4
        ) ?: return messageDialogVM.showMessageDialog("Failed to convert from point DD coordinate")

        // use CoordinateFormatter to convert to Latitude Longitude, formatted as Degrees, Minutes, Seconds
        degreesMinutesSeconds = CoordinateFormatter.toLatitudeLongitudeOrNull(
            point = newLocation,
            format = LatitudeLongitudeFormat.DegreesMinutesSeconds,
            decimalPlaces = 4
        ) ?: return messageDialogVM.showMessageDialog("Failed to convert from point DMS coordinate")

        // use CoordinateFormatter to convert to Universal Transverse Mercator, using latitudinal bands indicator
        utm = CoordinateFormatter.toUtmOrNull(
            point = newLocation,
            utmConversionMode = UtmConversionMode.LatitudeBandIndicators,
            addSpaces = true
        ) ?: return messageDialogVM.showMessageDialog("Failed to convert from point UTM coordinate")

        // use CoordinateFormatter to convert to United States National Grid (USNG)
        usng = CoordinateFormatter.toUsngOrNull(
            point = newLocation,
            precision = 4,
            addSpaces = true,
        ) ?: return messageDialogVM.showMessageDialog("Failed to convert from point USNG coordinate")
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
                ) ?: return messageDialogVM.showMessageDialog("Failed to convert DMS/DD coordinate to point")
            }

            NotationType.UTM -> {
                // use CoordinateFormatter to parse UTM coordinates
                CoordinateFormatter.fromUtmOrNull(
                    coordinates = coordinateNotation,
                    utmConversionMode = UtmConversionMode.LatitudeBandIndicators,
                    spatialReference = null
                ) ?: return messageDialogVM.showMessageDialog("Failed to convert UTM coordinate to point")
            }

            NotationType.USNG -> {
                // use CoordinateFormatter to parse US National Grid coordinates
                CoordinateFormatter.fromUsngOrNull(
                    coordinates = coordinateNotation,
                    spatialReference = null
                ) ?: return messageDialogVM.showMessageDialog("Failed to convert USNG coordinate to point")
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
