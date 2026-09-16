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

package com.esri.arcgismaps.sample.dynamicbasemapgallery.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.BasemapStyleInfo
import com.arcgismaps.mapping.BasemapStyleLanguageInfo
import com.arcgismaps.mapping.BasemapStyleLanguageStrategy
import com.arcgismaps.mapping.BasemapStyleParameters
import com.arcgismaps.mapping.BasemapStylesService
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.Worldview
import com.arcgismaps.toolkit.basemapgallery.BasemapGalleryItem
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class DynamicBasemapGalleryViewModel(app: Application) : AndroidViewModel(app) {

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

    // The style info of the currently selected basemap gallery item
    var selectedBasemapStyleInfo by mutableStateOf<BasemapStyleInfo?>(null)
        private set

    // The language currently applied to the selected basemap, if any
    var selectedLanguage by mutableStateOf<BasemapStyleLanguageInfo?>(null)
        private set

    // The worldview currently applied to the selected basemap, if any
    var selectedWorldview by mutableStateOf<Worldview?>(null)
        private set

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }

            // Load available basemap styles from the BasemapStyles service and create gallery items
            val service = BasemapStylesService()

            service.load().onSuccess {
                val stylesInfo = service.info?.stylesInfo
                if (!stylesInfo.isNullOrEmpty()) {
                    stylesInfo.forEach { basemapStyleInfo ->
                        basemapGalleryItems.add(BasemapGalleryItem(basemapStyleInfo))
                    }
                    selectedBasemapStyleInfo = stylesInfo.firstOrNull { it.style == BasemapStyle.ArcGISImagery }
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

    fun onDoneClicked(
        item: BasemapGalleryItem?,
        languageInfo: BasemapStyleLanguageInfo?,
        worldview: Worldview?
    ) {
        // Nothing was selected, so there is nothing to update
        if (item == null) return

        when (val tag = item.tag) {
            is BasemapStyleInfo -> {
                selectedBasemapStyleInfo = tag
                selectedLanguage = languageInfo
                selectedWorldview = worldview
                updateBasemap(basemapStyleInfo = tag, languageInfo = languageInfo, worldview = worldview)
            }
            else -> {
                messageDialogVM.showMessageDialog(
                    title = "Unsupported item type",
                    description = "The selected gallery item is not a BasemapStyleInfo."
                )
            }
        }
    }


    // Update the BaseMap with selected style, language and worldview
    private fun updateBasemap(
        basemapStyleInfo: BasemapStyleInfo,
        languageInfo: BasemapStyleLanguageInfo? = null,
        worldview: Worldview? = null
    ) {
        val parameters = BasemapStyleParameters().apply {
            if (languageInfo != null) {
                languageStrategy = BasemapStyleLanguageStrategy.Specific(Locale.forLanguageTag(languageInfo.languageCode))
            }
            if (worldview != null) {
                this.worldview = worldview
            }
        }
        arcGISMap.setBasemap(
            basemap = Basemap(basemapStyle = basemapStyleInfo.style, basemapStyleParameters = parameters)
        )
    }
}
