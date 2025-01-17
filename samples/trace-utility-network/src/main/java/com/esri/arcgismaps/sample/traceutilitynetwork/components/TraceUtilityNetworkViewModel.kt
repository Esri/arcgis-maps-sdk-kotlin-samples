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
import com.arcgismaps.data.QueryParameters
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
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.SelectionMode
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
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.utilitynetworks.UtilityDomainNetwork
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityElementTraceResult
import com.arcgismaps.utilitynetworks.UtilityNetwork
import com.arcgismaps.utilitynetworks.UtilityNetworkSource
import com.arcgismaps.utilitynetworks.UtilityNetworkSourceType
import com.arcgismaps.utilitynetworks.UtilityTerminal
import com.arcgismaps.utilitynetworks.UtilityTier
import com.arcgismaps.utilitynetworks.UtilityTraceParameters
import com.arcgismaps.utilitynetworks.UtilityTraceType
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TraceUtilityNetworkViewModel(application: Application) : AndroidViewModel(application) {

    // The textual hint shown to the user
    private val _hint = MutableStateFlow<String?>(null)
    val hint = _hint.asStateFlow()

    // Is trace utility network enabled
    private val _canTrace = MutableStateFlow(false)
    val canTrace = _canTrace.asStateFlow()

    // The trace state used for the sample
    private val _traceState = MutableStateFlow(TraceState.ADD_STARTING_POINT)
    val traceState = _traceState.asStateFlow()

    // Currently selected utility trace type
    private val _selectedTraceType = MutableStateFlow<UtilityTraceType>(UtilityTraceType.Connected)
    val selectedTraceType = _selectedTraceType.asStateFlow()

    // Currently selected point type (start/barrier)
    private val _selectedPointType = MutableStateFlow(PointType.Start)
    val selectedPointType = _selectedPointType.asStateFlow()

    // Terminal configuration options (high/low)
    private val _terminalConfigurationOptions = MutableStateFlow<List<UtilityTerminal>>(listOf())
    val terminalConfigurationOptions = _terminalConfigurationOptions.asStateFlow()

    // Currently selected terminal configuration
    private var _selectedTerminalConfigurationIndex = MutableStateFlow<Int?>(null)

    // ArcGISMap holding the UtilityNetwork and operational layers
    val arcGISMap = ArcGISMap(
        item = PortalItem(
            portal = Portal.arcGISOnline(connection = Portal.Connection.Authenticated),
            itemId = NAPERVILLE_ELECTRICAL_NETWORK_ITEM_ID
        )
    ).apply {
        // Add the map with streets night vector basemap
        setBasemap(Basemap(BasemapStyle.ArcGISStreetsNight))
    }

    // Used to handle map view animations
    val mapViewProxy = MapViewProxy()

    // The utility network used for tracing.
    private val utilityNetwork: UtilityNetwork
        get() = arcGISMap.utilityNetworks.first()

    // Use the ElectricDistribution domain network
    private val electricDistribution: UtilityDomainNetwork?
        get() = utilityNetwork.definition?.getDomainNetwork("ElectricDistribution")

    // Use the Medium Voltage Tier
    private val mediumVoltageTier: UtilityTier?
        get() = electricDistribution?.getTier("Medium Voltage Radial")

    // Create lists for starting locations and barriers
    private val utilityElementStartingLocations: MutableList<UtilityElement> = mutableListOf()
    private val utilityElementBarriers: MutableList<UtilityElement> = mutableListOf()

    // Graphics overlay for the starting locations and barrier graphics
    val graphicsOverlay = GraphicsOverlay()

    // Create symbols for the starting point and barriers
    private val startingPointSymbol = SimpleMarkerSymbol(
        style = SimpleMarkerSymbolStyle.Cross,
        color = Color.green,
        size = 25f
    )
    private val barrierPointSymbol = SimpleMarkerSymbol(
        style = SimpleMarkerSymbolStyle.X,
        color = Color.red,
        size = 25f
    )

    // Add custom unique renderer values for the electrical distribution layer
    private val electricalDistributionUniqueValueRenderer = UniqueValueRenderer(
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

    /**
     * Initializes view model by adding credentials, loading map and utility network,
     * and electrical device and distribution feature layers.
     */
    suspend fun initializeTrace() {
        // A licensed user is required to perform utility network operations
        TokenCredential.create(
            url = SAMPLE_PORTAL_URL,
            username = USERNAME,
            password = PASSWORD
        ).onSuccess { tokenCredential ->
            // Add the loaded token credential to the app session's authenticationManager
            ArcGISEnvironment.authenticationManager.arcGISCredentialStore.add(tokenCredential)
            // Load the Naperville electric web-map
            arcGISMap.load().getOrElse {
                handleError(
                    title = "Error loading the web-map: ${it.message}",
                    description = it.cause.toString()
                )
            }
            // Load the utility network associated with the web-map
            utilityNetwork.load().getOrElse {
                handleError(
                    title = "Error loading the utility network: ${it.message}",
                    description = it.cause.toString()
                )
            }

            // Once loaded, remove all operational layers
            arcGISMap.operationalLayers.clear()

            // Create the two service feature table to be added as layers
            val electricalDeviceTable = ServiceFeatureTable("$FEATURE_SERVICE_URL/0")
            val electricalDistributionTable = ServiceFeatureTable("$FEATURE_SERVICE_URL/3")

            // Create feature layers from the service feature tables.
            val electricalDeviceFeatureLayer = FeatureLayer.createWithFeatureTable(
                featureTable = electricalDeviceTable
            )
            val electricalDistributionFeatureLayer = FeatureLayer.createWithFeatureTable(
                featureTable = electricalDistributionTable
            ).apply {
                // Customize rendering for the layer
                renderer = electricalDistributionUniqueValueRenderer
            }

            // Add the two feature layers to the map's operational layers
            arcGISMap.operationalLayers.addAll(
                listOf(
                    electricalDistributionFeatureLayer,
                    electricalDeviceFeatureLayer
                )
            )

            // Update hint values to reflect trace stage changes
            viewModelScope.launch {
                _traceState.collect { updateHint(it) }
            }
        }.onFailure {
            handleError(
                title = "Error using TokenCredential: ${it.message}",
                description = it.cause.toString()
            )
        }
    }

    /**
     * Performs an identify operation to obtain the [ArcGISFeature] nearest to the
     * tapped [screenCoordinate]. The selected feature is then used to [identifyUtilityElement].
     */
    fun identifyNearestArcGISFeature(
        mapPoint: Point,
        screenCoordinate: ScreenCoordinate
    ) {
        viewModelScope.launch {
            // Identify the feature on the tapped location
            val identifyResults: List<IdentifyLayerResult> =
                mapViewProxy.identifyLayers(
                    screenCoordinate = screenCoordinate,
                    tolerance = 4.dp,
                    returnPopupsOnly = false,
                    maximumResults = 1
                ).getOrElse {
                    return@launch messageDialogVM.showMessageDialog(
                        title = it.message.toString(),
                        description = it.cause.toString()
                    )
                }
            // If the identify returns a result, retrieve the geoelement as an ArcGISFeature
            identifyResults.firstOrNull()?.geoElements?.firstOrNull()?.let { identifiedFeature ->
                (identifiedFeature as? ArcGISFeature)?.let { arcGISFeature ->
                    // Identify the utility element associated with the selected feature
                    identifyUtilityElement(
                        identifiedFeature = arcGISFeature,
                        mapPoint = mapPoint
                    )
                }
            }
        }
    }

    /**
     * Uses the [mapPoint] to identify any utility elements in the utility network.
     * Based on the [UtilityNetworkSourceType] create an element for a junction or an edge.
     */
    private fun identifyUtilityElement(
        identifiedFeature: ArcGISFeature,
        mapPoint: Point
    ) {
        // Get the network source of the identified feature
        val utilityNetworkSource = utilityNetwork.definition?.networkSources?.value?.firstOrNull {
            it.featureTable.tableName == identifiedFeature.featureTable?.tableName
        } ?: return handleError("Selected feature does not contain a Utility Network Source.")

        // Check if the network source is a junction or an edge
        when (utilityNetworkSource.sourceType) {
            UtilityNetworkSourceType.Junction -> {
                // Create a junction element with the identified feature
                createJunctionUtilityElement(
                    identifiedFeature = identifiedFeature,
                    utilityNetworkSource = utilityNetworkSource
                )
            }

            UtilityNetworkSourceType.Edge -> {
                // Create an edge element with the identified feature
                createEdgeUtilityElement(
                    identifiedFeature = identifiedFeature,
                    mapPoint = mapPoint
                )
            }
        }
    }

    /**
     * Create a [UtilityElement] of the [identifiedFeature].
     */
    private fun createJunctionUtilityElement(
        identifiedFeature: ArcGISFeature,
        utilityNetworkSource: UtilityNetworkSource
    ) {
        // Find the code matching the asset group name in the feature's attributes
        val assetGroupCode = identifiedFeature.attributes["assetgroup"] as Int
        // Find the network source's asset group with the matching code
        utilityNetworkSource.assetGroups.first { it.code == assetGroupCode }.assetTypes
            // Find the asset group type code matching the feature's asset type code
            .first { it.code == identifiedFeature.attributes["assettype"].toString().toInt() }
            .let { utilityAssetType ->
                // Get the list of terminals for the feature
                val terminals = utilityAssetType.terminalConfiguration?.terminals
                    ?: return handleError("Error retrieving terminal configuration")

                // If there is only one terminal, use it to create a utility element
                when (terminals.size) {
                    1 -> {
                        // Create a utility element
                        utilityNetwork.createElementOrNull(
                            arcGISFeature = identifiedFeature,
                            terminal = terminals.first()
                        )?.let { utilityElement ->
                            // Add the utility element to the map
                            addUtilityElementToMap(
                                identifiedFeature = identifiedFeature,
                                mapPoint = identifiedFeature.geometry as Point,
                                utilityElement = utilityElement
                            )
                        }
                    }
                    // If there is more than one terminal, prompt the user to select one
                    else -> {
                        // Reset the index, as the user would need to make a choice
                        _selectedTerminalConfigurationIndex.value = null
                        // Get a list of terminal names from the terminal configuration
                        val terminalConfiguration = utilityAssetType.terminalConfiguration ?: return
                        // Update the list of available terminal options
                        _terminalConfigurationOptions.value = terminalConfiguration.terminals
                        // Show the dialog to choose a terminal configuration
                        _traceState.value = TraceState.TERMINAL_CONFIGURATION_REQUIRED

                        viewModelScope.launch {
                            _selectedTerminalConfigurationIndex.collect { selectedIndex ->
                                if (selectedIndex != null) {
                                    // Create a utility element
                                    val element = utilityNetwork.createElementOrNull(
                                        arcGISFeature = identifiedFeature,
                                        terminal = terminals[selectedIndex]
                                    ) ?: return@collect handleError(
                                        "Error creating utility element"
                                    )
                                    // Add the utility element graphic to the map
                                    addUtilityElementToMap(
                                        identifiedFeature = identifiedFeature,
                                        mapPoint = identifiedFeature.geometry as Point,
                                        utilityElement = element
                                    )
                                    // Dismiss the dialog to choose another point
                                    _traceState.value = TraceState.ADD_STARTING_POINT
                                }
                            }
                        }
                    }
                }
            }
    }

    /**
     * Create a [UtilityElement] of the [identifiedFeature].
     */
    private fun createEdgeUtilityElement(
        identifiedFeature: ArcGISFeature,
        mapPoint: Point
    ) {
        // Create a utility element with the identified feature
        val element = (utilityNetwork.createElementOrNull(
            arcGISFeature = identifiedFeature,
            terminal = null
        ) ?: return handleError("Error creating element"))
        // Calculate the fraction along these the map point is located
        element.fractionAlongEdge = GeometryEngine.fractionAlong(
            line = GeometryEngine.createWithZ(
                geometry = identifiedFeature.geometry!!,
                z = null // Remove the z-coordinate value from the identified geometry
            ) as Polyline,
            point = mapPoint,
            tolerance = -1.0
        )
        // Add the utility element graphic to the map
        addUtilityElementToMap(
            identifiedFeature = identifiedFeature,
            mapPoint = mapPoint,
            utilityElement = element
        )
        // Update the hint text
        updateHint("Fraction along the edge: ${element.fractionAlongEdge}")
    }

    /**
     * Add [utilityElement] to either the starting locations or barriers list
     * and add a graphic representing it to the [graphicsOverlay].
     */
    private fun addUtilityElementToMap(
        identifiedFeature: ArcGISFeature,
        mapPoint: Point,
        utilityElement: UtilityElement
    ) {
        graphicsOverlay.graphics.add(
            Graphic(
                geometry = GeometryEngine.nearestCoordinate(
                    geometry = identifiedFeature.geometry!!,
                    point = mapPoint
                )?.coordinate
            ).apply {
                // Add the element to the appropriate list (starting locations or barriers),
                // and add the appropriate symbol to the graphic
                when (_selectedPointType.value) {
                    PointType.Start -> {
                        utilityElementStartingLocations.add(utilityElement)
                        symbol = startingPointSymbol
                        _canTrace.value = true
                    }

                    PointType.Barrier -> {
                        utilityElementBarriers.add(utilityElement)
                        symbol = barrierPointSymbol
                    }
                }
            }
        )
    }

    /**
     * Uses the elements selected as starting locations and (optionally) barriers
     * to perform a connected trace, then selects all connected elements
     * found in the trace to highlight them.
     */
    fun traceUtilityNetwork() {
        // Check that the utility trace parameters are valid
        if (utilityElementStartingLocations.isEmpty()) {
            return handleError("No starting locations provided for trace.")
        }

        val traceType = _selectedTraceType.value

        // Create utility trace parameters for the given trace type
        val traceParameters = UtilityTraceParameters(
            traceType = traceType,
            startingLocations = utilityElementStartingLocations
        ).apply {
            // If any barriers have been created, add them to the parameters
            barriers.addAll(utilityElementBarriers)
            // Set the trace configuration using the tier from the utility domain network
            traceConfiguration = mediumVoltageTier?.getDefaultTraceConfiguration()
        }

        // Run the utility trace and get the results
        viewModelScope.launch {
            // Update the trace state
            _traceState.value = TraceState.RUNNING_TRACE_UTILITY_NETWORK
            // Perform the trace with the above parameters, and obtain the results list
            val traceResults = utilityNetwork.trace(traceParameters)
                .getOrElse {
                    return@launch handleError(
                        title = "Error performing trace: ${it.message}",
                        description = it.cause.toString()
                    )
                }

            // Get the utility trace result's first result as a utility element trace result
            (traceResults.first() as? UtilityElementTraceResult)?.let { utilityElementTraceResult ->
                // Ensure the result is not empty
                if (utilityElementTraceResult.elements.isEmpty())
                    return@launch handleError("No elements found in the trace result")

                arcGISMap.operationalLayers.filterIsInstance<FeatureLayer>()
                    .forEach { featureLayer ->
                        // Clear previous selection
                        featureLayer.clearSelection()
                        val params = QueryParameters().apply {
                            returnGeometry = true // Used to calculate the viewpoint result
                        }
                        // Create query parameters to find features who's network source name matches the layer's feature table name
                        utilityElementTraceResult.elements.filter {
                            it.networkSource.name == featureLayer.featureTable?.tableName
                        }.forEach { utilityElement ->
                            params.objectIds.add(utilityElement.objectId)
                        }
                        // Select features that match the query
                        val featureQueryResult = featureLayer.selectFeatures(
                            parameters = params,
                            mode = SelectionMode.New
                        ).getOrElse {
                            return@launch handleError(
                                title = it.message.toString(),
                                description = it.cause.toString()
                            )
                        }

                        // Create list of all the feature result geometries
                        val resultGeometryList = mutableListOf<Geometry>()
                        featureQueryResult.iterator().forEach { feature ->
                            feature.geometry?.let {
                                resultGeometryList.add(it)
                            }
                        }

                        // Obtain the union geometry of all the feature geometries
                        GeometryEngine.unionOrNull(resultGeometryList)?.let { unionGeometry ->
                            // Set the map's viewpoint to the union result geometry
                            mapViewProxy.setViewpointAnimated(Viewpoint(boundingGeometry = unionGeometry))
                        }
                    }

                // Update the trace state
                _traceState.value = TraceState.TRACE_COMPLETED
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
        utilityElementBarriers.clear()
        utilityElementStartingLocations.clear()
        graphicsOverlay.graphics.clear()
        _traceState.value = TraceState.ADD_STARTING_POINT
        _canTrace.value = false
        _selectedTraceType.value = UtilityTraceType.Connected
        _selectedTerminalConfigurationIndex.value = null
        _selectedPointType.value = PointType.Start
        _terminalConfigurationOptions.value = listOf()
    }

    /**
     * Update the [utilityTraceType] selected by the user
     */
    fun updateTraceType(utilityTraceType: UtilityTraceType) {
        _selectedTraceType.value = utilityTraceType
        _traceState.value = TraceState.ADD_STARTING_POINT
    }

    /**
     * Switch from adding .start points to adding .barrier, or vice versa.
     */
    fun updatePointType(pointType: PointType) {
        _selectedPointType.value = pointType
        when (pointType) {
            PointType.Start -> {
                _traceState.value = TraceState.ADD_STARTING_POINT
            }

            PointType.Barrier -> {
                _traceState.value = TraceState.ADD_BARRIER_POINT
            }
        }
    }

    /**
     * Update the index used to select the [terminalConfigurationOptions]
     */
    fun updateTerminalConfigurationOption(index: Int) {
        _selectedTerminalConfigurationIndex.value = index
    }

    /**
     * Update the hint flow to display new [message].
     */
    private fun updateHint(message: String) {
        _hint.value = message
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    private fun handleError(title: String, description: String = "") {
        reset()
        _traceState.value = TraceState.TRACE_FAILED
        messageDialogVM.showMessageDialog(title, description)
    }

    companion object {
        // Public credentials for the data in this sample.
        const val USERNAME = "viewer01"
        const val PASSWORD = "I68VGU^nMurF"

        // The portal item ID for Naperville’s electrical network
        private const val NAPERVILLE_ELECTRICAL_NETWORK_ITEM_ID = "471eb0bf37074b1fbb972b1da70fb310"
        private const val SAMPLE_SERVER_7 = "https://sampleserver7.arcgisonline.com"
        private const val SAMPLE_PORTAL_URL = "$SAMPLE_SERVER_7/portal/sharing/rest"
        private const val FEATURE_SERVICE_URL =
            "$SAMPLE_SERVER_7/server/rest/services/UtilityNetwork/NapervilleElectric/FeatureServer"
    }
}

enum class PointType {
    Start,
    Barrier
}

object TraceState {
    const val ADD_STARTING_POINT = "Tap on map to add a stating location point(s)"
    const val ADD_BARRIER_POINT = "Tap on map to add a barrier point(s)"
    const val TERMINAL_CONFIGURATION_REQUIRED = "Select Terminal Configuration"
    const val RUNNING_TRACE_UTILITY_NETWORK = "Evaluating trace utility network"
    const val TRACE_COMPLETED = "Trace completed"
    const val TRACE_FAILED = "Fail to run trace"
}