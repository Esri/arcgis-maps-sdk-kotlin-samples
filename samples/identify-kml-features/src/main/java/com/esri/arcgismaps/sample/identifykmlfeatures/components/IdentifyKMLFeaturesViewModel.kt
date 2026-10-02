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

package com.esri.arcgismaps.sample.identifykmlfeatures.components

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.kml.KmlDataset
import com.arcgismaps.mapping.kml.KmlPlacemark
import com.arcgismaps.mapping.layers.KmlLayer
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IdentifyKMLFeaturesViewModel(app: Application) : AndroidViewModel(app) {
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISDarkGray)

    val mapViewProxy = MapViewProxy()


    val kmlDataset = KmlDataset(uri = "https://www.arcgis.com/sharing/rest/content/items/f5e0e5cd088846a5b97b7ed66a8bad5c/data")

    val forecastLayer = KmlLayer(kmlDataset = kmlDataset)

    private val _isLoading = MutableStateFlow(false)

    private val _pointAndHtml = MutableStateFlow<Pair<Point, String>?>(null)
    val pointAndHtml: StateFlow<Pair<Point, String>?> = _pointAndHtml.asStateFlow()

    private val _offset = MutableStateFlow(Offset.Zero)
    val offset: StateFlow<Offset> = _offset

    private val _tapLocationGraphicsOverlay = MutableStateFlow(GraphicsOverlay())
    val tapLocationGraphicsOverlay: StateFlow<GraphicsOverlay> =
        _tapLocationGraphicsOverlay.asStateFlow()



    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            _isLoading.value = true
            arcGISMap.operationalLayers.apply {
                clear()
                add(forecastLayer)
            }
            forecastLayer.load().onSuccess {
                arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
                // If the layer has a full extent after loading, use MapViewProxy to set the viewpoint.
                forecastLayer.fullExtent?.let { extent ->
                    mapViewProxy.setViewpointGeometry(boundingGeometry = extent, paddingInDips = 25.0)
                }
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Failed to load KML layer",
                    description = error.message.toString()
                )
            }
            _isLoading.value = false
        }
    }




    fun onSingleTapConfirmed(event: SingleTapConfirmedEvent) {
        viewModelScope.launch {
            val identifyResult = mapViewProxy.identify(layer = forecastLayer, screenCoordinate = event.screenCoordinate, tolerance = 10.0.dp)
            val feature = identifyResult.getOrNull()
            if (feature != null && feature.geoElements.isNotEmpty()) {
                val firstKMLPlacemark : KmlPlacemark = feature.geoElements.first() as KmlPlacemark
                // Google Earth only displays the placemarks with description or extended data.
                // To match its behavior, add a description placeholder if it is empty.
                if(firstKMLPlacemark.description.isEmpty()){
                    firstKMLPlacemark.description = "Weather Condition"
                }
                val kmlText = firstKMLPlacemark.balloonContent
                //Update the state with the identified point and HTML content to be displayed in the callout.
                _pointAndHtml.value = Pair(event.mapPoint!!, kmlText)
            } else {
                _pointAndHtml.value = null
            }
        }
    }
}
