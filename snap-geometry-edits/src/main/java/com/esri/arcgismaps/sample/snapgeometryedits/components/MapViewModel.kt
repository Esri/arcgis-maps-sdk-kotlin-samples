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

package com.esri.arcgismaps.sample.snapgeometryedits.components

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.layers.FeatureTilingMode
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditorStyle
import com.arcgismaps.mapping.view.geometryeditor.ReticleVertexTool
import com.arcgismaps.mapping.view.geometryeditor.SnapSourceSettings
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.snapgeometryedits.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    application: Application,
    private val sampleCoroutineScope: CoroutineScope
) : AndroidViewModel(application) {
    // create a map using the URL of the web map
    val map = ArcGISMap(application.getString(R.string.web_map))

    // create a graphic, graphic overlay, and geometry editor
    private var identifiedGraphic = Graphic()
    val graphicsOverlay = GraphicsOverlay()
    val geometryEditor = GeometryEditor()

    // create a mapViewProxy that will be used to identify features in the MapView and set the viewpoint
    val mapViewProxy = MapViewProxy()

    // create a messageDialogViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    // create lists for displaying the snap sources in the bottom sheet
    private val _snapSourceSettingsList = MutableStateFlow(listOf<SnapSourceSettings>())
    val snapSourceList: StateFlow<List<SnapSourceSettings>> = _snapSourceSettingsList

    // create boolean flags to track the state of UI components
    val isCreateButtonEnabled = mutableStateOf(false)
    val isSnapSettingsButtonEnabled = mutableStateOf(false)
    val isBottomSheetVisible = mutableStateOf(false)
    val snappingCheckedState = mutableStateOf(false)
    val snapSourceCheckedState = mutableStateListOf<Boolean>()
    val isUndoButtonEnabled = geometryEditor.canUndo
    val isSaveButtonEnabled = geometryEditor.isStarted
    val isDeleteButtonEnabled = geometryEditor.isStarted

    /**
     * Configure the map and enable the UI after the map's layers are loaded.
     */
    init {
        sampleCoroutineScope.launch {
            // set the id for the graphics overlay
            graphicsOverlay.id = "Editor Graphics Overlay"
            // set the tool for the geometry editor to use a reticle
            geometryEditor.tool = ReticleVertexTool()
            // set the feature layer's tiling mode
            map.loadSettings.featureTilingMode =
                FeatureTilingMode.EnabledWithFullResolutionWhenSupported
            // load the map
            map.load().onSuccess {
                // load the map's operational layers
                map.operationalLayers.forEach { layer ->
                    layer.load().onFailure { error ->
                        messageDialogVM.showMessageDialog(
                            error.message.toString(),
                            error.cause.toString()
                        )
                    }
                }
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    error.message.toString(),
                    error.cause.toString()
                )
            }
            isCreateButtonEnabled.value = true
            isSnapSettingsButtonEnabled.value = true
        }
    }

    /**
     * Synchronises the snap source collection with the map's operational layers, sets the bottom
     * sheet UI, and shows it to configure snapping.
     */
    fun showBottomSheet() {
        if (geometryEditor.snapSettings.sourceSettings.isEmpty()) {
            // sync the snap source collection
            geometryEditor.snapSettings.syncSourceSettings()
            // initialise the snap source lists used for the bottom sheet
            geometryEditor.snapSettings.sourceSettings.forEach { snapSource ->
                snapSourceCheckedState.add(snapSource.isEnabled)
            }
            _snapSourceSettingsList.value = geometryEditor.snapSettings.sourceSettings
        }
        isBottomSheetVisible.value = true
    }

    /**
     * Toggles snapping using the [checkedValue] from the bottom sheet.
     */
    fun snappingEnabledStatus(checkedValue: Boolean) {
        snappingCheckedState.value = checkedValue
        geometryEditor.snapSettings.isEnabled = snappingCheckedState.value
    }

    /**
     * Toggles snapping for the snap source at [index] using the [checkedValue] from the
     * BottomSheet.
     */
    fun sourceEnabledStatus(checkedValue: Boolean, index: Int) {
        snapSourceCheckedState[index] = checkedValue
        geometryEditor.snapSettings.sourceSettings[index].isEnabled = snapSourceCheckedState[index]
    }

    /**
     * Hides the bottom sheet.
     */
    fun dismissBottomSheet() {
        isBottomSheetVisible.value = false
    }

    /**
     * Starts the GeometryEditor using the selected [GeometryType].
     */
    fun startEditor(selectedGeometry: GeometryType) {
        if (!geometryEditor.isStarted.value) {
            geometryEditor.start(selectedGeometry)
            isCreateButtonEnabled.value = false
        }
    }

    /**
     * Stops the GeometryEditor and updates the identified graphic or calls [createGraphic].
     */
    fun stopEditor() {
        if (identifiedGraphic.geometry != null) {
            identifiedGraphic.geometry = geometryEditor.stop()
            identifiedGraphic.isSelected = false
        } else if (geometryEditor.isStarted.value) {
            createGraphic()
        }
        isCreateButtonEnabled.value = true
    }

    /**
     * Creates a graphic from the geometry and add it to the GraphicsOverlay.
     */
    private fun createGraphic() {
        val geometry = geometryEditor.stop()
            ?: return messageDialogVM.showMessageDialog(
                "Error!",
                "Error stopping editing session"
            )
        val graphic = Graphic(geometry)

        when (geometry) {
            is Point, is Multipoint -> graphic.symbol = GeometryEditorStyle().vertexSymbol
            is Polyline -> graphic.symbol = GeometryEditorStyle().lineSymbol
            is Polygon -> graphic.symbol = GeometryEditorStyle().fillSymbol
            else -> {}
        }
        graphicsOverlay.graphics.add(graphic)
        graphic.isSelected = false
    }

    /**
     * Deletes the selected element and stops the geometry editor if there are no
     * more elements in the geometry.
     */
    fun deleteSelection() {
        if (geometryEditor.geometry.value?.isEmpty == true) {
            geometryEditor.stop()
            isCreateButtonEnabled.value = true
        }

        val selectedElement = geometryEditor.selectedElement.value
        if (selectedElement?.canDelete == true) {
            geometryEditor.deleteSelectedElement()
        }
    }

    /**
     * Reverts the last event on the geometry editor.
     */
    fun editorUndo() {
        geometryEditor.undo()
    }

    /**
     * Identifies the graphic at the tapped screen coordinate in the provided [singleTapConfirmedEvent]
     * and starts the GeometryEditor using the identified graphic's geometry. Hide the BottomSheet on
     * [singleTapConfirmedEvent].
     */
    fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        sampleCoroutineScope.launch {
            val graphicsResult = mapViewProxy.identifyGraphicsOverlays(
                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                tolerance = 10.0.dp,
                returnPopupsOnly = false
            ).getOrNull()

            if (!geometryEditor.isStarted.value) {
                if (graphicsResult != null) {
                    if (graphicsResult.isNotEmpty()) {
                        identifiedGraphic = graphicsResult[0].graphics[0]
                        identifiedGraphic.isSelected = true
                        identifiedGraphic.geometry?.let {
                            geometryEditor.start(it)
                            isCreateButtonEnabled.value = false
                        }
                    }
                }
                identifiedGraphic.geometry = null
            }
        }
        dismissBottomSheet()
    }
}
