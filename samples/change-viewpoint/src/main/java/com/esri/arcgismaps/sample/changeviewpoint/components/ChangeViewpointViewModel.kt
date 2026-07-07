/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.changeviewpoint.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.BuildConfig
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class ChangeViewpointViewModel(app: Application) : AndroidViewModel(app) {

    val viewpointScale = 5000.0

    private val startPoint = Point(
        x = -14093.0,
        y = 6711377.0,
        spatialReference = SpatialReference.webMercator()
    )
    val arcGISMap by mutableStateOf(
        value = ArcGISMap(BasemapStyle.ArcGISImagery).apply {
            initialViewpoint = Viewpoint(center = startPoint, scale = viewpointScale)
        }
    )

    val mapViewProxy = MapViewProxy()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    //function for when "Geometry"button is clicked

    fun onGeometryClicked() {
        val points = PolylineBuilder(SpatialReference.webMercator()).apply {
            addPoint(Point(x = -13823.0, y = 6710390.0))
            addPoint(Point(x = -13823.0, y = 6710150.0))
            addPoint(Point(x = -14680.0, y = 6710390.0))
            addPoint(Point(x = -14680.0, y = 6710150.0))
        }
        val geometry = points.toGeometry()
        viewModelScope.launch {
            mapViewProxy.setViewpointGeometry(boundingGeometry = geometry)
        }
    }

    //function for when "Center" button is clicked
    fun onCenterClicked() {
        val point = Point(
            x = -12153.0,
            y = 6710527.0,
            spatialReference = SpatialReference.webMercator()
        )
        viewModelScope.launch {
            mapViewProxy.setViewpointCenter(point, viewpointScale)
        }
    }

    //function for when "Animate" button is clicked
    fun onAnimateClicked() {
        val point = Point(
            x = -14093.0,
            y = 6711377.0,
            spatialReference = SpatialReference.webMercator()
        )
        val viewpoint = Viewpoint(center = point, scale = viewpointScale)

        viewModelScope.launch {
            mapViewProxy.setViewpointAnimated(viewpoint = viewpoint, duration = 7.seconds)
        }
    }
}
