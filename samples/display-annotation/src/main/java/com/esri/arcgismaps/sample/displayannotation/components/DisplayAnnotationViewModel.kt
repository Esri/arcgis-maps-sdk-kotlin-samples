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

package com.esri.arcgismaps.sample.displayannotation.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.AnnotationLayer
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.data.ServiceFeatureTable
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class DisplayAnnotationViewModel(app: Application) : AndroidViewModel(app) {
    // A URL to a feature layer for the rivers in East Lothian.
    private val eastLothianRiversUrl =
        "https://services1.arcgis.com/6677msI40mnLuuLr/arcgis/rest/services/East_Lothian_Rivers/FeatureServer/0"
    // A URL to an annotation layer for the rivers in East Lothian.
    private val riversAnnotationUrl =
        "https://sampleserver6.arcgisonline.com/arcgis/rest/services/RiversAnnotation/FeatureServer/0"

    // ArcGISMap with light gray canvas basemap and initial viewpoint (East Lothian, Scotland)
    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISLightGray).apply {
            initialViewpoint = Viewpoint(
                latitude = 55.882436,
                longitude = -2.725610,
                scale = 72223.819286
            )
        }
    )

    // Message dialog for error handling
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            // Create and load the feature layer
            val featureTable = ServiceFeatureTable(eastLothianRiversUrl)
            featureTable.load().onFailure {
                messageDialogVM.showMessageDialog(it)
                return@launch
            }
            val featureLayer = FeatureLayer.createWithFeatureTable(featureTable)

            // Create and load the annotation layer
            val annotationLayer = AnnotationLayer(riversAnnotationUrl)
            annotationLayer.load().onFailure {
                messageDialogVM.showMessageDialog(it)
                return@launch
            }

            // Add both layers to the map's operational layers
            arcGISMap.operationalLayers.addAll(listOf(featureLayer, annotationLayer))
        }
    }
}
