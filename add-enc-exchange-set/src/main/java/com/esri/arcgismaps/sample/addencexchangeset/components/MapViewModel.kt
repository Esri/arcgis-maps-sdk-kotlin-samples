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
import kotlinx.coroutines.launch
import java.io.File
import kotlin.io.path.Path

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val viewpointAmerica = Viewpoint(39.8, -98.6, 10e7)
    var viewpoint = mutableStateOf(viewpointAmerica)
    val arcGISMap by mutableStateOf(ArcGISMap(BasemapStyle.ArcGISOceans).apply {
        initialViewpoint = viewpoint.value
    })
    lateinit var completeExtent: Envelope
    val mapViewProxy = MapViewProxy()


    val encEnvironmentSettings: EncEnvironmentSettings = EncEnvironmentSettings

    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(R.string.app_name)
    }

    val encResourcesPath = provisionPath + File.separator + "hydrography"
    val encDataPath = provisionPath + File.separator + "ExchangeSetwithoutUpdates/ENC_ROOT/CATALOG.031"
    val encExchangeSet = EncExchangeSet(listOf(encDataPath))

    //val encData

    init {
        encEnvironmentSettings.resourcePath = encResourcesPath
        encEnvironmentSettings.sencDataPath = application.externalCacheDir?.path
        Path(encResourcesPath).toFile().listFiles()?.forEach { file: File? ->
            println(file)
        }
        viewModelScope.launch {
            encExchangeSet.load().onSuccess {
                encExchangeSet.datasets.forEach { encDataset ->
                    val encCell = EncCell(encDataset)
                    val encLayer = EncLayer(encCell)
                    arcGISMap.operationalLayers.add(encLayer)
                    encLayer.load().onSuccess {
                        val extent = encLayer.fullExtent

                        // Do I smell? I smell home cooking.
                        extent?.let {
                            if (!::completeExtent.isInitialized) {
                                completeExtent = extent
                            } else {
                                // if geometry engine operation successful, update the extent
                                completeExtent = GeometryEngine.combineExtentsOrNull(completeExtent, extent) ?: completeExtent
                            }
                            mapViewProxy.setViewpoint(Viewpoint(completeExtent))
                        }
                    }
                }
            }
        }

    }
}
