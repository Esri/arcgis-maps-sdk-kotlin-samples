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
import com.arcgismaps.data.ServiceGeodatabase
import com.arcgismaps.geometry.Envelope
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
import com.arcgismaps.mapping.view.geometryeditor.SnapSourceSettings
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.snaptofeatures.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    application: Application,
    private val sampleCoroutineScope: CoroutineScope
) : AndroidViewModel(application) {

    // create a map using a basemap
    val map = ArcGISMap(Basemap(BasemapStyle.ArcGISStreetsNight))

    // create a service geodatabase, graphic, graphic overlay, and geometry editor
    private var serviceGeodatabase = ServiceGeodatabase("")
    private var identifiedGraphic = Graphic()
    val graphicsOverlay = GraphicsOverlay()
    val geometryEditor = GeometryEditor()

    // create a mapViewProxy that will be used to identify features in the MapView
    // should also be passed to the  composable MapView this mapViewProxy is associated with
    val mapViewProxy = MapViewProxy()

    // create a messageDialogViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    // create a list to be used for displaying the snap sources in the bottom sheet
    private val _snapSourceSettingsList = MutableStateFlow(listOf<SnapSourceSettings>())
    val snapSourceList: StateFlow<List<SnapSourceSettings>> = _snapSourceSettingsList

    // boolean flags to track the state of the geometry editor, snap settings, and UI components
    val isSnapSettingsButtonEnabled = mutableStateOf(false)
    val isCreateButtonEnabled = mutableStateOf(false)
    val isBottomSheetVisible = mutableStateOf(false)
    val snappingCheckedState = mutableStateOf(false)
    val snapSourceCheckedState = mutableStateListOf(false)

    /**
     * Add the data layer to the map and sync the snap source collection.
     */
    init {
        sampleCoroutineScope.launch {
            // set the map's viewpoint to Naperville, Illinois
            mapViewProxy.setViewpointCenter(Point(-9812798.0, 5126406.0), 2000.0)
            // create a service geodatabase from the uri and load it
            serviceGeodatabase = ServiceGeodatabase(application.getString(R.string.service_url))
            serviceGeodatabase.load().onSuccess {
                for (layerID in serviceGeodatabase.serviceInfo?.layerInfos?.indices!!.reversed()) {
                    // create a feature layer from the service feature table
                    val featureTable = serviceGeodatabase.getTable(layerID.toLong())
                    val featureLayer = FeatureLayer.createWithFeatureTable(featureTable!!)
                    // set the feature tiling mode and load the layer
                    featureLayer.tilingMode = FeatureTilingMode.EnabledWithFullResolutionWhenSupported
                    featureLayer.load().onSuccess {
                        // add the layer to the Map's operational layers
                        map.operationalLayers.add(featureLayer)
                    }.onFailure { error ->
                        messageDialogVM.showMessageDialog(
                            error.message.toString(),
                            error.cause.toString()
                        )
                    }
                }
                // enable the create and snap settings buttons
                isCreateButtonEnabled.value = true
                isSnapSettingsButtonEnabled.value = true
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
                        isCreateButtonEnabled.value = false
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
    fun startEditor(selectedGeometry: GeometryType) {
        if (!geometryEditor.isStarted.value) {
            geometryEditor.start(selectedGeometry)
            isCreateButtonEnabled.value = false
        }
    }

    /**
     * Stop the GeometryEditor and update the Graphic or GraphicsOverlay.
     */
    fun stopEditor() {
        if (identifiedGraphic.geometry != null) {
            identifiedGraphic.geometry = geometryEditor.stop()
            identifiedGraphic.isSelected = false
        } else {
            if (geometryEditor.isStarted.value) {
                createNewGraphic()
            }
        }
        isCreateButtonEnabled.value = true
    }

    /**
     * Create a Graphic from the GeometryEditor's geometry and add it to the GraphicsOverlay.
     */
    private fun createNewGraphic() {
        // stop the geometryEditor and store the geometry
        val geometry = geometryEditor.stop()
        val graphic = Graphic(geometry)

        // apply symbology to the graphic
        when (geometry!!) {
            is Point -> graphic.symbol = GeometryEditorStyle().vertexSymbol
            is Multipoint -> graphic.symbol = GeometryEditorStyle().vertexSymbol
            is Polyline -> graphic.symbol = GeometryEditorStyle().lineSymbol
            is Polygon -> graphic.symbol = GeometryEditorStyle().fillSymbol
            is Envelope -> graphic.symbol = GeometryEditorStyle().lineSymbol
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
        isCreateButtonEnabled.value = true
    }

    /**
     * Update the snapSettings.isEnabled value using the [checkedValue] from the BottomSheet toggle.
     */
    fun snappingEnabledStatus(checkedValue: Boolean) {
        snappingCheckedState.value = checkedValue
        geometryEditor.snapSettings.isEnabled = snappingCheckedState.value
    }

    /**
     * Update the sourceSettings at [index] enabled value to the [checkedValue]
     * from the BottomSheet toggle.
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
        if (geometryEditor.snapSettings.sourceSettings.isEmpty()) {
            // synchronise the snap source collection with the Map's operational layers
            geometryEditor.snapSettings.syncSourceSettings()
            // update the lists used for the UI
            _snapSourceSettingsList.value = geometryEditor.snapSettings.sourceSettings
            geometryEditor.snapSettings.sourceSettings.forEach {
                snapSourceCheckedState.add(it.isEnabled)
            }
        }
        isBottomSheetVisible.value = true
    }
}
