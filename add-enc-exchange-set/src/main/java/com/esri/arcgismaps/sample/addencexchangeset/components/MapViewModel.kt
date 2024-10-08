/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.addencexchangeset.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.hydrography.EncCell
import com.arcgismaps.hydrography.EncEnvironmentSettings
import com.arcgismaps.hydrography.EncExchangeSet
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.EncLayer
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.addencexchangeset.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(R.string.app_name)
    }

    // Paths to ENC data and hydrology resources
    private val encResourcesPath = provisionPath + application.getString(R.string.enc_res_dir)
    private val encDataPath = provisionPath + application.getString(R.string.enc_data_dir)

    // Create an ENC exchange set from the local ENC data
    private val encExchangeSet = EncExchangeSet(listOf(encDataPath))
    private val encEnvironmentSettings: EncEnvironmentSettings = EncEnvironmentSettings

    // Envelope for setting map view, to be initialised from extent of ENC layers
    private lateinit var completeExtent: Envelope

    // Create a map with the oceans basemap style
    val arcGISMap by mutableStateOf(ArcGISMap(BasemapStyle.ArcGISOceans))
    // Create a map view proxy for handling updates to the map view
    val mapViewProxy = MapViewProxy()
    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        // provide ENC environment with location of ENC resources and configure SENC caching location
        encEnvironmentSettings.resourcePath = encResourcesPath
        encEnvironmentSettings.sencDataPath = application.externalCacheDir?.path

        viewModelScope.launch {
            encExchangeSet.load().onSuccess {
                encExchangeSet.datasets.forEach { encDataset ->

                    // create a layer for each ENC dataset and add it to the map
                    val encCell = EncCell(encDataset)
                    val encLayer = EncLayer(encCell)
                    arcGISMap.operationalLayers.add(encLayer)

                    encLayer.load().onSuccess {
                        val extent = encLayer.fullExtent
                        extent?.let {
                            // initialise, or update, the extent of all ENC layers
                            completeExtent = when(::completeExtent.isInitialized){
                                // use previous value if geometry engine fails to combine extents
                                true -> GeometryEngine.combineExtentsOrNull(completeExtent, extent) ?: completeExtent
                                false -> extent
                            }
                        }
                    }.onFailure { err ->
                        messageDialogVM.showMessageDialog("Error loading ENC layer", err.message.toString())
                    }
                }

                // set the viewpoint to the extent bounding all ENC layers
                mapViewProxy.setViewpoint(Viewpoint(completeExtent))
            }.onFailure { err ->
                messageDialogVM.showMessageDialog("Error loading ENC exchange set", err.message.toString())
            }
        }
    }
}
