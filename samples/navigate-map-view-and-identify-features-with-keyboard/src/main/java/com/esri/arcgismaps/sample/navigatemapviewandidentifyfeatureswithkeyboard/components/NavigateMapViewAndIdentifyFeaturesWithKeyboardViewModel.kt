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

package com.esri.arcgismaps.sample.navigatemapviewandidentifyfeatureswithkeyboard.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

const val RESTAURANTS_SERVICE_URL = "https://services2.arcgis.com/ZQgQTuoyBrtmoGdP/arcgis/rest/services/redlands_food/FeatureServer/0"
class NavigateMapViewAndIdentifyFeaturesWithKeyboardViewModel(app: Application) : AndroidViewModel(app) {

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISLightGray).apply {
        initialViewpoint = Viewpoint(-117.1825, 34.0556, 2500.0)
        operationalLayers.add(
            FeatureLayer.createWithFeatureTable(
                featureTable = ServiceFeatureTable(uri = RESTAURANTS_SERVICE_URL)
            )
        )
    }

    val mapViewProxy = MapViewProxy()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }
}
