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

package com.esri.arcgismaps.sample.traceutilitynetwork.components

import android.app.Application
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.httpcore.authentication.TokenCredential
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.UniqueValue
import com.arcgismaps.mapping.symbology.UniqueValueRenderer
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.utilitynetworks.UtilityDomainNetwork
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityElementTraceResult
import com.arcgismaps.utilitynetworks.UtilityNetwork
import com.arcgismaps.utilitynetworks.UtilityNetworkSourceType
import com.arcgismaps.utilitynetworks.UtilityTier
import com.arcgismaps.utilitynetworks.UtilityTraceParameters
import com.arcgismaps.utilitynetworks.UtilityTraceResult
import com.arcgismaps.utilitynetworks.UtilityTraceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TraceUtilityNetworkViewModel(application: Application) : AndroidViewModel(application) {

    // The textual hint shown to the user
    private val _hint = MutableStateFlow<String?>(null)
    val hint: Flow<String?> = _hint.asStateFlow()

    // The last element that was added (start/barrier)
    private val _lastAddedElement = MutableStateFlow<UtilityElement?>(null)
    val lastAddedElement: Flow<UtilityElement?> = _lastAddedElement.asStateFlow()

    // The last tap in screen coords + map coords
    private val _lastSingleTap = MutableStateFlow<Pair<ScreenCoordinate, Point>?>(null)
    val lastSingleTap: Flow<Pair<ScreenCoordinate, Point>?> = _lastSingleTap.asStateFlow()

    // The pending trace parameters
    private val _pendingTraceParameters = MutableStateFlow<UtilityTraceParameters?>(null)
    val pendingTraceParameters: Flow<UtilityTraceParameters?> =
        _pendingTraceParameters.asStateFlow()

    // Whether the terminal selector is open
    private val _terminalSelectorIsOpen = MutableStateFlow(false)
    val terminalSelectorIsOpen: Flow<Boolean> = _terminalSelectorIsOpen.asStateFlow()

    // Current trace “activity” state
    private val _tracingActivity = MutableStateFlow<TracingActivity>(TracingActivity.None)
    val tracingActivity: Flow<TracingActivity> = _tracingActivity

    // An ArcGISMap holding the UtilityNetwork and operational layers
    val arcGISMap = ArcGISMap(
        item = PortalItem(
            portal = Portal.arcGISOnline(connection = Portal.Connection.Authenticated),
            itemId = NAPERVILLE_ELECTRICAL_NETWORK_ITEM_ID
        )
    ).apply {
        setBasemap(Basemap(BasemapStyle.ArcGISStreetsNight))
    }


    // Used to handle map view animations
    val mapViewProxy = MapViewProxy()

    // The UtilityNetwork object, assumed to be in map.utilityNetworks[0]
    private val network: UtilityNetwork
        get() = arcGISMap.utilityNetworks[0]

    // Domain network
    private val electricDistribution: UtilityDomainNetwork?
        get() = network.definition?.getDomainNetwork("ElectricDistribution")

    // Tier
    private val mediumVoltageRadial: UtilityTier?
        get() = electricDistribution?.getTier("Medium Voltage Radial")

    // Barrier / Start points overlay
    val pointsOverlay: GraphicsOverlay by lazy {
        GraphicsOverlay().apply {
            val barrierUniqueValue = UniqueValue(
                "Barrier",
                "", // description
                SimpleMarkerSymbol(SimpleMarkerSymbolStyle.X, Color.red, 20f),
                listOf(PointType.Barrier.name)
            )

            // UniqueValueRenderer to differentiate barrier vs. start
            renderer = UniqueValueRenderer().apply {
                fieldNames.add("PointType")
                uniqueValues.add(barrierUniqueValue)
                defaultSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.green, 20f)
            }
        }
    }

    companion object {
        const val USERNAME = "viewer01"
        const val PASSWORD = "I68VGU^nMurF"
        private const val SAMPLE_SERVER_7 = "https://sampleserver7.arcgisonline.com"
        private const val SAMPLE_PORTAL_URL = "$SAMPLE_SERVER_7/portal/sharing/rest"
        private const val FEATURE_SERVICE_URL = SAMPLE_SERVER_7 +
                "/server/rest/services/UtilityNetwork/NapervilleElectric/FeatureServer"
        // The portal item ID for Naperville’s electrical network
        private const val NAPERVILLE_ELECTRICAL_NETWORK_ITEM_ID = "471eb0bf37074b1fbb972b1da70fb310"
        // Feature layer IDs relevant to this sample
        private val FEATURE_LAYER_IDS = listOf("3", "0")

    }

    /**
     * Sets up the map and utility network:
     * - Adds credentials
     * - Loads the map & network
     * - Adds relevant operational layers
     */
    init {
        viewModelScope.launch {
            // A licensed user is required to perform utility network operations
            TokenCredential.create(
                url = SAMPLE_PORTAL_URL,
                username = USERNAME,
                password = PASSWORD
            ).onSuccess { tokenCredential ->
                ArcGISEnvironment.authenticationManager.arcGISCredentialStore.add(tokenCredential)
                arcGISMap.load().onSuccess {
                    // Load the network
                    network.load().onSuccess {

                        // Once loaded, remove all operational layers
                        arcGISMap.operationalLayers.clear()

                        // Add relevant layers
                        FEATURE_LAYER_IDS.forEach { layerId ->
                            val table = ServiceFeatureTable("$FEATURE_SERVICE_URL/$layerId")
                            val layer = FeatureLayer.createWithFeatureTable(table)
                            if (layerId == "3") {
                                // Customize rendering for layer with ID 3
                                layer.renderer = UniqueValueRenderer().apply {
                                    fieldNames.add("ASSETGROUP")
                                    uniqueValues.add(
                                        UniqueValue(
                                            description = "Low voltage",
                                            label = "",
                                            symbol = SimpleLineSymbol(
                                                style = SimpleLineSymbolStyle.Dash,
                                                color = Color.cyan,
                                                width = 3f
                                            ),
                                            values = listOf(3)
                                        )
                                    )
                                    uniqueValues.add(
                                        UniqueValue(
                                            description = "Medium voltage",
                                            label = "",
                                            symbol = SimpleLineSymbol(
                                                style = SimpleLineSymbolStyle.Solid,
                                                color = Color.cyan,
                                                width = 3f
                                            ),
                                            values = listOf(5)
                                        )
                                    )
                                }
                            }
                            arcGISMap.operationalLayers.add(layer)
                        }
                    }.onFailure {
                        updateUserHint("An error occurred while loading the network: ${it.message}")
                    }
                }.onFailure {
                    updateUserHint("An error occurred while loading the network: ${it.message}")
                }
            }.onFailure {
                updateUserHint("An error occurred while loading the network: ${it.message}")
            }
        }
    }

    /**
     * Initialize the trace parameters and switch to “settingPoints” mode for .start points.
     */
    fun setTraceParameters(traceType: UtilityTraceType) {
        val params = UtilityTraceParameters(traceType, emptyList()).apply {
            // Attempt to set default config from the tier
            traceConfiguration = mediumVoltageRadial?.getDefaultTraceConfiguration()
        }
        _pendingTraceParameters.value = params
        _tracingActivity.value = TracingActivity.SettingPoints(PointType.Start)
        updateUserHint()  // triggers default “Tap on the map to add a start location.”
    }

    /**
     * Add a feature to the trace (barrier or start) based on the current tracing activity.
     */
    private fun addFeature(
        feature: ArcGISFeature,
        mapPoint: Point
    ) {
        val currentParams = _pendingTraceParameters.value
        val activity = _tracingActivity.value
        if (currentParams == null || activity !is TracingActivity.SettingPoints) return

        // Convert to a UtilityElement. This is typically done via:
        //   network.createElement(feature)
        // (ArcGIS Android differs slightly from iOS)
        val element = try {
            network.createElementOrNull(feature)
        } catch (e: Exception) {
            updateUserHint("Error adding element to the trace: ${e.message}")
            return
        }

        // For a junction, add using its geometry
        // For an edge, compute fractionAlongEdge
        when (element?.networkSource?.sourceType) {
            UtilityNetworkSourceType.Junction -> {
                addElementToPendingTrace(element, feature.geometry)
                val terminalCount = element.assetType.terminalConfiguration?.terminals?.size ?: 0
                if (terminalCount > 1) {
                    // Show terminal selector
                    _terminalSelectorIsOpen.value = true
                }

            }

            UtilityNetworkSourceType.Edge -> {
                val line = feature.geometry as Polyline
                val fraction = GeometryEngine.fractionAlong(line, mapPoint, 1.0)
                element.fractionAlongEdge = fraction
                updateUserHint("fractionAlongEdge: %.3f".format(fraction))
                addElementToPendingTrace(element, mapPoint)

            }

            null -> {

            }
        }
    }

    /**
     * Actually add the UtilityElement to our UtilityTraceParameters (start or barrier)
     * and place a graphic on the map.
     */
    private fun addElementToPendingTrace(element: UtilityElement, pointGeom: Geometry?) {
        val currentParams = _pendingTraceParameters.value
        val activity = _tracingActivity.value
        if (currentParams == null || activity !is TracingActivity.SettingPoints) return

        val graphic = Graphic(pointGeom).apply {
            attributes["PointType"] = activity.pointType.name
        }

        when (activity.pointType) {
            PointType.Barrier -> currentParams.barriers.add(element)
            PointType.Start -> currentParams.startingLocations.add(element)
        }

        pointsOverlay.graphics.add(graphic)
        _lastAddedElement.value = element
    }

    /**
     * Switch from adding .start points to adding .barrier, or vice versa.
     */
    fun setPointType(pointType: PointType) {
        val currentActivity = _tracingActivity.value
        if (currentActivity is TracingActivity.SettingPoints) {
            _tracingActivity.value = currentActivity.copy(pointType = pointType)
        }
        updateUserHint()
    }

    /**
     * Run the trace, select results in the map’s feature layers.
     */
    fun trace() {
        val params = _pendingTraceParameters.value ?: return
        viewModelScope.launch {
            try {
                _tracingActivity.value = TracingActivity.TraceRunning
                updateUserHint()

                // Perform the trace
                val traceResults: List<UtilityTraceResult> = network.trace(params).getOrElse {
                    return@launch updateUserHint("An error occurred: ${it.message}")
                }

                // Filter out element results
                val elementResults = traceResults.filterIsInstance<UtilityElementTraceResult>()

                // For each result, group by network source name
                elementResults.forEach { eResult ->
                    val groupedBySource = eResult.elements.groupBy { it.networkSource.name }

                    // For each group, find the corresponding FeatureLayer and select the features
                    groupedBySource.forEach { (sourceName, elements) ->
                        val layer = arcGISMap.operationalLayers
                            .filterIsInstance<FeatureLayer>()
                            .firstOrNull { fl -> fl.featureTable?.tableName == sourceName }
                            ?: return@forEach

                        // Convert the UtilityElements to actual Features
                        val features = network.getFeaturesForElements(elements).getOrElse {
                            return@launch updateUserHint(it.message)
                        }
                        // Select them
                        layer.selectFeatures(features)
                    }
                }

                // Mark trace as completed
                _tracingActivity.value = TracingActivity.TraceCompleted
                updateUserHint()

            } catch (e: Exception) {
                _tracingActivity.value = TracingActivity.TraceFailed(e.message ?: "Unknown error")
                updateUserHint()
            }
        }
    }

    /**
     * Resets the trace, removing graphics and clearing selections.
     */
    fun reset() {
        arcGISMap.operationalLayers
            .filterIsInstance<FeatureLayer>()
            .forEach { it.clearSelection() }
        pointsOverlay.graphics.clear()
        _pendingTraceParameters.value = null
        _tracingActivity.value = TracingActivity.None
        updateUserHint(null)
    }

    /**
     * Update the user hint text based on the current tracingActivity, or override with message.
     */
    private fun updateUserHint(message: String? = null) {
        _hint.value = message
            ?: when (val activity = _tracingActivity.value) {
                TracingActivity.None -> null
                is TracingActivity.SettingPoints -> {
                    when (activity.pointType) {
                        PointType.Start -> "Tap on the map to add a start location."
                        PointType.Barrier -> "Tap on the map to add a barrier."
                    }
                }

                TracingActivity.TraceCompleted -> "Trace completed."
                is TracingActivity.TraceFailed -> "Trace failed.\n${activity.description}"
                TracingActivity.TraceRunning -> null
            }
    }

    fun identifyFeature(tapEvent: SingleTapConfirmedEvent) {
        if (_tracingActivity.value is TracingActivity.SettingPoints) {
            viewModelScope.launch {
                val identifyResults: List<IdentifyLayerResult> =
                    mapViewProxy.identifyLayers(
                        screenCoordinate = tapEvent.screenCoordinate,
                        tolerance = 4.dp,
                        returnPopupsOnly = false,
                        maximumResults = 1
                    ).getOrThrow()
                val firstFeature =
                    identifyResults.firstOrNull()?.geoElements?.firstOrNull()
                if (firstFeature != null) {
                    (firstFeature as? ArcGISFeature)?.let { arcGISFeature ->
                        addFeature(arcGISFeature, tapEvent.mapPoint!!)
                    }
                }
            }
        }
    }
}

enum class PointType {
    Start,
    Barrier
}

sealed class TracingActivity {
    data object None : TracingActivity()
    data class SettingPoints(val pointType: PointType) : TracingActivity()
    data object TraceCompleted : TracingActivity()
    data class TraceFailed(val description: String) : TracingActivity()
    data object TraceRunning : TracingActivity()
}
