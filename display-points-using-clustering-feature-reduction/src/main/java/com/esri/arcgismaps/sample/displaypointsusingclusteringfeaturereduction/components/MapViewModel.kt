/* Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.displaypointsusingclusteringfeaturereduction.components

import android.app.Application
import android.text.Html
import androidx.compose.runtime.mutableStateOf
import androidx.core.text.HtmlCompat
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.popup.FieldsPopupElement
import com.arcgismaps.mapping.popup.TextPopupElement
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.portal.Portal
import com.esri.arcgismaps.sample.displaypointsusingclusteringfeaturereduction.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val application: Application,
    private val sampleCoroutineScope: CoroutineScope
) : AndroidViewModel(application) {
    // set the MapView mutable stateflow
    val mapViewState = MutableStateFlow(MapViewState())

    // create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    // Flag indicating whether feature reduction is enabled or not
    val isFeatureReductionEnabled = mutableStateOf(true)

    // Flag indicating whether to show the loading dialog
    val showLoadingDialog = mutableStateOf(false)

    init {
        // show loading dialog to indicate that the map is loading
        showLoadingDialog.value = true
        // load the portal and create a map from the portal item
        val portalItem = PortalItem(
            Portal(application.getString(R.string.portal_url)),
            "8916d50c44c746c1aafae001552bad23"
        )

        // set the map to be displayed in the layout's MapView
        mapViewState.value.arcGISMap = ArcGISMap(portalItem)

        sampleCoroutineScope.launch {
            mapViewState.value.arcGISMap.load().onSuccess {
                showLoadingDialog.value = false
            }
        }
    }

    /**
    `* Toggle the FeatureLayer's featureReduction property
     */
    fun toggleFeatureReduction() {
        val map = mapViewState.value.arcGISMap
        isFeatureReductionEnabled.value = !isFeatureReductionEnabled.value
        if (map.loadStatus.value == LoadStatus.Loaded) {
            map.operationalLayers.forEach { layer ->
                when (layer) {
                    is FeatureLayer -> {
                        layer.featureReduction?.isEnabled = isFeatureReductionEnabled.value
                    }

                    else -> {}
                }
            }
        }
    }

    /**
     * Identify the feature layer results to fetch and display details for each popup element
     */
    fun handleIdentifyResult(result: Result<List<IdentifyLayerResult>>) {
        sampleCoroutineScope.launch {
            result.onSuccess { identifyResultList ->
                val popupOutput = StringBuilder()
                identifyResultList.forEach { identifyLayerResult ->
                    val popups = identifyLayerResult.popups
                    popups.forEach { popup ->
                        popupOutput.appendLine(popup.title)
                        popup.evaluateExpressions().onSuccess {
                            popup.evaluatedElements.forEach { popupElement ->
                                when (popupElement) {
                                    is FieldsPopupElement -> {
                                        popupElement.fields.forEach { popupField ->
                                            popupOutput.appendLine(
                                                "\n${Html.fromHtml(popupField.label, HtmlCompat.FROM_HTML_MODE_LEGACY)}: ${popup.getFormattedValue(popupField)}"
                                            )
                                        }
                                    }
                                    is TextPopupElement -> {
                                        popupOutput.appendLine(
                                            "\n${Html.fromHtml(popupElement.text, HtmlCompat.FROM_HTML_MODE_LEGACY)}"
                                        )
                                    }
                                    else -> {
                                        popupOutput.appendLine("Unsupported popup element: ${popupElement.javaClass.name}")
                                    }
                                }
                            }
                        }.onFailure { error ->
                            messageDialogVM.showMessageDialog(
                                title = "Error in evaluating popup expression: ${error.message.toString()}",
                                description = error.cause.toString()
                            )
                        }
                    }
                }
                if (popupOutput.toString().isNotEmpty()) {
                    messageDialogVM.showMessageDialog(
                        title = "Popup Details",
                        description = popupOutput.toString()
                    )
                }
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Error in identify: ${error.message.toString()}",
                    description = error.cause.toString()
                )
            }
        }
    }
}

/**
 * Class that represents the MapView state
 */
data class MapViewState(
    var arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight),
    val viewpoint: Viewpoint = Viewpoint(39.8, -98.6, 10e7)
)

