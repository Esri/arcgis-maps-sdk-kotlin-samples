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

package com.esri.arcgismaps.sample.applysymbologytoshapefile.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.data.ShapefileFeatureTable
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.esri.arcgismaps.sample.applysymbologytoshapefile.R
import kotlinx.coroutines.launch
import java.io.File
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel

class ApplySymbologyToShapefileViewModel(application: Application) : AndroidViewModel(application) {
    val provisionPath =
        application.getExternalFilesDir(null)?.path + File.separator + application.getString(R.string.apply_symbology_to_shapefile_app_name)

    // Initialize the map with a topographic basemap and an initial viewpoint
    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
            val center = Point(
                x = -11662054.0,
                y = 4818336.0,
                spatialReference = SpatialReference.webMercator()
            )
            initialViewpoint = Viewpoint(center, 200000.0)
        }
    )

    // Create a message dialog view model to surface errors
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            try {
                // Ensure the shapefile is available on device storage and load its layer
                val shapefileTable = ShapefileFeatureTable("${provisionPath}${File.separator}Subdivisions.shp")
                val featureLayer = FeatureLayer.createWithFeatureTable(shapefileTable)

                // Define renderer: red outline with yellow fill for all features
                val outlineSymbol = SimpleLineSymbol(
                    style = SimpleLineSymbolStyle.Solid,
                    color = Color.red,
                    width = 1.0f
                )
                val fillSymbol = SimpleFillSymbol(
                    style = SimpleFillSymbolStyle.Solid,
                    color = Color.yellow,
                    outline = outlineSymbol
                )

                // Apply the renderer to the layer
                featureLayer.renderer = SimpleRenderer(fillSymbol)

                // Add the shapefile layer to the map and attempt to load the map
                arcGISMap.operationalLayers.add(featureLayer)
                arcGISMap.load().onSuccess { /* map ready */ }.onFailure { error ->
                    messageDialogVM.showMessageDialog(error)
                }
            } catch (e: Exception) {
                messageDialogVM.showMessageDialog(e)
            }
        }
    }
}
