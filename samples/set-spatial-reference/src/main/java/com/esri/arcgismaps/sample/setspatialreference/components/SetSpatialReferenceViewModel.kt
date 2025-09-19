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

package com.esri.arcgismaps.sample.setspatialreference.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.layers.ArcGISMapImageLayer
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

/**
 * ViewModel for the "Set Spatial Reference" sample.
 */
class SetSpatialReferenceViewModel(application: Application) : AndroidViewModel(application) {

    // Message dialog view model for presenting errors
    val messageDialogVM = MessageDialogViewModel()

    // Create a map using World Bonne spatial reference (WKID: 54024)
    var arcGISMap = ArcGISMap(spatialReference = SpatialReference(wkid = WORLD_BONNE_WKID)).apply {
        // Use an ArcGISMapImageLayer as the basemap's base layer so the
        // map image service will be displayed in the map's spatial reference.
        setBasemap(basemap = Basemap(baseLayer = ArcGISMapImageLayer(url = WORLD_CITIES_MAP_SERVICE)))
    }

    init {
        viewModelScope.launch {

                // Load the map and show an error dialog if loading fails.
                arcGISMap.load().onFailure { throwable ->
                    messageDialogVM.showMessageDialog(throwable)
                }
        }
    }

    companion object {
        // The spatial reference for the sample, World Bonne (WKID: 54024)
        private const val WORLD_BONNE_WKID = 54024

        // Map image service URL with World Bonne spatial reference.
        private const val WORLD_CITIES_MAP_SERVICE =
            "https://sampleserver6.arcgisonline.com/arcgis/rest/services/SampleWorldCities/MapServer"
    }
}
