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

package com.esri.arcgismaps.sample.addkmllayer.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.kml.KmlDataset
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.layers.KmlLayer
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.addkmllayer.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * ViewModel for the Add KML Layer sample.
 */
class AddKmlLayerViewModel(private val app: Application) : AndroidViewModel(app) {

    // Lazy provision path for local sample resource
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() + File.separator + app.getString(R.string.add_kml_layer_app_name)
    }

    // The ArcGIS map used by the composable MapView
    val arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight).apply {
        initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
    }

    // Build the list of KML options
    val kmlOptions: List<KmlOption> = listOf(
        KmlOption.LocalFile(File(provisionPath, "US_State_Capitals.kml")),
        KmlOption.FromUrl(url = "https://www.spc.noaa.gov/products/outlook/SPC_outlooks.kml"),
        KmlOption.PortalItemOption(
            PortalItem(
                portal = Portal(url = "https://www.arcgis.com"),
                itemId = "9fe0b1bfdcd64c83bd77ea0452c76253"
            )
        )
    )

    // Selected label shown in the drop-down TextField
    private val _selectedKmlOption = MutableStateFlow(kmlOptions[0])
    val selectedKmlOption = _selectedKmlOption.asStateFlow()

    // Map proxy to allow the ViewModel to set viewpoint
    val mapViewProxy: MapViewProxy = MapViewProxy()

    // Loading state to show/hide the loading dialog
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Message dialog ViewModel
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Validate local file then load the map
        val localKmlFile = File(provisionPath, "US_State_Capitals.kml")
        if (!localKmlFile.exists()) {
            messageDialogVM.showMessageDialog(
                title = "Local KML not found",
                description = "Expected file at: ${localKmlFile.canonicalPath}"
            )
        }

        // Load the default KML layer
        setKmlLayer(0)
    }

    /**
     * Sets the active KML layer on the map based on the provided index from the KML options list.
     */
    fun setKmlLayer(index: Int) {
        _selectedKmlOption.value = kmlOptions[index]

        viewModelScope.launch {
            _isLoading.value = true
            val kmlLayer = _selectedKmlOption.value.createLayer()
            // Replace the map's operational layers with the KML layer.
            arcGISMap.operationalLayers.apply {
                clear()
                add(kmlLayer)
            }
            // Attempt to load the KML layer.
            kmlLayer.load().onSuccess {
                arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
                // If the layer has a full extent after loading, use MapViewProxy to set the viewpoint.
                kmlLayer.fullExtent?.let { extent ->
                    mapViewProxy.setViewpointGeometry(boundingGeometry = extent, paddingInDips = 25.0)
                }
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Failed to load KML layer",
                    description = error.message.toString()
                )
            }
            _isLoading.value = false
        }
    }
}

/**
 * Encapsulates the three KML sources used by this sample.
 */
sealed class KmlOption(val label: String) {
    class FromUrl(val url: String) : KmlOption("From URL") {
        override fun createLayer(): KmlLayer = KmlLayer(KmlDataset(url))
    }

    class LocalFile(val file: File) : KmlOption("Local File") {
        override fun createLayer(): KmlLayer = KmlLayer(KmlDataset(file.canonicalPath))
    }

    class PortalItemOption(val portalItem: PortalItem) : KmlOption("Portal Item") {
        override fun createLayer(): KmlLayer = KmlLayer(item = portalItem)
    }

    abstract fun createLayer(): KmlLayer
}
