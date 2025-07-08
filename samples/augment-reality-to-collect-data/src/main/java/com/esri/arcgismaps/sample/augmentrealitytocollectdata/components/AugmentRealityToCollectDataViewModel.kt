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

package com.esri.arcgismaps.sample.augmentrealitytocollectdata.components

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.ElevationSource
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.SurfacePlacement
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class AugmentRealityToCollectDataViewModel(app: Application) : AndroidViewModel(app) {
    val basemap = Basemap(BasemapStyle.ArcGISHumanGeography)
    val featureTable = ServiceFeatureTable("https://services2.arcgis.com/ZQgQTuoyBrtmoGdP/arcgis/rest/services/AR_Tree_Survey/FeatureServer/0")
    val featureLayer = FeatureLayer.createWithFeatureTable(featureTable)
    val arcGISScene = ArcGISScene(basemap).apply {
        // an elevation source is required for the scene to be placed at the correct elevation
        // if not used, the scene may appear far below the device position because the device position
        // is calculated with elevation
        baseSurface.elevationSources.add(ElevationSource.fromTerrain3dService())
        baseSurface.backgroundGrid.isVisible = false
        baseSurface.opacity = 0.0f
        // add the AR tree survey service feature table.
        operationalLayers.add(featureLayer)
    }

    val graphicsOverlay = GraphicsOverlay().apply {
        sceneProperties.surfacePlacement = SurfacePlacement.Absolute
    }

    val worldScaleSceneViewProxy = WorldScaleSceneViewProxy()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    private var marker : Graphic? = null

    init {
        viewModelScope.launch {
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    fun addMarker(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        graphicsOverlay.graphics.removeFirstOrNull()
        worldScaleSceneViewProxy
            .screenToBaseSurface(singleTapConfirmedEvent.screenCoordinate)
            ?.let { point ->
                val newMarker = Graphic(
                    point,
                    SimpleMarkerSceneSymbol(
                        SimpleMarkerSceneSymbolStyle.Diamond,
                        Color.green,
                        height = 1.0,
                        width = 1.0,
                        depth = 1.0
                    )
                )
                marker = newMarker
                graphicsOverlay.graphics.add(newMarker)
            }
    }

    fun addTree(context: Context, health: TreeHealth){
        if (marker == null) {
            showToast(context, "Please create marker by tapping on the screen")
            return
        }
        marker?.let {
            // set up the feature attributes
            val featureAttributes = mapOf<String, Any>(
                "Health" to health.value,
                "Height" to 3.2,
                "Diameter" to 1.2,
            )

            // get Point
            val point = (it.geometry as? Point)
            if (point == null) {
                showToast(context, "Something went wrong")
                return@let
            }

            // create a new feature at the mapPoint
            val feature = featureTable.createFeature(featureAttributes, point)

            // add the feature to the feature table
            viewModelScope.launch {
                featureTable.addFeature(feature)
                    .onSuccess {
                        // Upload changes to the local table to the feature service.
                        featureTable.applyEdits()
                            .onSuccess { showToast(context, "Successfully added tree data!")}
                            .onFailure { e -> showError(context, e) }
                    }.onFailure { e -> showError(context, e) }
            }

            feature.refresh()
        }
    }
}

enum class TreeHealth(val value: Short){
    Dead(0),
    Distressed(5),
    Healthy(10),
}

fun showError(context: Context, e: Throwable){
    Log.d("AugmentRealityToCollectDataViewModel", e.message ?: e.toString())
    showToast(context, e.message ?: "Unknown error")
}

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
