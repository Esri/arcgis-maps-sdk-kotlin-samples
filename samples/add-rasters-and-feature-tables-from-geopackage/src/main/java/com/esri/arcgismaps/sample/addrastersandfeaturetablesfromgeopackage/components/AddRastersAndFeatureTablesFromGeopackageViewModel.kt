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

package com.esri.arcgismaps.sample.addrastersandfeaturetablesfromgeopackage.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.GeoPackage
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.RasterLayer
import com.esri.arcgismaps.sample.addrastersandfeaturetablesfromgeopackage.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File

class AddRastersAndFeatureTablesFromGeopackageViewModel(app: Application) : AndroidViewModel(app) {

    private val provisionPath: String by lazy { app.getExternalFilesDir(null)?.path.toString() +
        File.separator +
        app.getString(R.string.add_rasters_and_feature_tables_from_geopackage_app_name)
    }

    // A map with a light gray basemap.
    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISLightGray).apply {
            initialViewpoint = Viewpoint(latitude = 39.7294, longitude = -104.8319, scale = 3e5)
        }
    )

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()


    init {
        viewModelScope.launch {
            arcGISMap.load()
            .onSuccess {
                loadRastersAndFeatureTablesFromGeopackage()
            }
            .onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = application.getString(R.string.add_rasters_and_feature_tables_from_geopackage_failed_to_load_map),
                    description =error.message.toString()
                )
            }
        }
    }

    private suspend fun loadRastersAndFeatureTablesFromGeopackage() {
        // Retrieve the Geopackage File
        val geopackageFile = File(provisionPath, "AuroraCO.gpkg")

        // Create the Geopackage object
        val geoPackage = GeoPackage(geopackageFile.path)

        // Load the GeoPackage before reading its raster and featuretable collections.
        geoPackage.load().onSuccess {
            // Create RasterLayers for each geoPackageRaster in the loaded geopackage.
            val rasterLayers = geoPackage.geoPackageRasters.map { geoPackageRaster ->
                val layer = RasterLayer(geoPackageRaster)
                // Makes the layer semi-transparent so it doesn't obscure the contents beneath it.
                layer.opacity = 0.5f
                layer
            }

            // Create FeatureLayers for each geoPackageFeatureTable in the loaded geopackage.
            val featureLayers = geoPackage.geoPackageFeatureTables.map { geoPackageFeatureTable ->
                FeatureLayer.createWithFeatureTable(geoPackageFeatureTable)
            }

            // Add the arrays of feature and raster layers to the map.
            arcGISMap.operationalLayers.addAll(rasterLayers + featureLayers)

        }.onFailure { error ->
            messageDialogVM.showMessageDialog(
                title = application.getString(R.string.add_rasters_and_feature_tables_from_geopackage_failed_to_load_geopackage),
                description = error.message.toString()
            )
        }
    }
}
