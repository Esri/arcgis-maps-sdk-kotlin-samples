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

package com.esri.arcgismaps.sample.identifylayerfeatures.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISMapImageLayer
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.FeatureLayer.Companion.createWithFeatureTable
import com.esri.arcgismaps.sample.identifylayerfeatures.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(private val application: Application,
                   private val sampleCoroutineScope: CoroutineScope
) : AndroidViewModel(application) {
    // set the MapView mutable stateflow
    val mapViewState = MutableStateFlow(MapViewState())
    // create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    init {
        // create a feature layer of damaged property data
        val featureTable = ServiceFeatureTable(application.getString(R.string.damage_assessment))
        val featureLayer = createWithFeatureTable(featureTable)

        // create a layer with world cities data
        val mapImageLayer = ArcGISMapImageLayer(application.getString(R.string.world_cities))
        sampleCoroutineScope.launch {
            mapImageLayer.load().onSuccess {
                mapImageLayer.apply {
                    subLayerContents.value[1].isVisible = false
                    subLayerContents.value[2].isVisible = false
                }
            }.onFailure { error ->
                // show the message dialog and pass the error message to be displayed in the dialog
                messageDialogVM.showMessageDialog(error.message.toString(), error.cause.toString())
            }
        }


        // create a topographic map
        val map = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
            // add world cities layer
            operationalLayers.add(mapImageLayer)

            // add damaged property data
            operationalLayers.add(featureLayer)
        }

        // assign the map to the map view
        mapViewState.value.arcGISMap = map
    }

    /**
     * Switch between two basemaps
     */
    fun changeBasemap() {
        val newArcGISMap: ArcGISMap =
            if (mapViewState.value.arcGISMap.basemap.value?.name.equals("ArcGIS:NavigationNight")) {
                ArcGISMap(BasemapStyle.ArcGISStreets)
            } else {
                ArcGISMap(BasemapStyle.ArcGISNavigationNight)
            }
        mapViewState.update { it.copy(arcGISMap = newArcGISMap) }
    }
}


/**
 * Data class that represents the MapView state
 */
data class MapViewState( // This would change based on each sample implementation
    var arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight),
    var viewpoint: Viewpoint = Viewpoint(Point(-10977012.785807, 4514257.550369,
        SpatialReference(wkid = 3857)), 68015210.0)
)
