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

package com.esri.arcgismaps.sample.projectgeometry.components

import android.app.Application
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.content.Context
import android.provider.Settings.System.getString
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.projectgeometry.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class ProjectGeometryViewModel(val app: Application) : AndroidViewModel(app) {
    // create a map with a navigation night basemap style
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISStreetsNight)
    // create a MapViewProxy to interact with the MapView
    val mapViewProxy = MapViewProxy()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // setup the red pin marker image as bitmap drawable
    private val markerDrawable: BitmapDrawable by lazy {
        // load the bitmap from resources and create a drawable
        val bitmap = BitmapFactory.decodeResource(app.resources, R.drawable.pin_symbol)
        BitmapDrawable(app.resources, bitmap)
    }

    // setup the red pin marker as a Graphic
    private val markerGraphic: Graphic by lazy {
        // creates a symbol from the marker drawable
        val markerSymbol = PictureMarkerSymbol.createWithImage(markerDrawable).apply {
            // resize the symbol into a smaller size
            width = 30f
            height = 30f
            // offset in +y axis so the marker spawned
            // is right on the touch point
            offsetY = 25f
        }
        // create the graphic from the symbol
        Graphic(symbol = markerSymbol)
    }

    val graphicsOverlay = GraphicsOverlay().apply {
        graphics.add(markerGraphic)
    }

    private val _infoText = MutableStateFlow(app.resources.getString(R.string.tap_to_begin))
    val infoText: StateFlow<String> = _infoText

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map",
                    error.message.toString()
                )
            }.onSuccess {
                // set the default viewpoint to Redlands,CA
                mapViewProxy.setViewpoint(Viewpoint(34.058, -117.195, 5e4))
            }
        }
    }

    fun onMapViewTapped(event: SingleTapConfirmedEvent) {
        event.mapPoint.let { point ->
            // update the marker location to where the user tapped on the map
            markerGraphic.geometry = point
            // set mapview to recenter to the tapped location
            mapViewProxy.setViewpoint(Viewpoint(point as Geometry))
            // project the web mercator location into a WGS84
            val projectedPoint =
                GeometryEngine.projectOrNull(point, SpatialReference.wgs84())
            _infoText.value = app.resources.getString(
                R.string.projection_info_text,
                point.toDisplayFormat(),
                projectedPoint?.toDisplayFormat()
            )
        }
    }
}

/**
 * Extension function for the Point type that returns
 * a float-precision formatted string suitable for display
 */
private fun Point.toDisplayFormat() =
    "${String.format(Locale.getDefault(),"%.5f", x)}, ${String.format(Locale.getDefault(),"%.5f", y)}"
