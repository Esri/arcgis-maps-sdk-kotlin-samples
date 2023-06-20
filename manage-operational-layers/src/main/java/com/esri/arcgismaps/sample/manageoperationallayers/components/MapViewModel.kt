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

package com.esri.arcgismaps.sample.manageoperationallayers.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISMapImageLayer
import com.esri.arcgismaps.sample.manageoperationallayers.R

class MapViewModel(
    application: Application
) : AndroidViewModel(application) {

    // get an instance of the MapView state
    val mapViewState = MapViewState()

    // a list of the map image layer names
    var layerNames: List<String> = listOf()

    init {
        // set the three map image layers
        val imageLayerElevation = ArcGISMapImageLayer(
            url = application.getString(R.string.elevationServiceURL)
        )
        val imageLayerCensus = ArcGISMapImageLayer(
            url = application.getString(R.string.censusServiceURL)
        )
        val imageLayerDamage = ArcGISMapImageLayer(
            url = application.getString(R.string.damageServiceURL)
        )
        // get a list of the layer names
        layerNames = listOf(
            imageLayerElevation.name,
            imageLayerCensus.name,
            imageLayerDamage.name
        )

        // add the layers to the map's operational layers
        mapViewState.arcGISMap.apply {
            operationalLayers.addAll(
                listOf(
                    imageLayerElevation,
                    imageLayerCensus,
                    imageLayerDamage
                )
            )
        }
    }

    fun moveLayerUp(layerName: String) {
        val operationalLayers = mapViewState.arcGISMap.operationalLayers

        if (operationalLayers.first().name == layerName) {
            return
        }

        val layerIndex = operationalLayers.indexOf(operationalLayers.find { it.name == layerName })
        val swapLayer = operationalLayers[layerIndex]
        operationalLayers[layerIndex] = operationalLayers[layerIndex - 1]
        operationalLayers[layerIndex - 1] = swapLayer

        layerNames = operationalLayers.map { layer -> layer.name }

        mapViewState.arcGISMap.operationalLayers.clear()
        mapViewState.arcGISMap.operationalLayers.addAll(operationalLayers)
    }

    fun moveLayerDown(layerName: String) {
    }

    fun toggleLayerVisibility(layerName: String) {
        mapViewState.arcGISMap.operationalLayers.forEach { layer ->
            if (layer.name == layerName) {
                layer.isVisible = !layer.isVisible
            }
        }
    }
}


/**
 * Class that represents the MapView state
 */
class MapViewState {
    var arcGISMap: ArcGISMap by mutableStateOf(ArcGISMap(BasemapStyle.ArcGISTopographic))
    var viewpoint: Viewpoint = Viewpoint(39.8, -98.6, 5e7)

}
