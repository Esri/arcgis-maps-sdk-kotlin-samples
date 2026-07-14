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

package com.esri.arcgismaps.sample.addfeaturelayers.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.GeoPackage
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.data.ShapefileFeatureTable
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.addfeaturelayers.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File

class AddFeatureLayersViewModel(app: Application) : AndroidViewModel(app) {
    //TODO - delete mutable state when the map does not change or the screen does not need to observe changes
    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
            initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
        }
    )

    val mapViewProxy = MapViewProxy()

    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() + File.separator + app.getString(R.string.add_feature_layers_app_name)
    }

    /*
        the feature layer source currently selected in the dropdown, exposed
        read-only so the screen can display it but not mutate it directly
     */
    var selectedFeatureLayerSource by mutableStateOf<FeatureLayerSource?>(null)
        private set

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    enum class FeatureLayerSource(val label: String) {
        SERVICE_FEATURE_TABLE("Service feature table"),
        PORTAL_ITEM("Portal item"),
        GEODATABASE("Geodatabase"),
        GEOPACKAGE("GeoPackage"),
        SHAPEFILE("Shapefile")
    }

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    //called when the user picks an item from the dropdown menu
    fun onFeatureLayerSourceSelected(index: Int) {
        val source = FeatureLayerSource.entries.getOrNull(index) ?: return
        if(source ==selectedFeatureLayerSource) return
        selectedFeatureLayerSource = source

        viewModelScope.launch {
            when(source) {
                FeatureLayerSource.SERVICE_FEATURE_TABLE -> loadFeatureServiceURL()
                FeatureLayerSource.PORTAL_ITEM -> loadPortalItem()
                FeatureLayerSource.GEODATABASE -> loadGeodatabase()
                FeatureLayerSource.GEOPACKAGE -> loadGeopackage()
                FeatureLayerSource.SHAPEFILE -> loadShapefile()
            }
        }
    }

    private suspend fun setFeatureLayer(layer: FeatureLayer, viewpoint: Viewpoint) {
        arcGISMap.operationalLayers.apply {
            clear()
            add(layer)
        }
        mapViewProxy.setViewpointAnimated(viewpoint)
    }

    private suspend fun loadFeatureServiceURL() {
        val serviceFeatureTable = ServiceFeatureTable(
            getApplication<Application>().getString(R.string.add_feature_layers_sample_service_url)
        )
        val featureLayer = FeatureLayer.createWithFeatureTable(serviceFeatureTable)
        val viewpoint = Viewpoint(41.70, -88.20, 120000.0)
        setFeatureLayer(featureLayer, viewpoint)
    }

    private suspend fun loadPortalItem() {
        val portal = Portal("https://www.arcgis.com")
        val portalItem = PortalItem(portal, "1759fd3e8a324358a0c58d9a687a8578")
        portalItem.load().onSuccess {
            val featureLayer = FeatureLayer.createWithItem(portalItem)
            val viewpoint = Viewpoint(45.5266, -122.6219, 2500.0)
            setFeatureLayer(featureLayer, viewpoint)
        }.onFailure {
            messageDialogVM.showMessageDialog(it)
        }
    }

    /** Load a feature layer from a local mobile geodatabase file. */
    private suspend fun loadGeodatabase() {
        val geodatabaseFile = File(
            provisionPath,
            getApplication<Application>().getString(R.string.geodatabase_la_trails)
        )
        val geodatabase = Geodatabase(geodatabaseFile.path)
        geodatabase.load().onSuccess {
            val geodatabaseFeatureTable = geodatabase.getFeatureTable("Trailheads")
            if (geodatabaseFeatureTable == null) {
                messageDialogVM.showMessageDialog(
                    IllegalStateException("Feature table name not found in geodatabase")
                )
                return@onSuccess
            }
            val featureLayer = FeatureLayer.createWithFeatureTable(geodatabaseFeatureTable)
            val viewpoint = Viewpoint(34.0772, -118.7989, 600000.0)
            setFeatureLayer(featureLayer, viewpoint)
        }.onFailure {
            messageDialogVM.showMessageDialog(it)
        }
    }

    /** Load a feature layer from a local GeoPackage file. */
    private suspend fun loadGeopackage() {
        val geopackageFile = File(provisionPath, "AuroraCO.gpkg")
        val geoPackage = GeoPackage(geopackageFile.path)
        geoPackage.load().onSuccess {
            val geoPackageFeatureTable = geoPackage.geoPackageFeatureTables.first()
            val featureLayer = FeatureLayer.createWithFeatureTable(geoPackageFeatureTable)
            val viewpoint = Viewpoint(39.7294, -104.8319, 500000.0)
            setFeatureLayer(featureLayer, viewpoint)
        }.onFailure {
            messageDialogVM.showMessageDialog(it)
        }
    }

    /** Load a feature layer from a local shapefile. */
    private suspend fun loadShapefile() {
        val shapefileFile = File(
            provisionPath,
            "ScottishWildlifeTrust_ReserveBoundaries_20201102.shp"
        )
        val shapefileFeatureTable = ShapefileFeatureTable(shapefileFile.path)
        shapefileFeatureTable.load().onSuccess {
            val featureLayer = FeatureLayer.createWithFeatureTable(shapefileFeatureTable)
            val viewpoint = Viewpoint(56.641344, -3.889066, 6000000.0)
            setFeatureLayer(featureLayer, viewpoint)
        }.onFailure {
            messageDialogVM.showMessageDialog(it)
        }
    }
}
