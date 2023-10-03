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
import android.widget.TextView
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
import com.esri.arcgismaps.sample.showcallout.R
import kotlinx.coroutines.flow.MutableStateFlow

class MapViewModel(private val application: Application) : AndroidViewModel(application) {
    // set the MapView mutable stateflow
    val mapViewState = MutableStateFlow(MapViewState(application))
    // View to show callout
    var calloutContent: TextView by mutableStateOf(TextView(application))
    // initialize lat long point
    var latLonPoint: Point? by mutableStateOf(null)

    fun onMapTapped(mapPoint: Point?) {
        // get map point from the Single tap event
        mapPoint?.let { point ->
            // convert the point to WGS84 for obtaining lat/lon format
            latLonPoint = GeometryEngine.projectOrNull(
                point,
                SpatialReference.wgs84()
            ) as Point
            // set the callout text to display point coordinates
            calloutContent.text = application.getString(
                R.string.callout_text,
                latLonPoint?.y,
                latLonPoint?.x
            )
        }
    }
}

/**
 * Data class that represents the MapView state
 */
data class MapViewState(val application: Application) {
    var arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight)
    var viewpoint: Viewpoint = Viewpoint(34.056295, -117.195800, 1000000.0)
}
