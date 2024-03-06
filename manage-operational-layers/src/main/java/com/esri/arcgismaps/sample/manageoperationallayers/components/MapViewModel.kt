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
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISMapImageLayer
import com.arcgismaps.mapping.layers.Layer
import com.esri.arcgismaps.sample.manageoperationallayers.R

class MapViewModel(
    application: Application
) : AndroidViewModel(application) {

    // create an ArcGISMap
    val arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = Viewpoint(39.8, -98.6, 5e7)
    }

    // a list of the active map image layer names
    var activateLayerNames = mutableStateListOf<String>()
        private set

    // a list of the inactive map image layer names
    var inactiveLayers = mutableStateListOf<Layer>()
        private set

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
        activateLayerNames.addAll(
            listOf(
                imageLayerElevation.name,
                imageLayerCensus.name,
                imageLayerDamage.name
            )
        )

        // add the layers to the map's operational layers
        arcGISMap.apply {
            operationalLayers.addAll(
                listOf(
                    imageLayerElevation,
                    imageLayerCensus,
                    imageLayerDamage
                )
            )
        }
    }

    /**
     * Swap the active layer with the layer on top.
     */
    fun moveLayerUp(layerName: String) {
        // get a copy of the operational layers
        val operationalLayers = arcGISMap.operationalLayers.toMutableList()
        // if move up on the first item is selected, then return
        if (operationalLayers.first().name == layerName) {
            return
        }
        // get the index of the tapped layer
        val layerIndex = operationalLayers.indexOf(operationalLayers.find { it.name == layerName })
        // swap the selected layer with the layer on top
        operationalLayers.swap(layerIndex, layerIndex - 1)
        // update the layer names list
        activateLayerNames.apply {
            clear()
            addAll(operationalLayers.map { layer -> layer.name })
        }
        // update the operational layers
        arcGISMap.operationalLayers.apply {
            clear()
            addAll(operationalLayers)
        }
    }

    /**
     * Swap the active layer with the layer on bottom.
     */
    fun moveLayerDown(layerName: String) {
        // get a copy of the operational layers
        val operationalLayers = arcGISMap.operationalLayers.toMutableList()
        // if move down on the last item is selected, then return
        if (operationalLayers.last().name == layerName) {
            return
        }
        // get the index of the tapped layer
        val layerIndex = operationalLayers.indexOf(operationalLayers.find { it.name == layerName })
        // swap the selected layer with the layer on bottom
        operationalLayers.swap(layerIndex, layerIndex + 1)
        // update the layer names list
        activateLayerNames.apply {
            clear()
            addAll(operationalLayers.map { layer -> layer.name })
        }
        // update the operational layers
        arcGISMap.operationalLayers.apply {
            clear()
            addAll(operationalLayers)
        }
    }

    /**
     * Removes [layerName] from map and adds it to the list of [inactiveLayers].
     */
    fun removeLayerFromMap(layerName: String) {
        arcGISMap.operationalLayers.apply {
            val layerIndex = indexOf(find { it.name == layerName })
            inactiveLayers.add(get(layerIndex))
            removeAt(layerIndex)
            activateLayerNames.removeAt(layerIndex)
        }
    }

    /**
     * Adds the [layerName] from the list of [inactiveLayers] to the map's operational layers.
     */
    fun addLayerToMap(layerName: String) {
        inactiveLayers.apply {
            val layerIndex = indexOf(find { it.name == layerName })
            arcGISMap.operationalLayers.add(get(layerIndex))
            activateLayerNames.add(get(layerIndex).name)
            removeAt(layerIndex)
        }
    }
}

/**
 * Extension function to swap two values of a mutable list.
 */
private fun <T> MutableList<T>.swap(index1: Int, index2: Int) {
    val tmp = this[index1]
    this[index1] = this[index2]
    this[index2] = tmp
}
