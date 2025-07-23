/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.setfeaturerequestmode.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.FeatureRequestMode
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections

class SetFeatureRequestModeViewModel(application: Application) : AndroidViewModel(application) {

    // Feature table of street trees in Portland
    private val featureTable =
        ServiceFeatureTable("https://services2.arcgis.com/ZQgQTuoyBrtmoGdP/arcgis/rest/services/Trees_of_Portland/FeatureServer/0")

    val arcGISMap =
        ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
            // Set a viewpoint to downtown Portland, OR
            initialViewpoint = Viewpoint(latitude = 45.5266, longitude = -122.6219, scale = 6000.0)
            // Create a feature layer from the feature table and add it to the map
            operationalLayers.add(FeatureLayer.createWithFeatureTable(featureTable))
        }

    private var viewpoint: Viewpoint? = null

    var currentFeatureRequestMode by mutableStateOf<FeatureRequestMode>(FeatureRequestMode.OnInteractionCache)
        private set

    var isLoading by mutableStateOf(false)
        private set

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map",
                    error.message.toString()
                )
            }
        }
    }

    /**
     * Called when the viewpoint of the map changes.
     */
    fun onViewpointChange(viewpoint: Viewpoint) {
        this.viewpoint = viewpoint
    }

    /**
     * Called when the feature request mode is changed.
     */
    fun onCurrentFeatureRequestModeChanged(featureRequestMode: FeatureRequestMode) {
        currentFeatureRequestMode = featureRequestMode
        featureTable.featureRequestMode = featureRequestMode
    }

    /**
     * Demonstrates how to manually fetch features from the service feature table using the current viewpoint.
     */
    fun fetchCacheManually() {

        // Show the progress indicator
        isLoading = true

        // Create query to select all tree features
        val queryParams = QueryParameters().apply {
            // Query for all tree conditions except "dead" with coded value '4' within the visible extent
            whereClause = "Condition < '4'"
            geometry = viewpoint?.targetGeometry as Envelope
        }

        // Setting this to * means all features
        val outfields: List<String> = Collections.singletonList("*")

        viewModelScope.launch(Dispatchers.IO) {
            // Get queried features from service feature table and clear previous cache
            featureTable.populateFromService(
                parameters = queryParams,
                clearCache = true,
                outFields = outfields
            ).onSuccess {
                // hide the loading ProgressBar
                isLoading = false
            }
        }
    }
}
