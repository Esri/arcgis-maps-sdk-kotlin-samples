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

package com.esri.arcgismaps.sample.browseogcapifeatureservice.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.data.FeatureRequestMode
import com.arcgismaps.data.OgcFeatureCollectionTable
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.OgcFeatureCollectionInfo
import com.arcgismaps.mapping.layers.OgcFeatureService
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BrowseOgcApiFeatureServiceViewModel(app: Application) : AndroidViewModel(app) {

    val mapViewProxy = MapViewProxy()

    // Message dialog for error handling
    val messageDialogVM = MessageDialogViewModel()

    var arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
            initialViewpoint = Viewpoint(
                center = Point(x = 36.10, y = 32.62, spatialReference = SpatialReference.wgs84()),
                scale = 200000.0
            )
        }

    // The current OGC API feature service URL
    var ogcServiceUrl by mutableStateOf("https://demo.ldproxy.net/daraa")
        private set

    // The list of available feature collection info titles
    private val _featureCollectionTitles = MutableStateFlow<List<String>>(emptyList())
    val featureCollectionTitles: StateFlow<List<String>> = _featureCollectionTitles.asStateFlow()

    // Map of title to OgcFeatureCollectionInfo
    private val featureCollectionInfos = mutableMapOf<String, OgcFeatureCollectionInfo>()

    // The currently selected feature collection title
    var selectedTitle by mutableStateOf("")
        private set

    // Show/hide the input dialog for entering a new OGC API URL
    var isUrlDialogVisible by mutableStateOf(false)
        private set

    // The text input for the OGC API URL dialog
    var urlInputText by mutableStateOf("https://demo.ldproxy.net/daraa")
        private set

    // The OGC API feature service instance
    private var ogcFeatureService: OgcFeatureService? = null

    /**
     * Loads an OGC API feature service from the given [url].
     */
    fun loadOgcFeatureService(url: String) {
        viewModelScope.launch {
            ogcFeatureService = OgcFeatureService(url)
            ogcFeatureService?.load()?.onSuccess {
                val serviceInfo = ogcFeatureService?.serviceInfo
                val infos = serviceInfo?.featureCollectionInfos ?: emptyList()
                // Map titles to info objects, discarding duplicates
                featureCollectionInfos.clear()
                infos.forEach { info ->
                    if (!featureCollectionInfos.containsKey(info.title)) {
                        featureCollectionInfos[info.title] = info
                    }
                }
                _featureCollectionTitles.value = featureCollectionInfos.keys.toList()
                // Set the selected title to the first available
                selectedTitle = featureCollectionTitles.value.firstOrNull() ?: ""
                // Display the first layer if available
                selectedTitle.takeIf { it.isNotEmpty() }?.let { title ->
                    displayLayerForTitle(title)
                }
            }?.onFailure {
                messageDialogVM.showMessageDialog(
                    title = "Failed to load OGC API feature service",
                    description = it.message.toString()
                )
            }
        }
    }

    /**
     * Loads and displays the feature layer for the given feature collection [title].
     */
    private fun displayLayerForTitle(title: String) {
        val info = featureCollectionInfos[title] ?: return
        viewModelScope.launch {
            // Remove all operational layers
            arcGISMap.operationalLayers.clear()
            // Create the OGC feature collection table
            val ogcFeatureCollectionTable = OgcFeatureCollectionTable(info)
            ogcFeatureCollectionTable.featureRequestMode = FeatureRequestMode.ManualCache
            // Populate the table with up to 1000 features
            val queryParams = QueryParameters().apply { maxFeatures = 1000 }
            ogcFeatureCollectionTable.populateFromService(queryParams, clearCache = false).onSuccess {
                // Create the feature layer
                val featureLayer = FeatureLayer.createWithFeatureTable(ogcFeatureCollectionTable)
                // Set a renderer based on geometry type
                featureLayer.renderer = createRendererForGeometryType(ogcFeatureCollectionTable.geometryType)
                arcGISMap.operationalLayers.add(featureLayer)
                info.extent?.let { envelope ->
                    mapViewProxy.setViewpointAnimated(Viewpoint(boundingGeometry = envelope))
                }
            }.onFailure {
                messageDialogVM.showMessageDialog(
                    title = "Failed to populate OGC feature collection table",
                    description = it.message.toString()
                )
            }
        }
    }

    /**
     * Creates a SimpleRenderer with a symbol appropriate for the given [geometryType].
     */
    private fun createRendererForGeometryType(geometryType: GeometryType?): SimpleRenderer? {
        return when (geometryType) {
            GeometryType.Point, GeometryType.Multipoint -> SimpleRenderer(
                SimpleMarkerSymbol(
                    style = SimpleMarkerSymbolStyle.Circle,
                    color = Color.blue,
                    size = 5f
                )
            )
            GeometryType.Polyline -> SimpleRenderer(
                SimpleLineSymbol(
                    style = SimpleLineSymbolStyle.Solid,
                    color = Color.blue,
                    width = 1f
                )
            )
            GeometryType.Polygon, GeometryType.Envelope -> SimpleRenderer(
                SimpleFillSymbol(
                    style = SimpleFillSymbolStyle.Solid,
                    color = Color.blue
                )
            )
            else -> null
        }
    }

    /**
     * Called when the user selects a new feature collection title.
     */
    fun onFeatureCollectionTitleSelected(title: String) {
        selectedTitle = title
        displayLayerForTitle(title)
    }

    /**
     * Called when the user requests to open the OGC API URL dialog.
     */
    fun onOpenUrlDialog() {
        isUrlDialogVisible = true
        urlInputText = ogcServiceUrl
    }

    /**
     * Called when the user cancels the OGC API URL dialog.
     */
    fun onCancelUrlDialog() {
        isUrlDialogVisible = false
        urlInputText = ogcServiceUrl
    }

    /**
     * Called when the user confirms a new OGC API URL.
     */
    fun onConfirmUrlDialog() {
        isUrlDialogVisible = false
        ogcServiceUrl = urlInputText
        loadOgcFeatureService(ogcServiceUrl)
    }

    /**
     * Called when the user edits the OGC API URL input text.
     */
    fun onUrlInputTextChanged(newText: String) {
        urlInputText = newText
    }

}

// Extension property to provide blue color for ArcGISMaps Color class
val Color.Companion.blue: Color
    get() = fromRgba(0, 0, 255, 255)
