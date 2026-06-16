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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.data.Feature
import com.arcgismaps.data.QueryFeatureFields
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.data.SpatialRelationship
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
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Fixed size for the area of interest used to identify features around the center of the screen.
const val AREA_OF_INTEREST_SIZE = 400F

class NavigateMapViewAndIdentifyFeaturesWithKeyboardViewModel(app: Application) :
    AndroidViewModel(app) {

    // Redlands restaurants service feature table.
    private val restaurantsFeatureTable = ServiceFeatureTable(
        uri = "https://services2.arcgis.com/ZQgQTuoyBrtmoGdP/arcgis/rest/services/redlands_food/FeatureServer/0"
    )

    // Feature layer to display the restaurants from feature table.
    private val restaurantsLayer = FeatureLayer.createWithFeatureTable(
        featureTable = restaurantsFeatureTable
    ).apply {
        // Symbolize each restaurant as a filled circle with a white outline.
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

    // Create a light gray basemap centered on Redlands using the restaurants layer.
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISLightGray).apply {
        initialViewpoint = Viewpoint(
            center = Point(
                x = -117.1825,
                y = 34.0556,
                spatialReference = SpatialReference.wgs84()
            ),
            scale = 2500.0
        )
        operationalLayers.add(restaurantsLayer)
    }

    // Create a MapViewProxy to perform identify and screen to location operations.
    val mapViewProxy = MapViewProxy()

    // Overlay for the numbered 1-9 labels corresponding to the selected features.
    val labelsOverlay = GraphicsOverlay()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // StateFlow to track the draw status of the MapView.
    private val _mapViewDrawStatus = MutableStateFlow<DrawStatus>(DrawStatus.InProgress)
    val mapViewDrawStatus = _mapViewDrawStatus.asStateFlow()

    // Show the overflow message when there are more than nine features identified.
    var isOverflowMessageVisible by mutableStateOf(false)
        private set

    // Index of the currently selected feature in the selectableFeatures list or null if no feature is selected.
    var selectedFeatureIndex by mutableStateOf<Int?>(null)
        private set
    private val selectableFeatures = mutableStateListOf<OrderedFeature>()

    // Limit the number of selectable features 9 for keyboard navigation (1-9).
    private val maxSelectableFeatures = 9

    // Expose the list of features that can be selected for callout display.
    val orderedFeatures: List<OrderedFeature> get() = selectableFeatures

    // Track the size of the MapView to build the area of interest envelope.
    private var mapViewSize = IntSize.Zero

    // Job for refreshing the identify job to ensure only one job is happening at a time.
    private var identifyFeaturesJob: Job? = null

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
        viewModelScope.launch {
            mapViewDrawStatus.first { it == DrawStatus.Completed }
            identifyFeatures()
        }
    }

    /**
     * Update the size of the MapView, used to build the area of interest.
     */
    fun updateMapViewSize(size: IntSize) {
        val wasMapViewUnmeasured = mapViewSize == IntSize.Zero
        mapViewSize = size
        if (
            wasMapViewUnmeasured &&
            size != IntSize.Zero &&
            _mapViewDrawStatus.value == DrawStatus.Completed
        ) {
            identifyFeatures()
        }
    }

    /**
     * Handle changes to the MapView's draw status.
     */
    fun handleDrawStatusChanged(drawStatus: DrawStatus) {
        _mapViewDrawStatus.value = drawStatus
    }

    /**
     * Handle changes to the MapView's navigation status, when navigation stops refresh identified features.
     */
    fun handleNavigationChanged(isNavigating: Boolean) {
        if (!isNavigating) {
            identifyFeatures()
        }
    }

    /**
     * Show a callout for the feature from selectable features list.
     */
    fun showCalloutForFeatureIndex(index: Int) {
        if (index in selectableFeatures.indices) {
            selectedFeatureIndex = index
        }
    }

    /**
     * Dismiss the currently shown callout.
     */
    fun dismissCallout() {
        selectedFeatureIndex = null
    }

    /**
     * Identify features that intersect with the envelope,
     * to update selection and labels for identified features,
     * then update the list of selectable features for callout display.
     */
    private fun identifyFeatures() {
        val previousIdentifyFeaturesJob = identifyFeaturesJob
        identifyFeaturesJob = viewModelScope.launch {
            // Cancel any ongoing identify job.
            previousIdentifyFeaturesJob?.cancelAndJoin()

            // Retrieve the area of interest envelope centered on the screen.
            val areaOfInterest = buildAreaOfInterestEnvelope() ?: return@launch

            // Resets the previous selection state.
            clearPreviousSelectionState()

            // Query for features that intersect with envelope.
            val queryParameters = QueryParameters().apply {
                geometry = GeometryEngine.normalizeCentralMeridian(areaOfInterest)
                spatialRelationship = SpatialRelationship.Intersects
                returnGeometry = true
            }
            val queryResult = restaurantsFeatureTable.queryFeatures(
                parameters = queryParameters,
                queryFeatureFields = QueryFeatureFields.LoadAll
            ).getOrElse {
                messageDialogVM.showMessageDialog(it)
                return@launch
            }

            // Order features by their screen position relative to the center of the screen
            val orderedFeatures = queryResult
                .mapNotNull { feature ->
                    val point = feature.geometry as? Point ?: return@mapNotNull null
                    val screenCoordinate =
                        mapViewProxy.locationToScreenOrNull(point) ?: return@mapNotNull null
                    OrderedFeature(
                        feature = feature,
                        point = point,
                        name = getFeatureName(feature = feature),
                        screenCoordinate = screenCoordinate
                    )
                }
                .sortedWith(
                    compareBy<OrderedFeature> { it.screenCoordinate.y }
                        .thenBy { it.screenCoordinate.x }
                )

            // Update state if there are more features than the maximum selectable features.
            isOverflowMessageVisible = orderedFeatures.size > maxSelectableFeatures

            // Update states of the selectable list, selects features, and add labels.
            orderedFeatures.forEachIndexed { index, orderedFeature ->
                restaurantsLayer.selectFeature(orderedFeature.feature)
                if (index >= maxSelectableFeatures) return@forEachIndexed
                labelsOverlay.graphics.add(
                    Graphic(
                        geometry = orderedFeature.point,
                        symbol = createLabelSymbol(index + 1, orderedFeature)
                    )
                )
                selectableFeatures.add(orderedFeature)
            }
        }
    }

    /**
     * Build a fixed size envelope centered on the screen to be used as the area of interest for identifying features.
     */
    private fun buildAreaOfInterestEnvelope(): Envelope? {
        val halfWidth = AREA_OF_INTEREST_SIZE / 2.0
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

    /**
     * Resets previous selection states for new identify operations.
     */
    private fun clearPreviousSelectionState() {
        restaurantsLayer.clearSelection()
        labelsOverlay.graphics.clear()
        selectableFeatures.clear()
        isOverflowMessageVisible = false
        dismissCallout()
    }

    /**
     * Create a text symbol for labeling identified features with their index and name.
     */
    private fun createLabelSymbol(index: Int, orderedFeature: OrderedFeature): TextSymbol {
        val labelText = orderedFeature.name?.let { "$index: $it" } ?: index.toString()
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

    /**
     * Get the name of the feature from its attributes.
     */
    private fun getFeatureName(
        feature: Feature,
        fallbackName: String? = null
    ): String? {
        val featureName = feature.attributes.entries
            .firstOrNull { (key, _) -> key.equals("name", ignoreCase = true) }
            ?.value
            ?.toString()
            ?.trim()
        return featureName?.takeIf { it.isNotBlank() } ?: fallbackName
    }
}

data class OrderedFeature(
    val feature: Feature,
    val point: Point,
    val name: String?,
    val screenCoordinate: ScreenCoordinate
)

private val Color.Companion.restaurantMarkerFill: Color
    get() = fromRgba(11, 79, 138, 255)

private val Color.Companion.labelText: Color
    get() = fromRgba(31, 35, 40, 255)

internal val Color.Companion.selectionHalo: Color
    get() = fromRgba(190, 24, 93, 255)
