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

package com.esri.arcgismaps.sample.addfeaturecollectionlayerfromportalitem.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.FeatureCollection
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureCollectionLayer
import com.arcgismaps.portal.Portal
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

/**
 * ViewModel that loads a [FeatureCollection] from a [PortalItem] and adds a [FeatureCollectionLayer]
 * to the ArcGISMap.
 */
class AddFeatureCollectionLayerFromPortalItemViewModel(app: Application) : AndroidViewModel(app) {

    // Create a Portal pointing to ArcGIS Online and the feature collection portal item
    private val portalItem = PortalItem(
        portal = Portal("https://www.arcgis.com"),
        itemId = "32798dfad17942858d5eef82ee802f0b"
    )

    // Create a FeatureCollection from the portal item and add it to a FeatureCollectionLayer
    private val featureCollectionLayer = FeatureCollectionLayer(
        featureCollection = FeatureCollection(item = portalItem)
    )

    // Map with an ocean basemap and an initial viewpoint and add the featureCollectionLayer
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISOceans).apply {
        initialViewpoint = Viewpoint(latitude = 39.8, longitude = -98.6, scale = 10e7)
        // Add the layer to the map's operational layers
        operationalLayers.add(featureCollectionLayer)
    }

    // Message dialog view model for error reporting
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the map which loads the portal item to then load feature collection layer
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }
}
