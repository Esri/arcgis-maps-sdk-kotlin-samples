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

package com.esri.arcgismaps.sample.createsymbolstylesfromwebstyles.components

import android.app.Application
import android.graphics.drawable.BitmapDrawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SymbolStyle
import com.arcgismaps.mapping.symbology.UniqueValue
import com.arcgismaps.mapping.symbology.UniqueValueRenderer
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateSymbolStylesFromWebStylesViewModel(application: Application) : AndroidViewModel(application) {

    val mapViewProxy = MapViewProxy()

    // Create a unique value renderer
    private val uniqueValueRenderer = UniqueValueRenderer().apply {
        // Add the name of a field from the feature layer data that symbols will be mapped to
        fieldNames.add("cat2")
    }

    // Create a feature layer from a service
    val featureLayer =
        FeatureLayer.createWithFeatureTable(ServiceFeatureTable("http://services.arcgis.com/V6ZHFr6zdgNZuVG0/arcgis/rest/services/LA_County_Points_of_Interest/FeatureServer/0"))
            .apply {
                // Set the unique value renderer on the feature layer
                renderer = uniqueValueRenderer
            }

    /**
     * Only scale symbols if map scale greater than or equal to 80,000
     */
    fun onMapScaleChanged(scale: Double) {
        featureLayer.scaleSymbols = scale >= 80000
    }

    // Create a map with the light gray basemap style
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISLightGray).apply {
        // Set a viewpoint
        initialViewpoint = Viewpoint(34.28301, -118.44186, 7000.0)
        // Set a reference scale on the map for controlling symbol size
        referenceScale = 100000.0
        // Add the feature layer to the map's operational layers
        operationalLayers.add(featureLayer)
    }

    // Create a symbol style from a web style. ArcGIS Online is used as the default portal when null is passed as the
    // portal parameter
    private val symbolStyle = SymbolStyle.createWithStyleNameAndPortal("Esri2DPointSymbolsStyle", null)

    // Create a list of the required symbol names in the web style
    private val symbolNames = listOf(
        "atm",
        "beach",
        "campground",
        "city-hall",
        "hospital",
        "library",
        "park",
        "place-of-worship",
        "police-station",
        "post-office",
        "school",
        "trail"
    )

    // Create a flow to hold the symbol names and icons
    private val _symbolNamesAndIconsFlow = MutableStateFlow<List<Pair<String, BitmapDrawable>>>(emptyList())
    val symbolNamesAndIconsFlow = _symbolNamesAndIconsFlow.asStateFlow()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the symbols into a list
        viewModelScope.launch {
            _symbolNamesAndIconsFlow.value = createUniqueValueSymbols()
        }

        viewModelScope.launch {
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map", error.message.toString()
                )
            }
        }
    }

    /**
     * Create a series of unique values and add them to the unique value renderer. Also create a list of the symbol
     * names and icons and return them as a list of pairs.
     */
    private suspend fun createUniqueValueSymbols(): MutableList<Pair<String, BitmapDrawable>> {
        // Create a list to hold the symbol names and icons
        val symbolNamesAndIconsList = mutableListOf<Pair<String, BitmapDrawable>>()
        symbolNames.forEach { symbolName ->
            // Search for each symbol in the symbol style
            symbolStyle.getSymbol(listOf(symbolName)).onSuccess { symbol ->
                // Get a list of all categories to be mapped to the symbol
                val categories = mapSymbolNameToField(symbolName)
                categories.forEach { category ->
                    // Add each unique value category to the unique value renderer
                    uniqueValueRenderer.uniqueValues.add(
                        UniqueValue(
                            label = symbolName, symbol = symbol, values = listOf(category)
                        )
                    )
                }
                // Create a swatch from the symbol
                symbol.createSwatch(
                    screenScale = 2.0f, width = 30.0f, height = 30.0f, backgroundColor = Color.white
                ).onSuccess { symbolBitmap ->
                    // Add the symbol name and symbol to the list
                    symbolNamesAndIconsList.add(Pair(symbolName, symbolBitmap))
                }
            }
        }
        // Sort the symbol name alphabetically
        symbolNamesAndIconsList.sortBy { it.first }
        // Update the flow with the new list
        return symbolNamesAndIconsList
    }
}

/**
 * Returns a list of categories to be matched to a symbol name.
 */
private fun mapSymbolNameToField(symbolName: String): List<String> {
    return mutableListOf<String>().apply {
        when (symbolName) {
            "atm" -> add("Banking and Finance")
            "beach" -> add("Beaches and Marinas")
            "campground" -> add("Campgrounds")
            "city-hall" -> addAll(listOf("City Halls", "Government Offices"))
            "hospital" -> addAll(
                listOf(
                    "Hospitals and Medical Centers",
                    "Health Screening and Testing",
                    "Health Centers",
                    "Mental Health Centers"
                )
            )

            "library" -> add("Libraries")
            "park" -> add("Parks and Gardens")
            "place-of-worship" -> add("Churches")
            "police-station" -> add("Sheriff and Police Stations")
            "post-office" -> addAll(listOf("DHL Locations", "Federal Express Locations"))
            "school" -> addAll(
                listOf(
                    "Public High Schools", "Public Elementary Schools", "Private and Charter Schools"
                )
            )

            "trail" -> add("Trails")
        }
    }.toList()
}
