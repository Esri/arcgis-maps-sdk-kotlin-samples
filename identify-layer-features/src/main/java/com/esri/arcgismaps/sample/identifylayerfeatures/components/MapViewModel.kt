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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISMapImageLayer
import com.arcgismaps.mapping.layers.FeatureLayer.Companion.createWithFeatureTable
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geocompose.MapViewProxy
import com.esri.arcgismaps.sample.identifylayerfeatures.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MapViewModel(
    application: Application,
    private val sampleCoroutineScope: CoroutineScope
) : AndroidViewModel(application) {

    // create a map using the topographic basemap style
    val map: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic)

    // create a mapViewProxy that will be used to identify features in the MapView
    // should also be passed to the composable MapView this mapViewProxy is associated with
    val mapViewProxy = MapViewProxy()

    // create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    // string text to display the identify layer results
    val bottomTextBanner = mutableStateOf("Tap on the map to identify feature layers")

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

        // add the world cities layer with and the damaged properties feature layer
        map.apply {
            // set initial Viewpoint to North America
            initialViewpoint = Viewpoint(39.8, -98.6, 5e7)
            operationalLayers.addAll(listOf(mapImageLayer, featureLayer))
        }

    }

    /**
     * Identify the feature layer results and display the resulting information
     */
    private fun handleIdentifyResult(result: Result<List<IdentifyLayerResult>>) {
        sampleCoroutineScope.launch {
            result.onSuccess { identifyResultList ->
                val message = StringBuilder()
                var totalCount = 0
                identifyResultList.forEach { identifyLayerResult ->
                    val geoElementsCount = geoElementsCountFromResult(identifyLayerResult)
                    val layerName = identifyLayerResult.layerContent.name
                    message.append(layerName).append(": ").append(geoElementsCount)

                    // add new line character if not the final element in array
                    if (identifyLayerResult != identifyResultList[identifyResultList.size - 1]) {
                        message.append("\n")
                    }
                    totalCount += geoElementsCount
                }
                // if any elements were found show the results, else notify user that no elements were found
                if (totalCount > 0) {
                    bottomTextBanner.value = "Number of elements found:\n${message}"
                } else {
                   bottomTextBanner.value = "Number of elements found: N/A"
                    messageDialogVM.showMessageDialog(
                        title = "No element found",
                        description = "Tap an area on the map with visible features"
                    )
                }
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Error identifying results: ${error.message.toString()}",
                    description = error.cause.toString()
                )
            }
        }
    }

    /**
     * Gets a count of the GeoElements in the passed result layer.
     * This method recursively calls itself to descend into sublayers and count their results.
     * @param result from a single layer.
     * @return the total count of GeoElements.
     */
    private fun geoElementsCountFromResult(result: IdentifyLayerResult): Int {
        var subLayerGeoElementCount = 0
        for (sublayerResult in result.sublayerResults) {
            // recursively call this function to accumulate elements from all sublayers
            subLayerGeoElementCount += geoElementsCountFromResult(sublayerResult)
        }
        return subLayerGeoElementCount + result.geoElements.size
    }

    /**
     * Identifies the tapped screen coordinate in the provided [singleTapConfirmedEvent]
     */
    fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        sampleCoroutineScope.launch {
            // identify the layers on the tapped coordinate
            val identifyResult = mapViewProxy.identifyLayers(
                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                tolerance = 12.dp,
                maximumResults = 10
            )
            // use the layer result to display feature information
            handleIdentifyResult(identifyResult)
        }
    }
}
