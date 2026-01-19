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

    val showLoadingDialog = mutableStateOf(true)

    private var buildingSceneLayer: BuildingSceneLayer? = null

    var selectedFloor by mutableStateOf("All")
    val floors: MutableList<String> = mutableListOf(selectedFloor)

    val categories: MutableList<BuildingSublayer> = mutableListOf()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // LocalSceneViewProxy enables identify operations from the ViewModel.
    val localSceneViewProxy = LocalSceneViewProxy()

    private var _popupState = MutableStateFlow<PopupState?>(null)
    val popupState = _popupState.asStateFlow()

    private var _identifyState = MutableStateFlow(false)
    val identifyState = _identifyState.debounce(1000)

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
     * Utility function to update the building filters based on the selected floor
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

    fun identify(tapPoint: DoubleXY) {
        _identifyState.value = true

        sublayerWithSelection?.clearSelection()

        viewModelScope.launch {
            localSceneViewProxy.identify(
                layer = buildingSceneLayer!!,
                screenCoordinate = tapPoint,
                tolerance = 12.dp,
                returnPopupsOnly = false,
                maximumResults = 1
            ).onSuccess {
                _identifyState.value = false

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
                _identifyState.value = false
            }
        }
    }

    fun dismissPopup() {
        _popupState.value = null
    }
}
