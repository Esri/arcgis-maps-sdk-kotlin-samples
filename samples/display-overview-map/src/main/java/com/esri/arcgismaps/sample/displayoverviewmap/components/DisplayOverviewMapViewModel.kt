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

package com.esri.arcgismaps.sample.displayoverviewmap.components

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class DisplayOverviewMapViewModel(app: Application) : AndroidViewModel(app) {

    private val initialViewpoint = Viewpoint(latitude = -41.44, longitude = 173.52, scale = 10e6)

    // the current viewpoint of the map
    val viewpoint = mutableStateOf(initialViewpoint)
    // the current visible area of the map
    val visibleArea: MutableState<Polygon?> = mutableStateOf(null)

    // set up feature layer
    private val touristAttractionsUrl =
        "https://services1.arcgis.com/ligOmcZkuYGDjlNm/arcgis/rest/services/Tourism_Attractions/FeatureServer/2"
    private val touristAttractionTable = ServiceFeatureTable(touristAttractionsUrl)
    private val featureLayerTouristAttractions = FeatureLayer.createWithFeatureTable(touristAttractionTable)

    // the map used by MapView
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = viewpoint.value
        // add tourist attraction feature layer to the map
        operationalLayers.add(featureLayerTouristAttractions)
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }
}
