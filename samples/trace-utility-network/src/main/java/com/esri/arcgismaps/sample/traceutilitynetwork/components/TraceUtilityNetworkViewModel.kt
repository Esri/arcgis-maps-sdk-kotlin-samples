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
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TraceUtilityNetworkViewModel(application: Application) : AndroidViewModel(application) {

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // The textual hint shown to the user
    private val _hint = MutableStateFlow<String?>(null)
    val hint = _hint.asStateFlow()

    // The pending trace parameters
    private val _pendingTraceParameters = MutableStateFlow<UtilityTraceParameters?>(null)
    val pendingTraceParameters = _pendingTraceParameters.asStateFlow()

    // Whether the terminal selector is open
    private val _terminalSelectorIsOpen = MutableStateFlow(false)
    val terminalSelectorIsOpen = _terminalSelectorIsOpen.asStateFlow()

    // Current trace state
    private val _traceState = MutableStateFlow<TraceState>(TraceState.None)
    val traceState = _traceState

    // Is trace utility network enabled
    private val _canTrace = MutableStateFlow(false)
    val canTrace = _canTrace.asStateFlow()

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
    val pointsOverlay = GraphicsOverlay().apply {
        val barrierUniqueValue = UniqueValue(
            description = "Barrier",
            label = "",
            symbol = SimpleMarkerSymbol(
                style = SimpleMarkerSymbolStyle.X,
                color = Color.red,
                size = 20f
            ),
            values = listOf(PointType.Barrier.name)
        )

        // UniqueValueRenderer to differentiate barrier vs. start
        renderer = UniqueValueRenderer(
            fieldNames = listOf("PointType"),
            uniqueValues = listOf(barrierUniqueValue),
            defaultSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.green, 20f)
        )
    }

    companion object {
        const val USERNAME = "viewer01"
        const val PASSWORD = "I68VGU^nMurF"

        // The portal item ID for Naperville’s electrical network
        private const val NAPERVILLE_ELECTRICAL_NETWORK_ITEM_ID = "471eb0bf37074b1fbb972b1da70fb310"

        // Feature layer IDs relevant to this sample
        private val FEATURE_LAYER_IDS = listOf("3", "0")
        private const val SAMPLE_SERVER_7 = "https://sampleserver7.arcgisonline.com"
        private const val SAMPLE_PORTAL_URL = "$SAMPLE_SERVER_7/portal/sharing/rest"
        private const val FEATURE_SERVICE_URL =
            "$SAMPLE_SERVER_7/server/rest/services/UtilityNetwork/NapervilleElectric/FeatureServer"
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
                // load the NapervilleElectric web-map
                arcGISMap.load().getOrElse {
                    messageDialogVM.showMessageDialog(
                        title = "Error loading the web-map: ${it.message}",
                        description = it.cause.toString()
                    )
                }
                // load the utility network associated with the web-map
                network.load().getOrElse {
                    messageDialogVM.showMessageDialog(
                        title = "Error loading the utility network: ${it.message}",
                        description = it.cause.toString()
                    )
                }

                // Once loaded, remove all operational layers
                arcGISMap.operationalLayers.clear()

                // Add relevant layers
                FEATURE_LAYER_IDS.forEach { layerId ->
                    val table = ServiceFeatureTable("$FEATURE_SERVICE_URL/$layerId")
                    val layer = FeatureLayer.createWithFeatureTable(table)
                    if (layerId == "3") {
                        // Customize rendering for layer with ID 3
                        layer.renderer = UniqueValueRenderer(
                            fieldNames = listOf("ASSETGROUP"),
                            uniqueValues = listOf(
                                UniqueValue(
                                    description = "Low voltage",
                                    label = "",
                                    symbol = SimpleLineSymbol(
                                        style = SimpleLineSymbolStyle.Dash,
                                        color = Color.cyan,
                                        width = 3f
                                    ),
                                    values = listOf(3)
                                ),
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
                        )
                    }
                    // Add the two feature layers to the map's operational layers.
                    arcGISMap.operationalLayers.add(layer)
                }
            }.onFailure {
                messageDialogVM.showMessageDialog(
                    title = "Error using TokenCredential: ${it.message}",
                    description = it.cause.toString()
                )
            }
        }
    }

    /**
     * Initialize the trace parameters and switch to “settingPoints” mode for .start points.
     */
    fun setTraceParameters(traceType: UtilityTraceType) {
        val params = UtilityTraceParameters(
            traceType = traceType,
            startingLocations = emptyList()
        ).apply {
            // Attempt to set default config from the tier
            traceConfiguration = mediumVoltageRadial?.getDefaultTraceConfiguration()
        }
        _pendingTraceParameters.value = params
        _traceState.value = TraceState.SettingPoints(PointType.Start)
        updateUserHint()  // triggers default “Tap on the map to add a start location.”
    }

    /**
     * Add a feature to the trace (barrier or start) based on the current tracing state.
     */
    private fun addFeature(
        feature: ArcGISFeature,
        mapPoint: Point
    ) {
        val currentParams = _pendingTraceParameters.value
        val state = _traceState.value
        if (currentParams == null || state !is TraceState.SettingPoints) return

        // Create a UtilityElement of the selected feature
        val element = try {
            network.createElementOrNull(feature)
        } catch (e: Exception) {
            return messageDialogVM.showMessageDialog(
                title = "Error creating UtilityElement: ${e.message}",
                description = e.cause.toString()
            )
        }

        // For a junction, add using its geometry
        // For an edge, compute fractionAlongEdge
        when (element?.networkSource?.sourceType) {
            UtilityNetworkSourceType.Junction -> {
                val terminalCount = element.assetType.terminalConfiguration?.terminals?.size ?: 0
                if (terminalCount > 1) {
                    // Show terminal selector
                    _terminalSelectorIsOpen.value = true
                }
                addElementToPendingTrace(
                    element = element,
                    pointGeometry = feature.geometry,
                    pointType = state.pointType
                )
            }

            UtilityNetworkSourceType.Edge -> {
                val line = feature.geometry as Polyline
                val fraction = GeometryEngine.fractionAlong(
                    line = line,
                    point = mapPoint,
                    tolerance = 1.0
                )
                element.fractionAlongEdge = fraction
                updateUserHint("fractionAlongEdge: %.3f".format(fraction))
                addElementToPendingTrace(
                    element = element,
                    pointGeometry = mapPoint,
                    pointType = state.pointType
                )
            }

            null -> {}
        }
    }

    /**
     * Actually add the UtilityElement to our UtilityTraceParameters (start or barrier)
     * and place a graphic on the map.
     */
    private fun addElementToPendingTrace(
        element: UtilityElement,
        pointGeometry: Geometry?,
        pointType: PointType
    ) {
        val graphic = Graphic(pointGeometry).apply {
            attributes["PointType"] = pointType.name
        }
        pointsOverlay.graphics.add(graphic)
        when (pointType) {
            PointType.Barrier -> _pendingTraceParameters.value?.barriers?.add(element)
            PointType.Start -> {
                _pendingTraceParameters.value?.startingLocations?.add(element)
                _canTrace.value = true
            }
        }
    }

    /**
     * Switch from adding .start points to adding .barrier, or vice versa.
     */
    fun setPointType(pointType: PointType) {
        val currentTraceState = _traceState.value
        if (currentTraceState is TraceState.SettingPoints) {
            _traceState.value = currentTraceState.copy(pointType = pointType)
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
                _traceState.value = TraceState.TraceRunning
                updateUserHint()

                // Perform the trace
                val traceResults: List<UtilityTraceResult> = network.trace(params).getOrElse {
                    return@launch messageDialogVM.showMessageDialog(
                        title = "Error performing trace: ${it.message}",
                        description = it.cause.toString()
                    )
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
                _traceState.value = TraceState.TraceCompleted
                updateUserHint()

            } catch (e: Exception) {
                _traceState.value = TraceState.TraceFailed(e.message ?: "Unknown error")
                messageDialogVM.showMessageDialog(
                    title = "Error performing trace: ${e.message}",
                    description = e.cause.toString()
                )
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
        _traceState.value = TraceState.None
        _canTrace.value = false
        updateUserHint(null)
    }

    /**
     * Update the user hint text based on the current TraceState, or override with message.
     */
    private fun updateUserHint(message: String? = null) {
        _hint.value = message
            ?: when (val state = _traceState.value) {
                TraceState.None -> null
                is TraceState.SettingPoints -> {
                    when (state.pointType) {
                        PointType.Start -> "Tap on the map to add a start location."
                        PointType.Barrier -> "Tap on the map to add a barrier."
                    }
                }

                TraceState.TraceCompleted -> "Trace completed."
                is TraceState.TraceFailed -> "Trace failed.\n${state.description}"
                TraceState.TraceRunning -> null
            }
    }

    fun identifyFeature(tapEvent: SingleTapConfirmedEvent) {
        if (_traceState.value is TraceState.SettingPoints) {
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
                        addFeature(
                            feature = arcGISFeature,
                            mapPoint = tapEvent.mapPoint!!
                        )
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

sealed class TraceState {
    data object None : TraceState()
    data object TraceRunning : TraceState()
    data object TraceCompleted : TraceState()
    data class SettingPoints(val pointType: PointType) : TraceState()
    data class TraceFailed(val description: String) : TraceState()
}
