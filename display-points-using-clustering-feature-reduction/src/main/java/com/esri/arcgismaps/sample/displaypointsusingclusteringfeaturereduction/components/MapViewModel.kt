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
import android.util.Log
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.popup.TextPopupElement
import com.arcgismaps.mapping.view.Callout
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.portal.Portal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(private val application: Application,
                   private val sampleCoroutineScope: CoroutineScope)
    : AndroidViewModel(application) {
    // set the MapView mutable stateflow
    val mapViewState = MutableStateFlow(MapViewState())


    // Flag indicating whether feature reduction is enabled or not
    private val _isEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    // Formatted content of popups
    private val _popupText: MutableStateFlow<String> = MutableStateFlow("")
    val popupText: StateFlow<String> = _popupText.asStateFlow()

    init {
        // load the portal and create a map from the portal item
        val portalItem = PortalItem(
            Portal("https://www.arcgis.com/"),
            "8916d50c44c746c1aafae001552bad23"
        )

        // set the map to be displayed in the layout's MapView
        mapViewState.value.arcGISMap = ArcGISMap(portalItem)
    }

    fun toggleFeatureReduction() {
        val map = mapViewState.value.arcGISMap
        _isEnabled.value = !_isEnabled.value
        if (map.loadStatus.value == LoadStatus.Loaded) {
            map.operationalLayers.forEach { layer ->
                when (layer) {
                    is FeatureLayer -> {
                        layer.featureReduction?.isEnabled = _isEnabled.value
                    }
                    else -> { }
                }
            }
        }
    }

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
                                    is TextPopupElement -> {
                                        popupOutput.appendLine("\n ${HtmlCompat.fromHtml(popupElement.text, HtmlCompat.FROM_HTML_OPTION_USE_CSS_COLORS)}")
                                    }
                                    else -> {}
                                }
                            }

                        }.onFailure {

                        }
                    }
                }
                _popupText.value = popupOutput.toString()
            }.onFailure {

            }
        }
    }

}


/**
 * Data class that represents the MapView state
 */
class MapViewState( // This would change based on each sample implementation
    var arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight),
    var viewpoint: Viewpoint = Viewpoint(39.8, -98.6, 10e7))

