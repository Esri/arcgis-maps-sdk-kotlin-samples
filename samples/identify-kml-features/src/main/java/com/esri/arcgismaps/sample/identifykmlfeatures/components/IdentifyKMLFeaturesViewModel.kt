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

    // The KML dataset is created from a URL that points to a KML file containing weather forecast data. The KML layer is then created using this dataset.
    val kmlDataset = KmlDataset(uri = "https://www.arcgis.com/sharing/rest/content/items/f5e0e5cd088846a5b97b7ed66a8bad5c/data")

    // The KML layer is created using the KML dataset. This layer will be added to the map's operational layers.
    val forecastLayer = KmlLayer(kmlDataset = kmlDataset)

    // A MutableStateFlow to track the loading state of the KML layer. This can be used to show a loading indicator in the UI while the layer is being loaded.
    private val _isLoading = MutableStateFlow(false)

    // A MutableStateFlow to track the currently selected point and its associated HTML content.
    private val _pointAndHtml = MutableStateFlow<Pair<Point, String>?>(null)
    val pointAndHtml: StateFlow<Pair<Point, String>?> = _pointAndHtml.asStateFlow()

    // A MutableStateFlow to track the offset of the callout. This can be used to adjust the position of the callout in the UI.
    private val _offset = MutableStateFlow(Offset.Zero)
    val offset: StateFlow<Offset> = _offset

    // A MutableStateFlow to track the graphics overlay used for displaying the tap location. This can be used to show a marker at the location where the user tapped on the map.
    private val _tapLocationGraphicsOverlay = MutableStateFlow(GraphicsOverlay())
    val tapLocationGraphicsOverlay: StateFlow<GraphicsOverlay> =
        _tapLocationGraphicsOverlay.asStateFlow()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            _isLoading.value = true
            arcGISMap.operationalLayers.apply {
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
            val feature = mapViewProxy.identify(layer = forecastLayer, screenCoordinate = event.screenCoordinate, tolerance = 10.0.dp).getOrNull()
            // Check if the identified feature is not null and has geoElements.
            if (feature != null && feature.geoElements.isNotEmpty()) {
                // Get the first KML placemark from the identify layer result's geoElements.
                val firstKMLPlacemark : KmlPlacemark? = feature.geoElements.filterIsInstance<KmlPlacemark>().firstOrNull()
                if(firstKMLPlacemark != null){
                    // Google Earth only displays the placemarks with description or extended data.
                    // To match its behavior, add a description placeholder if it is empty.
                    if(firstKMLPlacemark.description.isEmpty()){
                        firstKMLPlacemark.description = "Weather Condition"
                    }
                    val kmlText = firstKMLPlacemark.balloonContent
                    //Update the state with the identified point and HTML content to be displayed in the callout.
                    event.mapPoint?.let { mapPoint ->
                        _pointAndHtml.value = Pair(mapPoint, kmlText)
                    }
                }else{
                    _pointAndHtml.value = null
                }
            } else {
                _pointAndHtml.value = null
            }
        }
    }
}