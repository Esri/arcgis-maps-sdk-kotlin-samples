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

package com.esri.arcgismaps.sample.snaptofeatures.components

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.FeatureTilingMode
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditorStyle
import com.arcgismaps.toolkit.geocompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.snaptofeatures.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MapViewModel(
    application: Application,
    private val sampleCoroutineScope: CoroutineScope
) : AndroidViewModel(application) {

    // Uris for the service feature layers of the Oil Sands project map
    private val boundariesServiceURL = application.getString(R.string.oilSandsBoundaries_url)
    private val pointsServiceURL = application.getString(R.string.oilSandsPoints_url)

    // create a map using the ArcGISNavigation basemap
    val map = ArcGISMap(Basemap(BasemapStyle.ArcGISNavigation))

    // create a geometryEditor, graphic, and graphicsOverlay
    val geometryEditor = GeometryEditor()
    var identifiedGraphic = Graphic()
    val graphicsOverlay = GraphicsOverlay()

    // create a mapViewProxy that will be used to identify features in the MapView
    // should also be passed to the  composable MapView this mapViewProxy is associated with
    val mapViewProxy = MapViewProxy()

    // create a messageDialogViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    // create boolean flags to track the state of the bottom sheet and snap settings
    val isBottomSheetVisible = mutableStateOf(false)
    val snappingCheckedState = mutableStateOf(false)
    val snapSourceCheckedState = mutableStateListOf(false)

    /**
     * Add the data layer to the map and sync the snap source collection.
     */
    init {
        sampleCoroutineScope.launch {
            // create feature tables from the Uri
            val featureTableBoundaries = ServiceFeatureTable(boundariesServiceURL)
            val featureTablePoints = ServiceFeatureTable(pointsServiceURL)

            // create feature layers from the feature tables
            val featureLayerBoundaries = FeatureLayer.createWithFeatureTable(featureTableBoundaries)
            val featureLayerPoints = FeatureLayer.createWithFeatureTable(featureTablePoints)

            // Set the tiling mode of the feature tables to disabled to ensure full-resolution geometries
            featureLayerBoundaries.tilingMode = FeatureTilingMode.Disabled
            featureLayerPoints.tilingMode = FeatureTilingMode.Disabled

            // load the layers and add them to the map's operational layers
            if (featureLayerBoundaries.load().isSuccess && featureLayerPoints.load().isSuccess){
                map.operationalLayers.add(featureLayerBoundaries)
                map.operationalLayers.add(featureLayerPoints)

                // set the map's initial viewpoint to the featureLayerBoundaries full extent
                featureLayerBoundaries.fullExtent?.let {
                    mapViewProxy.setViewpointAnimated(Viewpoint(it.extent))
                }

                // call syncSourceSettings() to synchronise the snap source collection with
                // the Map's operational layers
                geometryEditor.snapSettings.syncSourceSettings()

                // populate the snapSourceCheckedState list with default values
                geometryEditor.snapSettings.sourceSettings.forEach {
                    snapSourceCheckedState.add(it.isEnabled)
                }
            } else {
                messageDialogVM.showMessageDialog(
                    "Error",
                    "The data layers failed to load."
                )
            }
        }
    }

    /**
     * Identifies the graphic at the tapped screen coordinate in the provided [singleTapConfirmedEvent]
     * and starts the GeometryEditor using the graphic's geometry. Hide the BottomSheet on
     * [singleTapConfirmedEvent].
     */
    fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        sampleCoroutineScope.launch {
            val graphicsResult = mapViewProxy.identifyGraphicsOverlays(
                screenCoordinate =  singleTapConfirmedEvent.screenCoordinate,
                tolerance = 10.0.dp,
                returnPopupsOnly = false
            ).getOrNull()

            if (!geometryEditor.isStarted.value) {
                if (graphicsResult != null) {
                    if (graphicsResult.isNotEmpty()) {
                        identifiedGraphic = graphicsResult[0].graphics[0]
                        identifiedGraphic.isSelected = true
                        identifiedGraphic.geometry?.let { geometryEditor.start(it) }
                    }
                }
                identifiedGraphic.geometry = null
            }
        }
        dismissBottomSheet()
    }

    /**
     * Starts the GeometryEditor using the selected [GeometryType] from the DropDownMenu.
     */
    fun editorStarted(type: GeometryType) {
        if (!geometryEditor.isStarted.value) {
            geometryEditor.start(type)
        }
    }

    /**
     * Stops the GeometryEditor adding the current edit to the GraphicsOverlay.
     */
    fun editorStopped() {
        if (identifiedGraphic.geometry != null) {
            updateSelectedGraphic()
        } else {
            if (geometryEditor.isStarted.value) {
                createNewGraphic()
            }
        }
    }

    /**
     * Update the current graphic's geometry and unselect it.
     */
    private fun updateSelectedGraphic() {
        identifiedGraphic.geometry = geometryEditor.stop()
        identifiedGraphic.isSelected = false
    }

    /**
     * Create a Graphic from the GeometryEditor's geometry and add it to the GraphicsOverlay.
     */
    private fun createNewGraphic() {
        // stop the geometryEditor and store the geometry
        val geometry = geometryEditor.stop()
        // create a new graphic from the geometry
        val graphic = Graphic(geometry)

        // apply symbology to the graphic based on the geometry
        when (geometry!!) {
            is Point -> graphic.symbol = GeometryEditorStyle().vertexSymbol
            is Multipoint -> graphic.symbol = GeometryEditorStyle().vertexSymbol
            is Polyline -> graphic.symbol = GeometryEditorStyle().lineSymbol
            is Polygon -> graphic.symbol = GeometryEditorStyle().fillSymbol
            else -> {
                messageDialogVM.showMessageDialog(
                    "Error",
                    "The current geometry edit cannot be saved."
                )
            }
        }
        // add the graphic to the graphicOverlay and unselect it
        graphicsOverlay.graphics.add(graphic)
        graphic.isSelected = false
    }

    /**
     * Undo the last event on the GeometryEditor.
     */
    fun editorUndo() {
        geometryEditor.undo()
    }

    /**
     * Stop the GeometryEditor and clear the GraphicsOverlay.
     */
    fun clearGraphics() {
        editorStopped()
        graphicsOverlay.graphics.clear()
    }

    /**
     * Update the snapSettings.isEnabled value using the [checkedValue] from the BottomSheet toggle.
     */
    fun snappingEnabledStatus(checkedValue: Boolean) {
        snappingCheckedState.value = checkedValue
        geometryEditor.snapSettings.isEnabled = snappingCheckedState.value
    }

    /**
     * Update the sourceSettings.isEnabled value of a snap source at [index]
     * using the [checkedValue] from the BottomSheet toggle.
     */
    fun sourceEnabledStatus(checkedValue: Boolean, index: Int) {
        snapSourceCheckedState[index] = checkedValue
        geometryEditor.snapSettings.sourceSettings[index].isEnabled = snapSourceCheckedState[index]
    }

    /**
     * Dismiss the BottomSheet.
     */
    fun dismissBottomSheet() {
        isBottomSheetVisible.value = false
    }

    /**
     * Show the BottomSheet.
     */
    fun showBottomSheet() {
        isBottomSheetVisible.value = true
    }
}
