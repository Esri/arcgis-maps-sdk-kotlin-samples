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

package com.esri.arcgismaps.sample.addwfslayer.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.WfsFeatureTable
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddWfsLayerViewModel(app: Application) : AndroidViewModel(app) {
    // MapViewProxy for identify and viewpoint operations
    val mapViewProxy = MapViewProxy()

    // Message dialog for error reporting
    val messageDialogVM = MessageDialogViewModel()

    // The ArcGISMap with the Seattle downtown initial viewpoint
    var arcGISMap by mutableStateOf(createMapWithWfsLayer())
        private set

    // Track if the WFS table is currently populating
    private val _isPopulating = MutableStateFlow(false)
    val isPopulating: StateFlow<Boolean> = _isPopulating.asStateFlow()

    // Hold the WFS feature table for population
    private val wfsFeatureTable: WfsFeatureTable

    // Used to track the latest visible area (Polygon)
    private var visibleArea: Polygon? = null

    init {
        // The FeatureLayer is always the first operational layer
        val featureLayer = arcGISMap.operationalLayers.first() as FeatureLayer
        wfsFeatureTable = featureLayer.featureTable as WfsFeatureTable
        // Load the map and layer
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
            featureLayer.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Called when the visible area changes. Used to update the visible area and trigger population if needed.
     */
    fun onVisibleAreaChanged(polygon: Polygon) {
        // Only populate on first visible area event
        if (visibleArea == null) {
            visibleArea = polygon
            populateWfsLayer(polygon.extent)
        } else {
            visibleArea = polygon
        }
    }

    /**
     * Called when navigation ends. Populates the WFS table for the current visible extent.
     */
    fun onNavigatingChanged(isNavigating: Boolean) {
        if (!isNavigating) {
            visibleArea?.extent?.let { populateWfsLayer(it) }
        }
    }

    /**
     * Populate the WFS feature table for the given extent.
     */
    private fun populateWfsLayer(extent: Envelope) {
        viewModelScope.launch {
            _isPopulating.value = true
            val query = QueryParameters().apply {
                geometry = extent
                spatialRelationship = com.arcgismaps.data.SpatialRelationship.Intersects
            }
            wfsFeatureTable.populateFromService(
                parameters = query,
                clearCache = false,
                outFields = emptyList()
            ).onFailure {
                messageDialogVM.showMessageDialog(it)
            }
            _isPopulating.value = false
        }
    }

    companion object {
        private fun createMapWithWfsLayer(): ArcGISMap {
            // Seattle downtown envelope
            val envelope = Envelope(
                -122.341581, 47.613758, -122.332662, 47.617207,
                spatialReference = SpatialReference.wgs84()
            )
            val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
            map.initialViewpoint = Viewpoint(boundingGeometry = envelope)

            // WFS table and layer setup
            val wfsUrl = "https://dservices2.arcgis.com/ZQgQTuoyBrtmoGdP/arcgis/services/Seattle_Downtown_Features/WFSServer?service=wfs&request=getcapabilities"
            val tableName = "Seattle_Downtown_Features:Buildings"
            val wfsFeatureTable = WfsFeatureTable(wfsUrl, tableName).apply {
                featureRequestMode = com.arcgismaps.data.FeatureRequestMode.ManualCache
                axisOrder = com.arcgismaps.mapping.layers.OgcAxisOrder.NoSwap
            }
            val featureLayer = FeatureLayer.createWithFeatureTable(wfsFeatureTable).apply {
                renderer = SimpleRenderer(
                    SimpleLineSymbol(
                        style = SimpleLineSymbolStyle.Solid,
                        color = Color.red,
                        width = 3f
                    )
                )
            }
            map.operationalLayers.add(featureLayer)
            return map
        }
    }
}
