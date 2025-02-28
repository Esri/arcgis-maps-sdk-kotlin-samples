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

package com.esri.arcgismaps.sample.showpopup.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.Feature
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class ShowPopupViewModel(application: Application) : AndroidViewModel(application) {

    val mapViewProxy = MapViewProxy()

    // Create a portal and load a map with information about mountains in the Sierra Nevada
    val portal = Portal("https://arcgis.com/")
    val portalItem = PortalItem(portal, "9f3a674e998f461580006e626611f9ad")
    val arcGISMap = ArcGISMap(portalItem)

    // Get the first visible feature layer with a popup definition
    private val featureLayer: FeatureLayer
        get() {
            return arcGISMap.operationalLayers.filterIsInstance<FeatureLayer>().first { featureLayer ->
                (featureLayer.featureTable?.geometryType == GeometryType.Point)
                    .and(featureLayer.isVisible)
                    .and(featureLayer.isPopupEnabled)
                    .and(featureLayer.popupDefinition != null)
            }
        }

    // Popup that gets passed to the main screen composable
    var popup: Popup? by mutableStateOf(null)
        private set

    // Keep track of the identified feature
    private var identifiedFeature: Feature? = null

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map",
                    error.message.toString()
                )
            }
        }
    }

    /**
     * Identify the feature at the given screen coordinate, select the feature, and show the popup for it.
     */
    fun identifyForPopup(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        viewModelScope.launch {
            mapViewProxy.identify(
                layer = featureLayer,
                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                tolerance = 12.dp,
                returnPopupsOnly = true
            ).onSuccess { result ->
                popup = result.popups.first().also { popup ->
                    identifiedFeature = (popup.geoElement as Feature).also { identifiedFeature ->
                        featureLayer.selectFeature(identifiedFeature)
                    }
                }
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to identify: ${error.message}",
                    error.message.toString()
                )
            }
        }
    }

    /**
     * Dismiss the popup and unselect the identified feature.
     */
    fun onDismissRequest() {
        popup = null
        identifiedFeature?.let { featureLayer.unselectFeature(it) }
    }
}
