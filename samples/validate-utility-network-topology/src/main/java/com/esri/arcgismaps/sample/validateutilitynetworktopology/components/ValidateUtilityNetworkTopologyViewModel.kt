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

/*
 * Copyright 2025 Esri
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
 */


import android.app.Application
import android.util.Log
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
import com.arcgismaps.mapping.symbology.TextSymbol
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

    // The ArcGISMap containing the Naperville Electric web map
    val arcGISMap = ArcGISMap(
        item = PortalItem(
            itemId = NAPERVILLE_ELECTRIC_WEBMAP_ITEM_ID,
            portal = Portal(
                url = SAMPLE_SERVER_7_PORTAL,
                connection = Portal.Connection.Authenticated
            )
        )
    ).apply {
        initialViewpoint = Viewpoint(center = Point(-9815160.0, 5128880.0), scale = 3640.0)
    }

    // Used to update map viewpoint and identify layers on tap
    val mapViewProxy = MapViewProxy()

    // Shows the starting location for tracing or other markers if desired
    val graphicsOverlay = GraphicsOverlay()

    // The utility network used for tracing
    private val utilityNetwork: UtilityNetwork
        get() = arcGISMap.utilityNetworks.first()

    // ServiceGeodatabase from the utility network, used for editing and for version management
    private var serviceGeodatabase: ServiceGeodatabase? = null

    // Basic downstream trace parameters for the sample, once loaded
    private var traceParameters: UtilityTraceParameters? = null

    // Whether we can call "Get State" (checking utility network capabilities)
    private val _canGetState = MutableStateFlow(false)
    val canGetState = _canGetState.asStateFlow()

    // Whether we can perform a trace right now (depends on network topology)
    private val _canTrace = MutableStateFlow(false)
    val canTrace = _canTrace.asStateFlow()

    // Whether we can validate the utility network topology (when we have dirty areas or errors)
    private val _canValidateNetworkTopology = MutableStateFlow(false)
    val canValidateNetworkTopology = _canValidateNetworkTopology.asStateFlow()

    // Whether we can clear the current feature selection from the map
    private val _canClearSelection = MutableStateFlow(false)
    val canClearSelection = _canClearSelection.asStateFlow()

    // A status message or log that is shown in the UI
    private val _statusMessage = MutableStateFlow("")
    val statusMessage = _statusMessage.asStateFlow()

    // Currently selected feature that the user is editing
    private val _selectedFeature = MutableStateFlow<ArcGISFeature?>(null)
    val selectedFeature = _selectedFeature.asStateFlow()

    // Field being edited on the selected feature (e.g. "devicestatus" or "nominalvoltage")
    private var selectedField: Field? = null

    // Available coded values from the above field's domain
    private val _fieldValueOptions = MutableStateFlow<List<CodedValue>>(emptyList())
    val fieldValueOptions = _fieldValueOptions.asStateFlow()

    // Currently selected coded value from the domain
    private val _selectedFieldValue = MutableStateFlow<CodedValue?>(null)
    val selectedFieldValue = _selectedFieldValue.asStateFlow()

    // Keep track of the current visible area of the MapView
    private val _currentVisibleArea = MutableStateFlow(Envelope(Point(0.0, 0.0), Point(0.0, 0.0)))

    // Simple message dialog for errors or important info
    val messageDialogVM = MessageDialogViewModel()

    /**
     * Clears any selected features from the map.
     */
    fun clearSelection() {
        clearLayerSelections()
        _selectedFeature.value = null
        selectedField = null
        _fieldValueOptions.value = emptyList()
        _selectedFieldValue.value = null
        _canClearSelection.value = false
        _statusMessage.value = INIT_STATUS_MESSAGE
        setButtonStatesFromCapabilities()
    }

    /**
     * Add credentials and loads the map & utility network. Then creates & switches to a
     * new version for editing. Setting up the default trace parameters. Adds the dirty area
     * table as a feature layer to the map. Then checks the utility network capabilities
     * to set button states.
     */
    suspend fun initialize() {
        Log.e("INITIALIZED","Initialize called")
        // Authenticate and load ArcGIS map
        setupMap()
        // Load utility network, and switch to a new version to allow edits
        setupUtilityNetwork()
        // Set up the default trace parameters (downstream)
        setupTraceParameters()
        // Check capabilities from the network definition to enable/disable relevant UI.
        setButtonStatesFromCapabilities()

        _statusMessage.value = INIT_STATUS_MESSAGE
    }

    private suspend fun setupMap() {
        ArcGISEnvironment.apply {
            applicationContext = getApplication<Application>().applicationContext
            authenticationManager.arcGISAuthenticationChallengeHandler = getAuthChallengeHandler()
        }

        // Load the Naperville electric web-map
        arcGISMap.apply {
            // Set the map to load in persistent session mode (workaround for server caching issue)
            // https://support.esri.com/en-us/bug/asynchronous-validate-request-for-utility-network-servi-bug-000160443
            loadSettings.featureServiceSessionType = FeatureServiceSessionType.Persistent
        }.load().onFailure {
            handleError(
                title = "Error loading the web-map: ${it.message}",
                description = it.cause.toString()
            )
        }
    }

    /**
     * Create a new version and switch to if to allow for attribute editing.
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
        serviceGeodatabase = (utilityNetwork.serviceGeodatabase)?.apply {
            // Load the service geodatabase
            load().onFailure {
                return handleError("Error loading service geodatabase: ${it.message}")
            }
            // Create a new private service version
            val versionInfo = createVersion(parameters).getOrElse {
                return handleError("Unable to create version", "${it.message}")
            }
            // Switch the service geodatabase to the newly created version
            switchVersion(versionInfo.name).onFailure {
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
     * Downstream trace parameters from a known device location, stopping traversal on an open device.
     */
    private fun setupTraceParameters() {
        val networkDefinition = utilityNetwork.definition ?: return
        val domainNetwork = networkDefinition.getDomainNetwork("ElectricDistribution") ?: return
        val mediumVoltageRadial = domainNetwork.getTier("Medium Voltage Radial") ?: return

        // Look up the relevant network source by its table name
        val deviceSource = networkDefinition.networkSources.value.first {
            it.name == DEVICE_TABLE_NAME
        }

        val circuitBreakerGroup = deviceSource.assetGroups.first {
            it.name == "Circuit Breaker"
        }

        val threePhaseType = circuitBreakerGroup.assetTypes.first {
            it.name == "Three Phase"
        }

        // Create the element for the known globalID
        val startElement = utilityNetwork.createElementOrNull(
            assetType = threePhaseType,
            globalId = STARTING_LOCATION_GLOBAL_ID
        ) ?: return messageDialogVM.showMessageDialog("Error creating utility network element")

        // Find the "Load" terminal
        startElement.terminal = threePhaseType.terminalConfiguration?.terminals?.firstOrNull {
            it.name == "Load"
        }

        // Create trace parameters for a downstream trace from the element
        traceParameters = UtilityTraceParameters(
            traceType = UtilityTraceType.Downstream,
            startingLocations = listOf(startElement)
        ).apply {
            // Copy the default trace config from that tier
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
                return@launch handleError("Get state failed: ${it.message}")
            }
            // Update to true if there are unsaved changes or errors in the utility network state
            _canValidateNetworkTopology.value = networkState.hasDirtyAreas || networkState.hasErrors
            // Update trace availability
            _canTrace.value = networkState.isNetworkTopologyEnabled
            // Update contextual hint
            val tip = if (_canValidateNetworkTopology.value)
                "Tap Validate before trace or expect a trace error."
            else "Tap on a feature to edit, or tap Trace."
            _statusMessage.value = buildString {
                appendLine("Utility Network State:")
                appendLine("Has dirty areas: ${networkState.hasDirtyAreas}")
                appendLine("Has errors: ${networkState.hasErrors}")
                appendLine("Network topology enabled: ${networkState.isNetworkTopologyEnabled}")
                appendLine(tip)
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
        ).apply { start() }
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
                appendLine("Tap \"Get State\" to check the updated network state.")
            }
        }
    }

    /**
     * Identify a single feature from a tap on the map, if it belongs to the device or line layer,
     * then gather the coded-value domain from the relevant field to allow editing.
     */
    fun identifyFeatureAt(screenCoordinate: ScreenCoordinate, selectedFieldAlias: (String) -> Unit) {
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

            // Find the first ArcGISFeature from results
            val foundFeature = identifyLayerResult?.geoElements?.firstOrNull() as? ArcGISFeature

            // Return if no feature was identified
            if (foundFeature == null) {
                _statusMessage.value =
                    "No feature identified. Tap a feature in the device or line layer."
                return@launch
            }

            // Find the coded-value domain field to edit
            val (fieldName, fieldAlias) = when (foundFeature.featureTable?.tableName) {
                DEVICE_TABLE_NAME -> DEVICE_STATUS_FIELD to "Device Status"
                LINE_TABLE_NAME -> NOMINAL_VOLTAGE_FIELD to "Nominal Voltage"
                else -> null to null
            }
            if (fieldName == null) {
                _statusMessage.value = "Selected feature is not editable."
                return@launch
            }

            // Attempt to retrieve the field from the feature table
            val tableField = foundFeature.featureTable?.fields?.firstOrNull {
                it.name == fieldName
            } ?: return@launch handleError("Field '$fieldName' not found in feature table.")

            // Obtain the list of coded value fields in the selected feature
            val codedValues = (tableField.domain as? CodedValueDomain)?.codedValues ?: emptyList()

            // Update the currently selected field that the user is editing
            selectedField = tableField

            // Update the list of field value options of the selected feature
            _fieldValueOptions.value = codedValues

            // Retrieve the current attribute
            val currentValue = foundFeature.attributes[fieldName]
            val matchedCode = codedValues.find { domainValue ->
                valuesAreEqual(domainValue.code, currentValue)
            }

            _selectedFieldValue.value = matchedCode

            // clear any previous selection
            clearLayerSelections()

            // Select the identified feature on its layer
            (foundFeature.featureTable?.layer as? FeatureLayer)?.selectFeature(foundFeature)

            // Update the currently selected feature that the user is editing
            _selectedFeature.value = foundFeature

            _canClearSelection.value = true

            _statusMessage.value = "Select a new '$fieldAlias' value, then tap 'Apply.'"
            selectedFieldAlias(fieldAlias.toString())
        }
    }

    /**
     * Runs a simple downstream trace from the previously configured trace parameters.
     * Selects any features found by the trace.
     */
    fun trace() {
        viewModelScope.launch {
            clearLayerSelections() // Clear previous selections

            _statusMessage.value = "Running a downstream trace…"

            val params = traceParameters ?: return@launch handleError("Trace parameters not set.")

            val traceResults = utilityNetwork.trace(params).getOrElse {
                return@launch handleError("Trace failed", it.message + "\n" + it.cause)
            }

            val elementTraceResult = traceResults.firstOrNull {
                it is UtilityElementTraceResult
            } as? UtilityElementTraceResult ?: return@launch

            if (elementTraceResult.elements.isEmpty()) {
                _statusMessage.value = "Trace completed: 0 elements found."
                return@launch
            }

            // Group elements by which layer/table they belong to and select them.
            selectTraceResultElements(elementTraceResult.elements)

            _statusMessage.value =
                "Trace completed: ${elementTraceResult.elements.size} elements found."
        }
    }

    /**
     * Select all features that match the given list of utility elements found by the trace.
     */
    private suspend fun selectTraceResultElements(elements: List<UtilityElement>) {
        val selectedTraceGeometryList = mutableListOf<Geometry>()
        arcGISMap.operationalLayers.filterIsInstance<FeatureLayer>().forEach { layer ->
            val matchingElements = elements.filter { element ->
                element.networkSource.featureTable.tableName == layer.featureTable?.tableName
            }

            if (matchingElements.isNotEmpty()) {
                // Query the list of matching features from the network
                val featureResult = utilityNetwork
                    .getFeaturesForElements(matchingElements)
                    .getOrElse {
                        return handleError(
                            title = "Error retrieving features for trace result",
                            description = it.message + "\n" + it.cause
                        )
                    }
                //  Select the list of features returned from the query
                layer.selectFeatures(featureResult)
                // Zoom to the union geometry of all selected features
                selectedTraceGeometryList.addAll(featureResult.mapNotNull { it.geometry })
                Log.e("Added", "Added geometry: ${selectedTraceGeometryList.size}")
            }
        }

        // Calculate the union geometry of all the selected features
        val unionGeometry = GeometryEngine.unionOrNull(selectedTraceGeometryList)

        // Set the viewpoint of the map to the result
        unionGeometry?.let {
            mapViewProxy.setViewpointGeometry(boundingGeometry = it, paddingInDips = 20.0)
        }

        _canClearSelection.value = true
    }

    /**
     * Update the feature with the new coded value and apply the edit to the feature service.
     */
    fun applyEdits() {
        // Apply the local edits to the server
        _statusMessage.value = "Applying edits…"

        val feature = _selectedFeature.value ?: return

        val serviceFeatureTable = feature.featureTable as? ServiceFeatureTable
            ?: return handleError("Feature is not from a service feature table.")

        val newCode = _selectedFieldValue.value?.code
            ?: return handleError("No new coded value selected.")

        // Update the feature's attribute
        val fieldName = selectedField?.name ?: return
        _statusMessage.value = "Updating feature attribute…"
        feature.attributes[fieldName] = newCode

        viewModelScope.launch {
            serviceFeatureTable.updateFeature(feature).getOrElse {
                return@launch handleError("Failed to update feature", it.message + "\n" + it.cause)
            }

            val utilityNetworkServiceGeodatabase = serviceGeodatabase
                ?: return@launch handleError("ServiceGeodatabase not found.")

            val editResults = utilityNetworkServiceGeodatabase.applyEdits().getOrElse {
                return@launch handleError("Apply edits failed: ${it.message}")
            }

            // Check for any feature edit errors
            val hadErrors = editResults.any { tableEditResult ->
                tableEditResult.editResults.any { featureEditResult -> featureEditResult.completedWithErrors }
            }

            if (hadErrors) {
                _statusMessage.value = "Apply edits completed with error(s)."
            } else {
                _statusMessage.value = """
                        Edits applied successfully.
                        Tap "Get State" to see if the utility network is now dirty.
                    """.trimIndent()
                // Typically, once you've edited, you may want to validate the network again.
                _canValidateNetworkTopology.value = true
            }
        }
    }

    /**
     * Clear the selection on all FeatureLayers in the map.
     */
    private fun clearLayerSelections() {
        arcGISMap.operationalLayers.filterIsInstance<FeatureLayer>().forEach { layer ->
            layer.clearSelection()
        }
    }

    /**
     * Compare two attribute values for equality, accounting for domain code types.
     */
    private fun valuesAreEqual(lhs: Any?, rhs: Any?): Boolean {
        // Basic checks
        if (lhs == null && rhs == null) return true
        if (lhs == null || rhs == null) return false

        // Convert to string, Int, or Double for some common domain code use-cases
        return when (lhs) {
            is Number -> lhs.toDouble() == (rhs as? Number)?.toDouble()
            else -> lhs == rhs
        }
    }

    fun updateSelectedValue(codedValue: CodedValue) {
        _selectedFieldValue.value = codedValue
    }

    fun updateVisibleArea(polygon: Polygon) {
        _currentVisibleArea.value = polygon.extent
        Log.e("VISIBLEAREA", "Current visible area updated.")
    }

    private fun handleError(title: String, description: String = "") {
        reset()
        // _traceState.value = TraceState.TRACE_FAILED
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
        //_traceState.value = TraceState.ADD_STARTING_POINT
        _canTrace.value = false
        //_selectedTraceType.value = UtilityTraceType.Connected
        //_selectedTerminalConfigurationIndex.value = null
        //_selectedPointType.value = PointType.Start
        //_terminalConfigurationOptions.value = listOf()
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

        private const val INIT_STATUS_MESSAGE = """
                Utility network loaded.
                Tap on a feature to edit its domain-coded field.
                Then tap "Apply" to update the value on the server.
                Use "Get State" to see if validating is needed or if tracing is available.
                """
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