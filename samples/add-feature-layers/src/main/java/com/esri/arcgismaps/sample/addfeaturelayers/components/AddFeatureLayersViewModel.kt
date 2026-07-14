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
    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
            initialViewpoint = Viewpoint(latitude = 39.8, longitude = -98.6, scale = 10e7)
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
        SERVICE_FEATURE_TABLE(label = "Service feature table"),
        PORTAL_ITEM(label = "Portal item"),
        GEODATABASE(label = "Geodatabase"),
        GEOPACKAGE(label = "GeoPackage"),
        SHAPEFILE(label = "Shapefile")
    }

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    //Called when the user picks an item from the dropdown menu
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

    // Replaces the map's operational layer with the given layer and then animates to the given viewpoint
    private suspend fun setFeatureLayer(layer: FeatureLayer, viewpoint: Viewpoint) {
        arcGISMap.operationalLayers.apply {
            clear()
            add(layer)
        }
        mapViewProxy.setViewpointAnimated(viewpoint)
    }

    /* Load a feature layer with a URL */
    private suspend fun loadFeatureServiceURL() {
        //Initialize the service feature table using a URL
        val serviceFeatureTable = ServiceFeatureTable(
            uri = getApplication<Application>().getString(R.string.add_feature_layers_sample_service_url)
        )
        // create a feature layer with the feature table
        val featureLayer = FeatureLayer.createWithFeatureTable(serviceFeatureTable)
        //Viewpoint centered on Naperville, IL
        val viewpoint = Viewpoint(latitude = 41.70, longitude = -88.20, scale = 120000.0)
        setFeatureLayer(featureLayer, viewpoint)
    }

    /* Load a feature layer with a portal item */
    private suspend fun loadPortalItem() {
        //Connect to the public ArcGIS online portal
        val portal = Portal(url = "https://www.arcgis.com")
        val portalItem = PortalItem(portal = portal, itemId = "1759fd3e8a324358a0c58d9a687a8578")
        portalItem.load().onSuccess {
            val featureLayer = FeatureLayer.createWithItem(portalItem)
            //viewpoint centered on Portland, OR
            val viewpoint = Viewpoint(latitude = 45.5266, longitude = -122.6219, scale = 2500.0)
            setFeatureLayer(featureLayer, viewpoint)
        }.onFailure {
            messageDialogVM.showMessageDialog(it)
        }
    }

    /* Load a feature layer from a local mobile geodatabase file. */
    private suspend fun loadGeodatabase() {
        //Locate the .geodatabase file downloaded to the device by DownloadActivity
        val geodatabaseFile = File(
            provisionPath,
            getApplication<Application>().getString(R.string.geodatabase_la_trails)
        )
        val geodatabase = Geodatabase(geodatabaseFile.path)
        geodatabase.load().onSuccess {
            //Get the "Trailheads" feature table from the geodatabase
            val geodatabaseFeatureTable = geodatabase.getFeatureTable(tableName = "Trailheads")
            if (geodatabaseFeatureTable == null) {
                messageDialogVM.showMessageDialog(
                    IllegalStateException("Feature table name not found in geodatabase")
                )
                return@onSuccess
            }
            val featureLayer = FeatureLayer.createWithFeatureTable(geodatabaseFeatureTable)
            //viewpoint centered on Malibu, CA
            val viewpoint = Viewpoint(latitude = 34.0772, longitude = -118.7989, scale = 600000.0)
            setFeatureLayer(featureLayer, viewpoint)
        }.onFailure {
            messageDialogVM.showMessageDialog(it)
        }
    }

    /* Load a feature layer from a local GeoPackage file. */
    private suspend fun loadGeopackage() {
        //locate the .gpkg file
        val geopackageFile = File(provisionPath, "AuroraCO.gpkg")
        val geoPackage = GeoPackage(geopackageFile.path)
        geoPackage.load().onSuccess {
            val geoPackageFeatureTable = geoPackage.geoPackageFeatureTables.first()
            val featureLayer = FeatureLayer.createWithFeatureTable(geoPackageFeatureTable)
            // viewpoint centered on Denver, CO
            val viewpoint = Viewpoint(latitude = 39.7294, longitude = -104.8319, scale = 500000.0)
            setFeatureLayer(featureLayer, viewpoint)
        }.onFailure {
            messageDialogVM.showMessageDialog(it)
        }
    }

    /* Load a feature layer from a local shapefile. */
    private suspend fun loadShapefile() {
        val shapefileFile = File(
            provisionPath,
            "ScottishWildlifeTrust_ReserveBoundaries_20201102.shp"
        )
        val shapefileFeatureTable = ShapefileFeatureTable(shapefileFile.path)
        shapefileFeatureTable.load().onSuccess {
            val featureLayer = FeatureLayer.createWithFeatureTable(shapefileFeatureTable)
            val viewpoint = Viewpoint(
                latitude = 56.641344,
                longitude = -3.889066,
                scale = 6000000.0
            )
            setFeatureLayer(featureLayer, viewpoint)
        }.onFailure {
            messageDialogVM.showMessageDialog(it)
        }
    }
}
