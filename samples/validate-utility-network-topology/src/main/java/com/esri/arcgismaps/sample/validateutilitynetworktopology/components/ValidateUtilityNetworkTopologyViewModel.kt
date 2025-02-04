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

package com.esri.arcgismaps.sample.validateutilitynetworktopology.components

import android.app.Application
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.Guid
import com.arcgismaps.arcgisservices.FeatureServiceSessionType
import com.arcgismaps.arcgisservices.ServiceVersionParameters
import com.arcgismaps.arcgisservices.VersionAccess
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.CodedValue
import com.arcgismaps.data.CodedValueDomain
import com.arcgismaps.data.Field
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.data.ServiceGeodatabase
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.TokenCredential
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.labeling.LabelDefinition
import com.arcgismaps.mapping.labeling.SimpleLabelExpression
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.SelectionMode
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.portal.Portal
import com.arcgismaps.tasks.geoprocessing.GeoprocessingExecutionType
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityElementTraceResult
import com.arcgismaps.utilitynetworks.UtilityNetwork
import com.arcgismaps.utilitynetworks.UtilityTraceParameters
import com.arcgismaps.utilitynetworks.UtilityTraceType
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.validateutilitynetworktopology.components.ValidateUtilityNetworkTopologyViewModel.Companion.PASSWORD
import com.esri.arcgismaps.sample.validateutilitynetworktopology.components.ValidateUtilityNetworkTopologyViewModel.Companion.USERNAME
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID

class ValidateUtilityNetworkTopologyViewModel(application: Application) :
    AndroidViewModel(application) {

    // The map containing the Naperville Electric web map
    val arcGISMap = ArcGISMap(
        item = PortalItem(
            itemId = NAPERVILLE_ELECTRIC_WEBMAP_ITEM_ID,
            portal = Portal(
                url = SAMPLE_SERVER_7_PORTAL,
                connection = Portal.Connection.Authenticated
            )
        )
    ).apply {
        initialViewpoint = Viewpoint(center = Point(-9815160.0, 5128780.0), scale = 4000.0)
        // Set the map to load in persistent session mode (workaround for server caching issue)
        // https://support.esri.com/en-us/bug/asynchronous-validate-request-for-utility-network-servi-bug-000160443
        loadSettings.featureServiceSessionType = FeatureServiceSessionType.Persistent
    }

    // Used to update map viewpoint and identify layers on tap
    val mapViewProxy = MapViewProxy()

    // Graphic to represent the starting point of the downstream trace
    private val startingLocationGraphic = Graphic(
        symbol = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Cross, color = Color.green, size = 25f
        )
    )

    // Overlay to show the starting location for tracing
    val graphicsOverlay = GraphicsOverlay(listOf(startingLocationGraphic))

    // The utility network used for tracing
    private val utilityNetwork: UtilityNetwork
        get() = arcGISMap.utilityNetworks.first()

    // ServiceGeodatabase from the utility network, used for switching to a new version
    private var serviceGeodatabase: ServiceGeodatabase? = null

    // Parameters for a downstream trace
    private var traceParameters: UtilityTraceParameters? = null

    // Track the state of utility network capabilities
    private val _canGetState = MutableStateFlow(false)
    val canGetState = _canGetState.asStateFlow()

    // Track the state of utility network topology
    private val _canTrace = MutableStateFlow(false)
    val canTrace = _canTrace.asStateFlow()

    // Track the state when we have dirty areas or errors
    private val _canValidateNetworkTopology = MutableStateFlow(false)
    val canValidateNetworkTopology = _canValidateNetworkTopology.asStateFlow()

    // Track the state when selections can be cleared from map
    private val _canClearSelection = MutableStateFlow(false)
    val canClearSelection = _canClearSelection.asStateFlow()

    // A collapsable status message to display in the UI
    private val _statusMessage = MutableStateFlow("")
    val statusMessage = _statusMessage.asStateFlow()

    // Currently selected feature that the user is editing
    private val _selectedFeature = MutableStateFlow<ArcGISFeature?>(null)
    val selectedFeature = _selectedFeature.asStateFlow()

    // Field being edited on the selected feature (e.g. "devicestatus" or "nominalvoltage")
    private var selectedField: Field? = null

    // List of coded values from the above field's domain
    private val _fieldValueOptions = MutableStateFlow<List<CodedValue>>(emptyList())
    val fieldValueOptions = _fieldValueOptions.asStateFlow()

    // Selected field coded value from the domain
    private val _selectedFieldValue = MutableStateFlow<CodedValue?>(null)
    val selectedFieldValue = _selectedFieldValue.asStateFlow()

    // Keep track of the current visible area to validate network topology
    private val _currentVisibleArea = MutableStateFlow(Envelope(Point(0.0, 0.0), Point(0.0, 0.0)))

    /**
     * Add credentials and loads the map & utility network. Then creates & switches to a
     * new version for editing. Sets up the default trace parameters and adds the dirty area
     * table as a feature layer to the map. Sets the button states based on
     * utility network capabilities.
     */
    suspend fun initialize() {
        // Authenticate and load ArcGIS map
        setupMap()
        // Load utility network, and switch to a new version to allow edits
        setupUtilityNetwork()
        // Set up the default trace parameters (downstream)
        setupTraceParameters()
        // Check capabilities from the network definition to enable/disable relevant UI.
        setButtonStatesFromCapabilities()
        // Display contextual hint
        _statusMessage.value = INIT_STATUS_MESSAGE
    }

    /**
     * Authenticate and load map.
     */
    private suspend fun setupMap() {
        ArcGISEnvironment.apply {
            applicationContext = getApplication<Application>().applicationContext
            authenticationManager.arcGISAuthenticationChallengeHandler = getAuthChallengeHandler()
        }
        // Load the Naperville electric web-map
        arcGISMap.load().onFailure {
            handleError(
                title = "Error loading the web-map: ${it.message}",
                description = it.cause.toString()
            )
        }
    }

    /**
     * Create a new version and switch to allow for attribute editing.
     */
    private suspend fun setupUtilityNetwork() {
        // Load the utility network associated with the web-map
        utilityNetwork.load().onFailure {
            handleError(
                title = "Error loading the utility network: ${it.message}",
                description = it.cause.toString()
            )
        }
        // Construct version parameters
        val parameters = ServiceVersionParameters().apply {
            name = "ValidateNetworkTopology_${UUID.randomUUID()}"
            description = "Validate network topology with ArcGIS Maps SDK."
            access = VersionAccess.Private
        }
        // Retrieve the service geodatabase from the utility network
        serviceGeodatabase = utilityNetwork.serviceGeodatabase?.apply {
            // Load the service geodatabase
            load().onFailure {
                return handleError("Error loading service geodatabase: ${it.message}")
            }
            // Create a new private service version
            val versionInfo = createVersion(newVersion = parameters).getOrElse {
                return handleError("Unable to create version", "${it.message}")
            }
            // Switch the service geodatabase to the newly created version
            switchVersion(versionName = versionInfo.name).onFailure {
                return handleError("Failed to switch to version", "${it.message}")
            }
        }
        // Add the dirty area table to the map to visualize it
        utilityNetwork.dirtyAreaTable?.let { dirtyAreaTable ->
            arcGISMap.operationalLayers.add(
                element = FeatureLayer.createWithFeatureTable(dirtyAreaTable)
            )
        }
        // Add labels to the map to visualize attribute editing
        addLabels(
            layerName = DEVICE_TABLE_NAME,
            fieldName = DEVICE_STATUS_FIELD,
            textColor = Color.blue
        )
        addLabels(
            layerName = LINE_TABLE_NAME,
            fieldName = NOMINAL_VOLTAGE_FIELD,
            textColor = Color.red
        )
    }

    /**
     * Downstream trace parameters from a known device location,
     * stopping traversal on an open device.
     */
    private suspend fun setupTraceParameters() {
        // Get the definition of the utility network
        val networkDefinition = utilityNetwork.definition ?: return
        // Look up the network source by its table name
        val utilityNetworkSource = networkDefinition.networkSources.value.first {
            it.name == DEVICE_TABLE_NAME
        }
        // Get the asset group
        val circuitBreakerGroup = utilityNetworkSource.assetGroups.first {
            it.name == "Circuit Breaker"
        }
        // Get the asset type
        val threePhaseType = circuitBreakerGroup.assetTypes.first {
            it.name == "Three Phase"
        }
        // Create the element for the known globalID
        val startingElement = utilityNetwork.createElementOrNull(
            assetType = threePhaseType,
            globalId = STARTING_LOCATION_GLOBAL_ID
        )?.apply {
            // Find the "Load" terminal
            terminal = threePhaseType.terminalConfiguration?.terminals?.first { it.name == "Load" }
        } ?: return handleError("Error creating utility network element")
        // Convert the utility element to an ArcGIS feature
        val startingFeature = utilityNetwork.getFeaturesForElements(
            elements = listOf(startingElement)
        ).getOrNull()?.firstOrNull()
        // Add a graphic to indicate the location on the map
        startingLocationGraphic.geometry = startingFeature?.geometry
        // Get the utility domain network
        val domainNetwork = networkDefinition.getDomainNetwork("ElectricDistribution") ?: return
        // Get the utility tier object
        val mediumVoltageRadial = domainNetwork.getTier("Medium Voltage Radial") ?: return
        // Create trace parameters for a downstream trace from the element
        traceParameters = UtilityTraceParameters(
            traceType = UtilityTraceType.Downstream,
            startingLocations = listOf(startingElement)
        ).apply {
            // Use the same trace config from the utility tier
            traceConfiguration = mediumVoltageRadial.getDefaultTraceConfiguration()
        }
    }

    /**
     * Enable or disable the main action buttons based on the network definition's capabilities.
     */
    private fun setButtonStatesFromCapabilities() {
        utilityNetwork.definition?.capabilities?.apply {
            _canGetState.value = supportsNetworkState
            _canTrace.value = supportsTrace
            _canValidateNetworkTopology.value = supportsValidateNetworkTopology
        }
    }

    /**
     * Adds labels for a given field name to a layer with a given name.
     */
    private fun addLabels(layerName: String, fieldName: String, textColor: Color) {
        // Create a expression for the label using the given field name
        val expression = SimpleLabelExpression(simpleExpression = "[$fieldName]")
        // Create a symbol for label's text using the given color
        val symbol = TextSymbol().apply {
            color = textColor
            size = 12f
            haloColor = Color.white
            haloWidth = 2f
        }
        // Create the definition from the expression and text symbol
        val definition = LabelDefinition(labelExpression = expression, textSymbol = symbol)
        // Add the definition to the map layer with the given layer name.
        val layer = arcGISMap.operationalLayers.first { it.name == layerName } as FeatureLayer
        layer.labelDefinitions.add(definition)
        layer.labelsEnabled = true
    }

    /**
     * Gets the current state of the utility network (hasDirtyAreas, hasErrors, isNetworkTopologyEnabled).
     */
    fun getState() {
        _statusMessage.value = "Getting utility network state…"
        viewModelScope.launch {
            val networkState = utilityNetwork.getState().getOrElse {
                return@launch handleError("Get state failed", it.message.toString())
            }
            // Update to true if there are unsaved changes or errors in the utility network state
            _canValidateNetworkTopology.value = networkState.hasDirtyAreas || networkState.hasErrors
            // Update trace availability
            _canTrace.value = networkState.isNetworkTopologyEnabled
            // Update contextual hint
            val tip = if (_canValidateNetworkTopology.value) {
                "Tap Validate before trace or expect a trace error."
            } else {
                "Tap on a feature to edit, or tap Trace."
            }
            _statusMessage.value = buildString {
                appendLine("Utility Network State:")
                appendLine("Has dirty areas: ${networkState.hasDirtyAreas}")
                appendLine("Has errors: ${networkState.hasErrors}")
                appendLine("Network topology enabled: ${networkState.isNetworkTopologyEnabled}")
                append(tip)
            }
        }
    }

    /**
     * Validates the utility network topology in the visible map extent to check
     * for check dirty areas and identify errors in the network topology.
     */
    fun validateNetworkTopology() {
        _statusMessage.value = "Validating utility network topology…"
        // Create and start the utility network validation job using the current visible extent
        val utilityNetworkValidationJob = utilityNetwork.validateNetworkTopology(
            extent = _currentVisibleArea.value,
            executionType = GeoprocessingExecutionType.SynchronousExecute
        ).also { job -> job.start() }
        viewModelScope.launch {
            // Retrieve the result from the job
            val validationResult = utilityNetworkValidationJob.result().getOrElse {
                return@launch handleError("Validation job error: ${it.message}")
            }
            // After validation, check if dirty areas remain.
            _canValidateNetworkTopology.value = validationResult.hasDirtyAreas
            _statusMessage.value = buildString {
                appendLine("Network Validation Result")
                appendLine("Has dirty areas: ${validationResult.hasDirtyAreas}")
                appendLine("Has errors: ${validationResult.hasErrors}")
                append("Tap 'Get State' to check the updated network state.")
            }
        }
    }

    /**
     * Identify a single feature from the [screenCoordinate], check if belongs to the
     * device or line layer, get the coded-value domain from it's field to allow editing.
     */
    fun identifyFeatureAt(
        screenCoordinate: ScreenCoordinate,
        selectedFieldAlias: (String) -> Unit
    ) {
        viewModelScope.launch {
            // Perform an identify operation at the given screen coords
            val identifyLayerResults = mapViewProxy.identifyLayers(
                screenCoordinate = screenCoordinate,
                tolerance = IDENTIFY_TOLERANCE.dp,
                returnPopupsOnly = false,
                maximumResults = 1
            ).getOrElse { return@launch handleError("Select feature failed: ${it.message}") }
            // Find the first result from identify results for device/line layers
            val identifyLayerResult = identifyLayerResults.firstOrNull { layerResult ->
                val layerName = layerResult.layerContent.name
                layerName == DEVICE_TABLE_NAME || layerName == LINE_TABLE_NAME
            }
            // Find the first feature from results
            val foundFeature = identifyLayerResult?.geoElements?.firstOrNull() as? ArcGISFeature
            // Return if no feature was identified
            if (foundFeature == null) {
                _statusMessage.value = "No feature identified. Tap in the device or line layer."
                return@launch
            }
            // Find the coded-value domain field to edit
            val (fieldName, fieldAlias) = when (foundFeature.featureTable?.tableName) {
                DEVICE_TABLE_NAME -> DEVICE_STATUS_FIELD to "Device status"
                LINE_TABLE_NAME -> NOMINAL_VOLTAGE_FIELD to "Nominal voltage"
                else -> null to null
            }
            if (fieldName == null) {
                _statusMessage.value = "Selected feature is not editable."
                return@launch
            }
            // Retrieve the field from the feature table
            val tableField = foundFeature.featureTable?.fields?.firstOrNull {
                it.name == fieldName
            } ?: return@launch handleError("Field '$fieldName' not found in feature table.")
            // Obtain the list of coded value fields in the selected feature
            val codedValues = (tableField.domain as? CodedValueDomain)?.codedValues ?: emptyList()
            // Update the currently selected field that the user is editing
            selectedField = tableField
            // Update the list of field value options of the selected feature
            _fieldValueOptions.value = codedValues
            // Retrieve the currently selected attribute
            _selectedFieldValue.value = codedValues.find { codedValue ->
                codedValue.code == foundFeature.attributes[fieldName]
            }
            // Clear any previous selections
            clearLayerSelections()
            // Select the identified feature on its layer
            (foundFeature.featureTable?.layer as? FeatureLayer)?.selectFeature(foundFeature)
            // Set the viewpoint to the selected feature
            foundFeature.geometry?.let { mapViewProxy.setViewpointGeometry(it, 20.0) }
            // Update the currently selected feature that the user is editing
            _selectedFeature.value = foundFeature
            // Update UI with selected feature info
            _statusMessage.value = "Select a new '$fieldAlias' value, then tap Apply."
            _canClearSelection.value = true
            selectedFieldAlias(fieldAlias.toString())
        }
    }

    /**
     * Run a simple downstream trace from the previously configured trace parameters.
     * Then select any features found by the trace.
     */
    fun trace() {
        _statusMessage.value = "Running a downstream trace…"
        viewModelScope.launch {
            // Clear previous selections
            clearLayerSelections()
            // Get the trace params
            val params = traceParameters ?: return@launch handleError("Trace parameters not set.")
            // Run trace, and obtain results, if errors/dirty areas are found the trace will fail
            val traceResults = utilityNetwork.trace(params).getOrElse {
                return@launch handleError("Trace failed", it.message + "\n" + it.cause)
            }
            // Get the first trace element result
            val elementTraceResult =
                traceResults.firstOrNull { it is UtilityElementTraceResult } as? UtilityElementTraceResult
                    ?: return@launch
            // Group elements by which layer/table they belong to and select them.
            selectTraceResultElements(elementTraceResult.elements)
            // Update contextual hint
            _statusMessage.value = "Trace completed:" +
                    "${elementTraceResult.elements.size} elements found."
        }
    }

    /**
     * Select all features that match the given list of utility elements found by the trace.
     */
    private suspend fun selectTraceResultElements(elements: List<UtilityElement>) {
        // Ensure the result is not empty
        if (elements.isEmpty()) return handleError("No elements found in the trace result")
        // Find the matching feature's geometry of each utility element in all layers
        arcGISMap.operationalLayers.filterIsInstance<FeatureLayer>()
            .forEach { featureLayer ->
                // Clear previous selection
                featureLayer.clearSelection()
                // Used to calculate the viewpoint result
                val params = QueryParameters().apply { returnGeometry = true }
                // Create query parameters to find features who's network source name matches the layer's feature table name
                elements.filter { it.networkSource.name == featureLayer.featureTable?.tableName }
                    .forEach { utilityElement -> params.objectIds.add(utilityElement.objectId) }
                // Check if any trace results were added from the above filter
                if (params.objectIds.isNotEmpty()) {
                    // Select features that match the query
                    val featureQueryResult = featureLayer.selectFeatures(
                        parameters = params,
                        mode = SelectionMode.New
                    ).getOrElse {
                        return handleError(
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
            }
        _canClearSelection.value = true
    }

    /**
     * Update the feature with the new coded value and apply the edit to the feature service.
     */
    fun applyEdits() {
        _statusMessage.value = "Applying edits…"
        // Get the selected feature to update
        val feature = _selectedFeature.value ?: return
        // Get the table which related to the feature
        val serviceFeatureTable = feature.featureTable as? ServiceFeatureTable
            ?: return handleError("Feature is not from a service feature table.")
        // Get the selected field value coded value
        val newCode = _selectedFieldValue.value?.code
            ?: return handleError("No new coded value selected.")
        // Update the feature's attribute
        val fieldName = selectedField?.name ?: return
        feature.attributes[fieldName] = newCode
        // Apply edits and handle results
        viewModelScope.launch {
            // Update the backing service feature table
            serviceFeatureTable.updateFeature(feature).onFailure {
                return@launch handleError("Failed to update feature", it.message + "\n" + it.cause)
            }
            // Apply all local edits in all tables to the service geodatabase
            val editResults = serviceGeodatabase?.applyEdits()?.getOrElse {
                return@launch handleError("Apply edits failed: ${it.message}")
            }
            // Check for any feature edit errors
            val hadErrors = editResults?.any { tableEditResult ->
                tableEditResult.editResults.any { featureEditResult ->
                    featureEditResult.completedWithErrors
                }
            } ?: true
            if (hadErrors) _statusMessage.value = "Apply edits completed with error(s)."
            else _statusMessage.value = "Edits applied successfully.\n" +
                    "Tap 'Get State' to check the updated network state."
            // Once edits are applied, enable to validate the network again.
            _canValidateNetworkTopology.value = true
        }
    }

    /**
     * Clears any selected features being edited on the map.
     */
    fun clearFeatureEditSelection() {
        clearLayerSelections()
        _selectedFeature.value = null
        selectedField = null
        _fieldValueOptions.value = emptyList()
        _selectedFieldValue.value = null
        _canClearSelection.value = false
        _statusMessage.value = INIT_STATUS_MESSAGE
    }

    /**
     * Clear the selection on all feature layers in the map.
     */
    private fun clearLayerSelections() {
        arcGISMap.operationalLayers.filterIsInstance<FeatureLayer>().forEach { layer ->
            layer.clearSelection()
        }
    }

    /**
     * Update the currently selected field value
     */
    fun updateSelectedValue(codedValue: CodedValue) {
        _selectedFieldValue.value = codedValue
    }

    /**
     * Update the currently visible extent
     */
    fun updateVisibleArea(polygon: Polygon) {
        _currentVisibleArea.value = polygon.extent
    }

    // Message dialog for errors
    val messageDialogVM = MessageDialogViewModel()

    /**
     * Display error and reset
     */
    private fun handleError(title: String, description: String = "") {
        reset()
        messageDialogVM.showMessageDialog(title, description)
    }

    /**
     * Resets the trace, removing graphics and clearing selections.
     */
    fun reset() {
        arcGISMap.operationalLayers
            .filterIsInstance<FeatureLayer>()
            .forEach { it.clearSelection() }
        graphicsOverlay.graphics.clear()
        _canTrace.value = false
    }

    companion object {
        // Public credentials for the data in this sample (Editor user)
        const val USERNAME = "editor01"
        const val PASSWORD = "S7#i2LWmYH75"

        // The Naperville electric web map on sample server 7
        private const val NAPERVILLE_ELECTRIC_WEBMAP_ITEM_ID = "6e3fc6db3d0b4e6589eb4097eb3e5b9b"
        private const val SAMPLE_SERVER_7_PORTAL = "https://sampleserver7.arcgisonline.com/portal/"

        // The relevant table names/fields in Naperville electric
        private const val DEVICE_TABLE_NAME = "Electric Distribution Device"
        private const val DEVICE_STATUS_FIELD = "devicestatus"
        private const val LINE_TABLE_NAME = "Electric Distribution Line"
        private const val NOMINAL_VOLTAGE_FIELD = "nominalvoltage"

        // The known device's global ID used for the default starting location
        private val STARTING_LOCATION_GLOBAL_ID = Guid("1CAF7740-0BF4-4113-8DB2-654E18800028")

        // Tolerance in device-independent pixels for identify
        private const val IDENTIFY_TOLERANCE = 10f

        private const val INIT_STATUS_MESSAGE = "Utility network loaded.\n" +
                "Tap on a feature to edit.\n" +
                "Tap on 'Get State' to check if validating is necessary or if tracing is available.\n" +
                "Tap 'Trace' to run a trace."
    }
}

/**
 * Returns a [ArcGISAuthenticationChallengeHandler] to access the utility network URL.
 */
private fun getAuthChallengeHandler(): ArcGISAuthenticationChallengeHandler {
    return ArcGISAuthenticationChallengeHandler { challenge ->
        val result: Result<TokenCredential> = runBlocking {
            TokenCredential.create(challenge.requestUrl, USERNAME, PASSWORD, 0)
        }
        if (result.getOrNull() != null) {
            val credential = result.getOrNull()
            return@ArcGISAuthenticationChallengeHandler ArcGISAuthenticationChallengeResponse
                .ContinueWithCredential(credential!!)
        } else {
            val ex = result.exceptionOrNull()
            return@ArcGISAuthenticationChallengeHandler ArcGISAuthenticationChallengeResponse
                .ContinueAndFailWithError(ex!!)
        }
    }
}

private val Color.Companion.blue: Color
    get() {
        return fromRgba(0, 0, 255, 255)
    }
