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

package com.esri.arcgismaps.sample.setbasemap.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.BasemapStyleInfo
import com.arcgismaps.mapping.BasemapStylesService
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.basemapgallery.BasemapGalleryItem
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class SetBasemapViewModel(application: Application) : AndroidViewModel(application) {
    // Map initialized with an imagery basemap to match the Swift sample
    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISImagery).apply {
            // Initial viewpoint centered near Los Angeles, CA at a scale of 1:1,000,000
            initialViewpoint = Viewpoint(
                center = Point(
                    x = -118.4,
                    y = 33.7,
                    spatialReference = SpatialReference.wgs84()
                ),
                scale = 1e6
            )
        }
    )

    // Items displayed by the Basemap Gallery
    val basemapGalleryItems = mutableStateListOf<BasemapGalleryItem>()

    // Message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            // Load the map and report any errors
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Failed to load map",
                    description = error.message.toString()
                )
            }

            // Load available basemap styles from the BasemapStyles service and create gallery items
            val service = BasemapStylesService()
            service.load().onSuccess {
                val stylesInfo = service.info?.stylesInfo
                if (!stylesInfo.isNullOrEmpty()) {
                    stylesInfo.forEach { basemapStyleInfo ->
                        basemapGalleryItems.add(BasemapGalleryItem(basemapStyleInfo))
                    }
                } else {
                    messageDialogVM.showMessageDialog(
                        title = "No basemap styles available",
                        description = "BasemapStylesService returned no styles."
                    )
                }
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Failed to load basemap styles",
                    description = error.message.toString()
                )
            }
        }
    }

    /**
     * Updates the map's basemap using the selected [BasemapStyleInfo].
     */
    fun updateBasemapFromStyleInfo(basemapStyleInfo: BasemapStyleInfo) {
        arcGISMap.setBasemap(Basemap(basemapStyleInfo.style))
    }

    /**
     * Convenience function to handle a BasemapGalleryItem click.
     */
    fun onBasemapGalleryItemClick(item: BasemapGalleryItem) {
        when (val tag = item.tag) {
            is BasemapStyleInfo -> updateBasemapFromStyleInfo(tag)
            else -> {
                messageDialogVM.showMessageDialog(
                    title = "Unsupported item type",
                    description = "The selected gallery item is not a BasemapStyleInfo."
                )
            }
        }
    }
}
