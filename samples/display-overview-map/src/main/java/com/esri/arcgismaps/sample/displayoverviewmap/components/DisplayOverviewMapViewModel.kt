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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    private val initialViewpoint = Viewpoint(latitude = 50.431999, longitude = -104.607293, scale = 10e3)

    // the current viewpoint of the map
    var viewpoint by mutableStateOf(initialViewpoint)
    // the current visible area of the map
    var visibleArea: Polygon? by mutableStateOf(null)

    // set up feature layer
    private val touristAttractionsUrl =
        "https://services6.arcgis.com/Do88DoK2xjTUCXd1/arcgis/rest/services/OSM_NA_Tourism/FeatureServer/0"
    private val touristAttractionsTable = ServiceFeatureTable(touristAttractionsUrl)
    private val featureLayerTouristAttractions = FeatureLayer.createWithFeatureTable(touristAttractionsTable)

    // the map used by MapView
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = viewpoint
        // add tourist attractions feature layer to the map
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
