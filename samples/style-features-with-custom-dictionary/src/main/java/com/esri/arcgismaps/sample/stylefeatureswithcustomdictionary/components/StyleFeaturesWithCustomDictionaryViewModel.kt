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

package com.esri.arcgismaps.sample.stylefeatureswithcustomdictionary.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.DictionaryRenderer
import com.arcgismaps.mapping.symbology.DictionarySymbolStyle
import com.arcgismaps.portal.Portal
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.data.ServiceFeatureTable
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.stylefeatureswithcustomdictionary.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

private const val RESTAURANTS_SERVICE_URL =
    "https://services2.arcgis.com/ZQgQTuoyBrtmoGdP/arcgis/rest/services/Redlands_Restaurants/FeatureServer/0"
private const val WEB_STYLE_ITEM_ID = "adee951477014ec68d7cf0ea0579c800"

class StyleFeaturesWithCustomDictionaryViewModel(app: Application) : AndroidViewModel(app) {

    // Feature layer showing restaurant data in Redlands, CA
    private val restaurantFeatureLayer = FeatureLayer.createWithFeatureTable(
        featureTable = ServiceFeatureTable(uri = RESTAURANTS_SERVICE_URL)
    )
    var arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = Viewpoint(
            latitude = 34.0543,
            longitude = -117.1963,
            scale = 1e4
        )
    }
    private var dictionaryRendererFromStyleFile: DictionaryRenderer? = null
    private var dictionaryRendererFromWebStyle: DictionaryRenderer? = null

    private val _selectedStyle = MutableStateFlow(CustomDictionaryStyle.StyleFile)

    val messageDialogVM = MessageDialogViewModel()
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() + File.separator + app.getString(R.string.style_features_with_custom_dictionary_app_name)
    }

    init {
        viewModelScope.launch {
            // Prepare the dictionary renderer from the style file
            dictionaryRendererFromStyleFile = createDictionaryRendererFromStyleFile()
            // Prepare the dictionary renderer from the web style
            dictionaryRendererFromWebStyle = createDictionaryRendererFromWebStyle().getOrElse {
                messageDialogVM.showMessageDialog(it)
                return@launch
            }

            // Apply the renderer for the initially selected style
            applyRendererForSelectedStyle()

            // Add the restaurant layer to the map and load the map
            arcGISMap.apply {
                operationalLayers.add(restaurantFeatureLayer)
            }.load().onFailure {
                messageDialogVM.showMessageDialog(it)
            }
        }
    }

    /**
     * Update the current dictionary style selection and apply the renderer to the feature layer.
     */
    fun updateSelectedStyle(style: CustomDictionaryStyle) {
        _selectedStyle.value = style
        applyRendererForSelectedStyle()
    }

    /**
     * Apply the dictionary renderer to the restaurants layer.
     */
    private fun applyRendererForSelectedStyle() {
        restaurantFeatureLayer.renderer = when (_selectedStyle.value) {
            CustomDictionaryStyle.StyleFile -> dictionaryRendererFromStyleFile
            CustomDictionaryStyle.WebStyle -> dictionaryRendererFromWebStyle
        }
    }

    /**
     * Create a dictionary renderer from the style file included in the app's assets.
     */
    private fun createDictionaryRendererFromStyleFile(): DictionaryRenderer {
        val restaurantsStyleFile = File(provisionPath, "Restaurant.stylx")
        check(restaurantsStyleFile.exists()) {
            "Style file not found. Expected at: ${restaurantsStyleFile.canonicalPath}"
        }
        val restaurantStyle = DictionarySymbolStyle.createFromFile(restaurantsStyleFile.path)

        return DictionaryRenderer(dictionarySymbolStyle = restaurantStyle)
    }

    /**
     * Create a dictionary renderer from the web style hosted as a Portal item.
     * Maps the feature layer's field "healthgrade" to the dictionary style's expected field "Inspection".
     */
    private suspend fun createDictionaryRendererFromWebStyle(): Result<DictionaryRenderer> {
        val portal = Portal(url = "https://www.arcgis.com")
        val portalItem = PortalItem(portal = portal, itemId = WEB_STYLE_ITEM_ID)
        val restaurantSymbolStyle = DictionarySymbolStyle(portalItem).apply {
            load().getOrElse {
                return Result.failure(it)
            }
        }

        return Result.success(
            DictionaryRenderer(
                dictionarySymbolStyle = restaurantSymbolStyle,
                symbologyFieldOverrides = mapOf("healthgrade" to "Inspection")
            )
        )
    }
}

/**
 * Enum representing the two custom dictionary styles available in this sample.
 */
enum class CustomDictionaryStyle {
    StyleFile,
    WebStyle
}
