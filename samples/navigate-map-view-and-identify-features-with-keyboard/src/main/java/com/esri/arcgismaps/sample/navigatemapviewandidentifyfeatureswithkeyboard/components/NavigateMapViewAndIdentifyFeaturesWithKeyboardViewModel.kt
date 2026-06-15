/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.navigatemapviewandidentifyfeatureswithkeyboard.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.data.Feature
import com.arcgismaps.data.FeatureQueryResult
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.HorizontalAlignment
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.symbology.VerticalAlignment
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

const val RESTAURANTS_SERVICE_URL =
    "https://services2.arcgis.com/ZQgQTuoyBrtmoGdP/arcgis/rest/services/redlands_food/FeatureServer/0"
private const val NAME_ATTRIBUTE = "name"
private const val MAX_SELECTABLE_FEATURES = 9
private const val AREA_OF_INTEREST_SIZE_PIXELS = 200.0

class NavigateMapViewAndIdentifyFeaturesWithKeyboardViewModel(app: Application) :
    AndroidViewModel(app) {

    private val restaurantsFeatureTable = ServiceFeatureTable(uri = RESTAURANTS_SERVICE_URL)
    private val restaurantsLayer = FeatureLayer.createWithFeatureTable(
        featureTable = restaurantsFeatureTable
    ).apply {
        renderer = SimpleRenderer(
            SimpleMarkerSymbol(
                style = SimpleMarkerSymbolStyle.Circle,
                color = Color.restaurantMarkerFill,
                size = 12f
            ).apply {
                outline = SimpleLineSymbol(
                    style = SimpleLineSymbolStyle.Solid,
                    color = Color.white,
                    width = 1.5f
                )
            }
        )
    }

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISLightGray).apply {
        initialViewpoint = Viewpoint(Point(-117.1825, 34.0556, SpatialReference.wgs84()), 2500.0)
        operationalLayers.add(restaurantsLayer)
    }

    val mapViewProxy = MapViewProxy()
    val labelsOverlay = GraphicsOverlay()
    val selectionProperties = SelectionProperties(color = Color.selectionHalo)

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    var isOverflowMessageVisible by mutableStateOf(false)
        private set

    var calloutState by mutableStateOf<CalloutState?>(null)
        private set

    private var mapViewSize = IntSize.Zero
    private val selectableFeatures = mutableListOf<Feature>()
    private var hasCompletedInitialSelection = false

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    fun updateMapViewSize(size: IntSize) {
        mapViewSize = size
    }

    fun handleDrawStatusChanged(drawStatus: DrawStatus) {
        if (drawStatus == DrawStatus.Completed && !hasCompletedInitialSelection) {
            hasCompletedInitialSelection = true
            refreshSelection()
        }
    }

    fun handleNavigationChanged(isNavigating: Boolean) {
        if (!isNavigating && hasCompletedInitialSelection) {
            refreshSelection()
        }
    }

    fun showCalloutForFeatureIndex(index: Int) {
        if (index !in selectableFeatures.indices) return

        val feature = selectableFeatures[index]
        val anchorPoint = feature.geometry as? Point ?: return
        val wgs84Point =
            GeometryEngine.projectOrNull(anchorPoint, SpatialReference.wgs84()) ?: return
        val name = getFeatureName(feature, fallback = "Restaurant") ?: "Restaurant"
        val details = buildString {
            appendLine("Lat: ${"%.6f".format(wgs84Point.y)}")
            append("Lon: ${"%.6f".format(wgs84Point.x)}")
        }

        calloutState = CalloutState(
            location = anchorPoint,
            title = name,
            details = details
        )
    }

    fun dismissCallout() {
        calloutState = null
    }

    private fun refreshSelection() {
        if (mapViewSize == IntSize.Zero) return

        viewModelScope.launch {
            clearPreviousSelectionState()

            val areaOfInterest = buildAreaOfInterestEnvelope() ?: return@launch
            val queryParameters = QueryParameters().apply {
                geometry = areaOfInterest
                spatialRelationship = com.arcgismaps.data.SpatialRelationship.Intersects
                returnGeometry = true
            }

            val queryResult = restaurantsFeatureTable.queryFeatures(queryParameters).getOrElse {
                messageDialogVM.showMessageDialog(it)
                return@launch
            }

            val orderedFeatures = queryResult
                .mapNotNull { feature ->
                    val point = feature.geometry as? Point ?: return@mapNotNull null
                    OrderedFeature(feature = feature, point = point)
                }
                .sortedWith(
                    compareByDescending<OrderedFeature> { it.point.y }
                        .thenBy { it.point.x }
                )

            isOverflowMessageVisible = orderedFeatures.size > MAX_SELECTABLE_FEATURES

            orderedFeatures.forEachIndexed { index, orderedFeature ->
                restaurantsLayer.selectFeature(orderedFeature.feature)

                if (index >= MAX_SELECTABLE_FEATURES) return@forEachIndexed

                labelsOverlay.graphics.add(
                    Graphic(
                        geometry = orderedFeature.point,
                        symbol = createLabelSymbol(index + 1, orderedFeature.feature)
                    )
                )
                selectableFeatures.add(orderedFeature.feature)
            }
        }
    }

    private fun buildAreaOfInterestEnvelope(): Envelope? {
        val halfWidth = AREA_OF_INTEREST_SIZE_PIXELS / 2.0
        val centerX = mapViewSize.width / 2.0
        val centerY = mapViewSize.height / 2.0
        val minPoint = mapViewProxy.screenToLocationOrNull(
            ScreenCoordinate(x = centerX - halfWidth, y = centerY - halfWidth)
        )
        val maxPoint = mapViewProxy.screenToLocationOrNull(
            ScreenCoordinate(x = centerX + halfWidth, y = centerY + halfWidth)
        )

        return if (minPoint != null && maxPoint != null) {
            Envelope(minPoint, maxPoint)
        } else {
            null
        }
    }

    private fun clearPreviousSelectionState() {
        restaurantsLayer.clearSelection()
        labelsOverlay.graphics.clear()
        selectableFeatures.clear()
        isOverflowMessageVisible = false
        dismissCallout()
    }

    private fun createLabelSymbol(index: Int, feature: Feature): TextSymbol {
        val labelText =
            getFeatureName(feature, fallback = null)?.let { "$index: $it" } ?: index.toString()
        return TextSymbol(
            text = labelText,
            color = Color.labelText,
            size = 15f,
            horizontalAlignment = HorizontalAlignment.Center,
            verticalAlignment = VerticalAlignment.Top
        ).apply {
            haloColor = Color.white
            haloWidth = 2f
            offsetY = -14f
        }
    }

    private fun getFeatureName(feature: Feature, fallback: String?): String? {
        val value = feature.attributes[NAME_ATTRIBUTE] as? String
        return if (!value.isNullOrBlank()) value else fallback
    }
}

data class CalloutState(
    val location: Point,
    val title: String,
    val details: String
)

private data class OrderedFeature(
    val feature: Feature,
    val point: Point
)

private val Color.Companion.restaurantMarkerFill: Color
    get() = fromRgba(11, 79, 138, 255)

private val Color.Companion.selectionHalo: Color
    get() = fromRgba(190, 24, 93, 255)

private val Color.Companion.labelText: Color
    get() = fromRgba(31, 35, 40, 255)
