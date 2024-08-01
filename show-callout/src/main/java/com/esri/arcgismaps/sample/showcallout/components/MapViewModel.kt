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

package com.esri.arcgismaps.sample.showcallout.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.showcallout.R

class MapViewModel(private val application: Application) : AndroidViewModel(application) {

    // Create a mapViewProxy that will be used to identify features in the MapView.
    // This should also be passed to the composable MapView this mapViewProxy is associated with.
    val mapViewProxy = MapViewProxy()

    // Create an ArcGISMap with viewpoint set to Los Angeles, CA.
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight).apply {
        initialViewpoint = Viewpoint(34.056295, -117.195800, 1000000.0)
    }

    // Keep track of the state of a lat/lon point.
    var latLonPoint: Point? by mutableStateOf(null)
    // Keep track of the state of the callout content String.
    var calloutContent: String by mutableStateOf("")

    /**
     * Show a callout at the map point of the single tap event.
     */
    fun showCalloutAt(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        // Get map point from the Single tap event.
        singleTapConfirmedEvent.mapPoint?.let { point ->
            // Convert the point to WGS84 to get a latitude and longitude coordinate.
            latLonPoint = GeometryEngine.projectOrNull(
                point,
                SpatialReference.wgs84()
            ) as Point
            // Set the callout text to display point coordinates.
            calloutContent = application.getString(R.string.callout_text, latLonPoint?.y, latLonPoint?.x)
        }
    }
}
