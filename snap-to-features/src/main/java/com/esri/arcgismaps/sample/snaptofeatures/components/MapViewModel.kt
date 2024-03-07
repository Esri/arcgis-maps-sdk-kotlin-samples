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
import com.arcgismaps.data.FeatureQueryResult
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.ServiceGeodatabase
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
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

    // Uri for the feature server
    private val serviceURL = application.getString(R.string.service_url)

    // create a map using a basemap
    val map = ArcGISMap(Basemap(BasemapStyle.ArcGISStreetsNight))

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
            // set the map's viewpoint to Naperville, Illinois
            mapViewProxy.setViewpointCenter(Point(-9812726.0, 5126959.0), 5000.0)

            // create a service geodatabase from the uri and load it
            val layerErrorList = mutableListOf<Throwable>()
            val serviceGeodatabase = ServiceGeodatabase(serviceURL)
            serviceGeodatabase.load().onSuccess {
                for ( x in serviceGeodatabase.serviceInfo?.layerInfos?.indices!!) {
                    // create a feature layer from the service feature table
                    val featureTable = serviceGeodatabase.getTable(x.toLong())
                    val featureLayer = FeatureLayer.createWithFeatureTable(featureTable!!)
                    // set the feature tiling mode and load the layer
                    featureLayer.tilingMode = FeatureTilingMode.Disabled
                    featureLayer.load().onSuccess {
                        // add the layer to the Map's operational layers
                        map.operationalLayers.add(featureLayer)
                    }.onFailure { error -> layerErrorList.add(error) }
                }
                if (layerErrorList.isNotEmpty()) {
                    var errorString = String()
                    layerErrorList.forEach {
                        errorString = it.message.toString()
                        errorString.plus("\n${it.cause.toString()}\n\n")
                    }
                    messageDialogVM.showMessageDialog(
                        "Error loading data layers.",
                        errorString
                    )
                }
                // call syncSourceSettings() to synchronise the snap source collection with
                // the Map's operational layers
                geometryEditor.snapSettings.syncSourceSettings()
                // populate the snapSourceCheckedState list with default values
                geometryEditor.snapSettings.sourceSettings.forEach {
                    snapSourceCheckedState.add(it.isEnabled)
                }
                // Update the geometry editor line symbol style
                setGeometryEditorStyle()
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    error.message.toString(),
                    error.cause.toString()
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
     * Stop the GeometryEditor and update the Graphic or GraphicsOverlay.
     */
    fun editorStopped() {
        if (identifiedGraphic.geometry != null) {
            identifiedGraphic.geometry = geometryEditor.stop()
            identifiedGraphic.isSelected = false
        } else {
            if (geometryEditor.isStarted.value) {
                createNewGraphic()
            }
        }
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
            is Polyline -> graphic.symbol = geometryEditor.tool.style.lineSymbol
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
     * Stop the GeometryEditor and remove the selected Graphic or clear the GraphicsOverlay.
     */
    fun clearGraphics() {
        identifiedGraphic.geometry = geometryEditor.stop()
        if (identifiedGraphic.geometry != null) {
            graphicsOverlay.graphics.remove(identifiedGraphic)
            identifiedGraphic.geometry = null
        } else {
            graphicsOverlay.graphics.clear()
        }
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
     * Set the GeometryEditor LineSymbol to the queried line feature symbol.
     */
    private fun setGeometryEditorStyle() {
        val queryParameters = QueryParameters()
        queryParameters.whereClause = "OBJECTID=139118"
        sampleCoroutineScope.launch {
            val featureQueryResult =
                (map.operationalLayers[8] as FeatureLayer).featureTable?.queryFeatures(
                    queryParameters
                )?.getOrElse {
                    messageDialogVM.showMessageDialog(it.message.toString(), it.cause.toString())
                } as FeatureQueryResult
            val feature = featureQueryResult.firstOrNull()
            val renderer = (map.operationalLayers[8] as FeatureLayer).renderer
            geometryEditor.tool.style.lineSymbol = feature?.let {
                renderer?.getSymbol(it, true)
            }
        }
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
