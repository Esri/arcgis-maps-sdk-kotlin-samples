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
import androidx.compose.runtime.setValue
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
import com.esri.arcgismaps.sample.addencexchangeset.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(
            R.string.app_name
        )
    }

    // Paths to ENC data and hydrology resources
    private val encResourcesPath = provisionPath + application.getString(R.string.enc_res_dir)
    private val encDataPath = provisionPath + application.getString(R.string.enc_data_dir)

    // Create an ENC exchange set from the local ENC data
    private val encExchangeSet = EncExchangeSet(listOf(encDataPath))
    private val encEnvironmentSettings: EncEnvironmentSettings = EncEnvironmentSettings

    // Create an empty map, to be updated once ENC data is loaded
    var arcGISMap by mutableStateOf(ArcGISMap())

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Provide ENC environment with location of ENC resources and configure SENC caching location
        encEnvironmentSettings.resourcePath = encResourcesPath
        encEnvironmentSettings.sencDataPath = application.externalCacheDir?.path

        viewModelScope.launch {
            encExchangeSet.load().onSuccess {

                // Calculate the combined extent of all datasets in the exchange set
                val exchangeSetExtent: Envelope? = encExchangeSet.extentOrNull()

                // Set the map to the oceans basemap style, and viewpoint to the exchange set extent
                arcGISMap = ArcGISMap(BasemapStyle.ArcGISOceans).apply {
                    exchangeSetExtent?.let {
                        initialViewpoint = Viewpoint(exchangeSetExtent)
                    }
                }

                encExchangeSet.datasets.forEach { encDataset ->
                    // Create a layer for each ENC dataset and add it to the map
                    val encCell = EncCell(encDataset)
                    val encLayer = EncLayer(encCell)
                    arcGISMap.operationalLayers.add(encLayer)

                    encLayer.load().onFailure { err ->
                        messageDialogVM.showMessageDialog(
                            "Error loading ENC layer",
                            err.message.toString()
                        )
                    }
                }
            }.onFailure { err ->
                messageDialogVM.showMessageDialog(
                    "Error loading ENC exchange set",
                    err.message.toString()
                )
            }
        }
    }
}

/**
 * Get the combined extent of every dataset in the exchange set.
 */
private fun EncExchangeSet.extentOrNull(): Envelope? {
    var extent: Envelope? = null

    datasets.forEach { dataset ->
        if (extent == null) {
            extent = dataset.extent
        }

        if (extent != null && dataset.extent != null) {
            // Update the combined extent of the exchange set if geometry engine returns non-null
            extent = GeometryEngine.combineExtentsOrNull(extent!!, dataset.extent!!) ?: extent
        }
    }
    return extent
}
