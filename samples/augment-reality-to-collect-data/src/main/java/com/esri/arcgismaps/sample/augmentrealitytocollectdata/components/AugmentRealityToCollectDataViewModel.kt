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
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.ElevationSource
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.SurfacePlacement
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewProxy
import com.arcgismaps.toolkit.ar.WorldScaleVpsAvailability
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

class AugmentRealityToCollectDataViewModel(app: Application) : AndroidViewModel(app) {
    private val basemap = Basemap(BasemapStyle.ArcGISHumanGeography)
    // The AR tree survey service feature table
    private val featureTable = ServiceFeatureTable("https://services2.arcgis.com/ZQgQTuoyBrtmoGdP/arcgis/rest/services/AR_Tree_Survey/FeatureServer/0")
    private val featureLayer = FeatureLayer.createWithFeatureTable(featureTable)
    val arcGISScene = ArcGISScene(basemap).apply {
        // an elevation source is required for the scene to be placed at the correct elevation
        // if not used, the scene may appear far below the device position because the device position
        // is calculated with elevation
        baseSurface.elevationSources.add(ElevationSource.fromTerrain3dService())
        baseSurface.backgroundGrid.isVisible = false
        baseSurface.opacity = 0.0f
        // add the AR tree survey feature layer.
        operationalLayers.add(featureLayer)
    }

    // The graphics overlay which shows marker symbols.
    val graphicsOverlay = GraphicsOverlay().apply {
        sceneProperties.surfacePlacement = SurfacePlacement.Absolute
    }

    var isVpsAvailable by mutableStateOf(false)

    val worldScaleSceneViewProxy = WorldScaleSceneViewProxy()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    var isDialogOptionsVisible by mutableStateOf(false)
        private set

    // The current marker graphic representing the user's selection
    private var treeMarker : Graphic? = null

    // A MutableSharedFlow that emits Point locations of the viewpoint camera
    val viewpointCameraLocationFlow = MutableSharedFlow<Point>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        viewModelScope.launch {
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
        periodicallyPollVpsAvailability()
    }

    // Adds a marker to the graphics overlay based on a single tap event
    fun addMarker(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        // Remove all graphics from the graphics overlay
        graphicsOverlay.graphics.clear()
        singleTapConfirmedEvent.mapPoint.let { point ->
            // Create a new marker graphic at the specified point with a diamond symbol
            val newMarker = Graphic(
                point,
                SimpleMarkerSceneSymbol(
                    SimpleMarkerSceneSymbolStyle.Diamond,
                    Color.yellow,
                    height = 1.0,
                    width = 1.0,
                    depth = 1.0
                )
            )
            treeMarker = newMarker
            graphicsOverlay.graphics.add(newMarker)
        }
    }

    // Adds a feature to represent a tree to the tree survey service feature table.
    fun addTree(context: Context, health: TreeHealth){
        treeMarker?.let { treeMarker ->
            // Set up the feature attributes
            val featureAttributes = mapOf<String, Any>(
                "Health" to health.value,
                "Height" to 3.2,
                "Diameter" to 1.2,
            )

            // Retrieve the marker's geometry as a Point
            val point = (treeMarker.geometry as? Point) ?: run {
                messageDialogVM.showMessageDialog("Something went wrong")
                return@let
            }

            // Create a new feature at the point
            val feature = featureTable.createFeature(featureAttributes, point)

            // Add the feature to the feature table
            viewModelScope.launch {
                featureTable.addFeature(feature)
                    .onSuccess {
                        // Upload changes from the local feature table to the feature service
                        featureTable.applyEdits()
                            .onSuccess { showToast(context, "Successfully added tree data!")}
                            .onFailure { e -> messageDialogVM.showMessageDialog(e) }
                    }.onFailure { e -> messageDialogVM.showMessageDialog(e) }
            }

            // Resets the feature's attributes and geometry to match the data source, discarding unsaved changes.
            feature.refresh()
        }
    }

    // Emits the camera location if it is not at (0.0, 0.0).
    fun onCurrentViewpointCameraChanged(cameraLocation: Point){
        if (cameraLocation.x != 0.0 && cameraLocation.y != 0.0) {
            viewpointCameraLocationFlow.tryEmit(cameraLocation)
        }
    }

    // Collects viewpoint camera locations once in 10 seconds and checks for VPS availability
    private fun periodicallyPollVpsAvailability(){
        viewModelScope.launch {
            viewpointCameraLocationFlow
                .sample(10_000)
                .collect { location ->
                    worldScaleSceneViewProxy.checkVpsAvailability(location.y, location.x).onSuccess {
                        isVpsAvailable = it == WorldScaleVpsAvailability.Available
                    }
                }
        }
    }

    /**
     * Displays a dialog for adding tree data if a marker exists
     */
    fun showDialog(context: Context){
        if (treeMarker == null) {
            showToast(context, "Please create marker by tapping on the screen")
            return
        }
        isDialogOptionsVisible = true
    }

    fun hideDialog(){
        isDialogOptionsVisible = false
    }
}

/**
 * Represents the health status of a tree.
 *
 * @property value The numerical value associated with the health status.
 */
enum class TreeHealth(val value: Short){
    Dead(0),
    Distressed(5),
    Healthy(10),
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
