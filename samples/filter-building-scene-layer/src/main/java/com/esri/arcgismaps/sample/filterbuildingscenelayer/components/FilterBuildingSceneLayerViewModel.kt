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

package com.esri.arcgismaps.sample.filterbuildingscenelayer.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.Feature
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.layers.BuildingSceneLayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingComponentSublayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingFilter
import com.arcgismaps.mapping.layers.buildingscene.BuildingFilterBlock
import com.arcgismaps.mapping.layers.buildingscene.BuildingGroupSublayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingSolidFilterMode
import com.arcgismaps.mapping.layers.buildingscene.BuildingSublayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingXrayFilterMode
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.toolkit.geoviewcompose.LocalSceneViewProxy
import com.arcgismaps.toolkit.popup.PopupState
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class FilterBuildingSceneLayerViewModel(app: Application) : AndroidViewModel(app) {
    val scene = ArcGISScene("https://www.arcgis.com/home/item.html?id=b7c387d599a84a50aafaece5ca139d44")

    // State to control if a loading progress indicator is shown
    val showLoadingDialog = mutableStateOf(true)

    // Building scene layer that will be filtered. Set after the WebScene is loaded.
    private var buildingSceneLayer: BuildingSceneLayer? = null

    // The selected floor
    var selectedFloor by mutableStateOf("All")

    // The list of available floors
    val floors: MutableList<String> = mutableListOf(selectedFloor)

    // The list of building sublayer categories
    val categories: MutableList<BuildingSublayer> = mutableListOf()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // LocalSceneViewProxy enables identify operations from the ViewModel.
    val localSceneViewProxy = LocalSceneViewProxy()

    // State that will contain a popup state for an identify result
    private var _popupState = MutableStateFlow<PopupState?>(null)
    val popupState = _popupState.asStateFlow()

    // State to control if a progress indicator is shown while identifying
    private var _showIdentifyProgressState = MutableStateFlow(false)
    // Use a debounce so progress indication is only shown for a slow identify
    val showIdentifyProgressState = _showIdentifyProgressState.debounce(1000)

    // Building scene layer sublayer that contains the currently selected feature
    private var sublayerWithSelection : BuildingComponentSublayer? = null

    init {
        viewModelScope.launch {
            scene.load().onFailure {
                messageDialogVM.showMessageDialog(it)
                showLoadingDialog.value = false
            }.onSuccess {
                showLoadingDialog.value = false

                buildingSceneLayer =
                    scene.operationalLayers.first { layer ->
                        layer is BuildingSceneLayer
                    } as BuildingSceneLayer

                buildingSceneLayer?.let { buildingSceneLayer ->
                    // Get the floor listing from the statistics
                    buildingSceneLayer.fetchStatistics().onSuccess { statistics ->
                        statistics["BldgLevel"]?.mostFrequentValues?.let {
                            floors.addAll(0, it.sorted())
                        }

                        // The top-level sublayer groups will be the categories
                        buildingSceneLayer.sublayers.find { sublayer ->
                            sublayer.name == "Full Model"
                        }?.let { buildingSublayer ->
                            buildingSublayer as BuildingGroupSublayer
                            categories.addAll(buildingSublayer.sublayers.sortedBy { it.name })
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates the building filters based on the selected floor
     */
    fun selectFloor(index: Int) {
        selectedFloor = floors[index]

        buildingSceneLayer?.let { buildingSceneLayer ->
            if (selectedFloor == "All") {
                // No filtering applied if 'All' floors are selected
                buildingSceneLayer.activeFilter = null
                return
            }
            // Build a building filter to show the selected floor and an xray view of the floors below.
            // Floors above the selected floor are not shown at all.
            val buildingFilter = BuildingFilter(
                name = "Floor filter",
                description = "Show selected floor and xray filter for lower floors.",
                listOf(
                    BuildingFilterBlock(
                        title = "solid block",
                        whereClause = "BldgLevel = $selectedFloor",
                        mode = BuildingSolidFilterMode()
                    ),
                    BuildingFilterBlock(
                        title = "x ray block",
                        whereClause = "BldgLevel < $selectedFloor",
                        mode = BuildingXrayFilterMode()
                    )
                )
            )
            buildingSceneLayer.activeFilter = buildingFilter
        }
    }

    /**
     * Does an identify on the building scene layer
     */
    fun identify(tapPoint: DoubleXY) {
        _showIdentifyProgressState.value = true

        sublayerWithSelection?.clearSelection()

        viewModelScope.launch {
            localSceneViewProxy.identify(
                layer = buildingSceneLayer!!,
                screenCoordinate = tapPoint,
                tolerance = 12.dp,
                returnPopupsOnly = false,
                maximumResults = 1
            ).onSuccess {
                _showIdentifyProgressState.value = false

                val results = it.sublayerResults

                if (results.isNotEmpty()) {
                    val element = results.first().geoElements.first()
                    val popup = Popup(element)
                    _popupState.value = PopupState(popup, viewModelScope)

                    val sublayer =
                        results.first().layerContent as BuildingComponentSublayer
                    sublayer.selectFeature(element as Feature)
                    sublayerWithSelection = sublayer
                }
            }.onFailure {
                _showIdentifyProgressState.value = false
            }
        }
    }

    /**
     * Dismisses any displayed popup
     */
    fun dismissPopup() {
        _popupState.value = null
    }
}
