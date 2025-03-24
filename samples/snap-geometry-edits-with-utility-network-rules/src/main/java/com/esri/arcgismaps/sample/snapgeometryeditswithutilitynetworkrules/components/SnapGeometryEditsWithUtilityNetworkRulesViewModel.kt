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

package com.esri.arcgismaps.sample.snapgeometryeditswithutilitynetworkrules.components

import android.app.Application
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.data.GeodatabaseFeatureTable
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.FeatureTilingMode
import com.arcgismaps.mapping.layers.SubtypeFeatureLayer
import com.arcgismaps.mapping.layers.SubtypeSublayer
import com.arcgismaps.mapping.symbology.Renderer
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.symbology.Symbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.mapping.view.geometryeditor.ReticleVertexTool
import com.arcgismaps.mapping.view.geometryeditor.SnapRuleBehavior
import com.arcgismaps.mapping.view.geometryeditor.SnapRules
import com.arcgismaps.mapping.view.geometryeditor.SnapSourceEnablingBehavior
import com.arcgismaps.mapping.view.geometryeditor.SnapSourceSettings
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.utilitynetworks.UtilityAssetType
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.snapgeometryeditswithutilitynetworkrules.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SnapGeometryEditsWithUtilityNetworkRulesViewModel(application: Application) : AndroidViewModel(application) {

    // Define the map view proxy and map with basemap and initial extent
    val mapViewProxy = MapViewProxy()

    val arcGISMap = ArcGISMap(basemapStyle = BasemapStyle.ArcGISStreetsNight).apply {
        // Geodatabase layers are always full extent, however if using feature service layers, we
        // must ensure that tiles use full resolution in order to snap to features
        loadSettings.featureTilingMode = FeatureTilingMode.EnabledWithFullResolutionWhenSupported
        initialViewpoint = Viewpoint(
            center = Point(-9811055.156028448, 5131792.19502501, SpatialReference.webMercator()),
            scale = 1e4
        )
    }

    // Get the file path of the geodatabase file
    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator +
                application.getString(R.string.snap_geometry_edits_with_utility_network_rules_app_name)
    }
    private val filePath = provisionPath + application.getString(R.string.naperville_geodatabase)

    // Create the mobile map package
    private val geodatabase = Geodatabase(filePath)

    // Hold references to the subtype sublayers for the distribution and service pipe layers
    private var distributionPipeLayer: SubtypeSublayer? = null
    private var servicePipeLayer: SubtypeSublayer? = null
    private var pipelineLayerName = application.getString(R.string.pipeline_layer_name)
    private var distributionPipeName = application.getString(R.string.distribution_pipe_name)
    private var servicePipeLayerName = application.getString(R.string.service_pipe_layer_name)

    // Message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // Symbols to help visualize the snap rules behaviors
    private val rulesPreventSymbol: Symbol =
        SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.red, 4f)
    private val rulesLimitSymbol: Symbol =
        SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.fromRgba(255, 165, 0 ), 3f)
    private val noneSymbol: Symbol =
        SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color.green, 3f)
    private val symbols = mutableMapOf(
        SnapRuleBehavior.RulesPreventSnapping to rulesPreventSymbol,
        SnapRuleBehavior.RulesLimitSnapping to rulesLimitSymbol,
        SnapRuleBehavior.None to noneSymbol
    )
    private val symbolSwatches = mutableMapOf<SnapRuleBehavior, BitmapDrawable>()

    // Save default renderers to reset the layers when a feature selection is cleared
    private var defaultDistributionRenderer: Renderer? = null
    private var defaultServiceRenderer: Renderer? = null

    // Define a geometry editor with a snapping enabled. A reticle tool is ideal for touch devices.
    val geometryEditor = GeometryEditor().apply {
        snapSettings.isEnabled = true
        snapSettings.isFeatureSnappingEnabled = true
        tool = ReticleVertexTool()
    }

    // Define a graphics overlay which can also act as a snap source
    private val defaultGraphicRenderer = SimpleRenderer(SimpleLineSymbol(
        SimpleLineSymbolStyle.Dash, Color.fromRgba(165, 165, 165), 3f))
    val graphicsOverlay = GraphicsOverlay(
        graphics = listOf(Graphic(Geometry.fromJsonOrNull(application.getString(R.string.graphic_geometry_json))))
    ).apply {
        id = application.getString(R.string.graphics_overlay_id)
        renderer = defaultGraphicRenderer
    }

    // Hold a reference to a selected feature and its related asset group and type
    private var selectedFeature: ArcGISFeature? = null

    private val _assetGroupNameState = MutableStateFlow("<Nothing selected>")
    var assetGroupNameState: StateFlow<String> = _assetGroupNameState

    private val _assetTypeNameState = MutableStateFlow("<Nothing selected>")
    val assetTypeNameState: StateFlow<String> = _assetTypeNameState

    // Represents the snap source settings object (for enabling and disabling), along with a name
    // to use in the UI, and a symbol swatch
    data class SnapSourceProperty(
        val name: String,
        val swatch: BitmapDrawable,
        val snapSourceSettings: SnapSourceSettings
    )
    private val _snapSourcePropertyList = MutableStateFlow(listOf<SnapSourceProperty>())
    internal val snapSourcePropertyList: StateFlow<List<SnapSourceProperty>> =
        _snapSourcePropertyList

    // Create boolean flags to track the state of UI components
    private val _isEditButtonEnabled = MutableStateFlow(false)
    internal val isEditButtonEnabled = _isEditButtonEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map",
                    error.message.toString()
                )
            }

            // Load the mobile map package
            geodatabase.load().onSuccess {
                // Add layers from the geodatabase to the map
                addLayersToMapFromGeodatabase(application)

                // Set the utility network on the map and load it
                arcGISMap.utilityNetworks.add(geodatabase.utilityNetworks.first())
                arcGISMap.utilityNetworks.first().load().onFailure {
                    messageDialogVM.showMessageDialog(
                        "Error loading utility network", it.message.toString()
                    )
                }

                // Set up symbol swatches
                symbolSwatches[SnapRuleBehavior.RulesPreventSnapping] = createSwatch(rulesPreventSymbol)
                symbolSwatches[SnapRuleBehavior.RulesLimitSnapping] = createSwatch(rulesLimitSymbol)
                symbolSwatches[SnapRuleBehavior.None] = createSwatch(noneSymbol)
            }.onFailure {
                messageDialogVM.showMessageDialog(
                    "Error loading geodatabase", it.message.toString()
                )
            }
        }
    }

    /**
     * Start the geometry editor to edit the geometry of the selected feature.
     */
    fun editFeatureGeometry() {
        // Get the symbol for the selected feature
        selectedFeature?.let { feature ->

            // Use the symbol from the selected feature in the style of the geometry editor tool
            val symbol =
                (feature.featureTable as GeodatabaseFeatureTable).layerInfo?.drawingInfo?.renderer?.getSymbol(
                    feature
                )
            geometryEditor.tool.style.apply {
                vertexSymbol = symbol
                feedbackVertexSymbol = symbol
                selectedVertexSymbol = symbol
                vertexTextSymbol = null
            }

            // Hide the selected feature
            if (feature.featureTable?.layer is FeatureLayer) {
                (feature.featureTable!!.layer as FeatureLayer).setFeatureVisible(feature, false)
            }

            // Start the geometry editor and center the map underneath the reticle
            feature.geometry?.let { initialGeometry ->
                viewModelScope.launch {
                    mapViewProxy.setViewpointCenter(initialGeometry.extent.center)
                }
                geometryEditor.start(initialGeometry)
                geometryEditor.selectVertex(0,0)
            }
        }
    }

    /**
     * Stop the geometry editor and discard the changes made to the geometry.
     */
    fun discardGeometryChanges() {
        // Discard the current edit
        geometryEditor.stop()

        // Reset the selection
        resetSelections()
    }

    /**
     * Stop the geometry editor, and update the previously identified feature with the new geometry.
     */
    fun saveGeometryChanges() {
        // Stop the geometry editor and get the updated geometry
        val finalGeometry = geometryEditor.stop()

        // Update the feature with the new geometry
        selectedFeature?.let { feature ->
            feature.geometry = finalGeometry

            viewModelScope.launch {
                (feature.featureTable as GeodatabaseFeatureTable).updateFeature(feature)
                    .onFailure { error ->
                        messageDialogVM.showMessageDialog(
                            "Error updating feature",
                            error.message.toString()
                        )
                    }
            }
        }

        // Reset the selection
        resetSelections()
    }

    /**
     * Identifies the tapped screen coordinate in the provided [singleTapConfirmedEvent] and gets
     * the asset at that location.
     */
    fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) {

        if (geometryEditor.isStarted.value || arcGISMap.operationalLayers.isEmpty()) {
            return
        }
        viewModelScope.launch {
            mapViewProxy.identifyLayers(
                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                tolerance = 12.dp,
                returnPopupsOnly = false,
                maximumResults = 1
            ).onSuccess { identifyResultList ->
                // As we are using subtype feature layers in this sample the returned features are
                // contained in the sublayer results
                val identifiedFeature = identifyResultList.firstOrNull()?.sublayerResults?.firstOrNull()?.geoElements?.firstOrNull()
                if (identifiedFeature !is ArcGISFeature) {
                    resetSelections()
                    return@launch
                }

                // In this sample we only allow selection of point features. If the identified
                // feature is null or the feature is not a point feature then reset and return.
                if (identifiedFeature.featureTable?.geometryType != GeometryType.Point) {
                    resetSelections()
                    return@launch
                } else if (
                    selectedFeature != null
                    && identifiedFeature != selectedFeature
                    && selectedFeature?.featureTable?.layer is FeatureLayer
                ) {
                    // If a feature is already selected and the tapped feature is not the selected
                    // feature then clear the previous selection
                    (selectedFeature?.featureTable?.layer as FeatureLayer).clearSelection()
                }

                // Update the selected feature and select it on the layer
                selectedFeature = identifiedFeature
                selectedFeature?.let { feature ->
                    (feature.featureTable?.layer as FeatureLayer).selectFeature(feature)

                    // Create a utility element for the selected feature using the utility network
                    arcGISMap.utilityNetworks.first().createElementOrNull(feature)?.let { element ->
                        // Update values for UI based on the selected feature
                        _assetGroupNameState.value = element.assetGroup.name
                        _assetTypeNameState.value = element.assetType.name
                        _isEditButtonEnabled.value = true
                        setSnapSettings(element.assetType)
                    } ?: run {
                        messageDialogVM.showMessageDialog("Error creating UtilityElement")
                        return@launch
                    }
            }
        }
    }

    /**
     * Creates SnapRules based on the given asset type, synchronizes the snap sources using these
     * rules, then updates the snap sources list used by the UI
     */
    private suspend fun setSnapSettings(assetType: UtilityAssetType) {
        // Get the snap rules associated with the asset type
        val snapRules = SnapRules.create(arcGISMap.utilityNetworks.first(), assetType).getOrThrow()

        geometryEditor.snapSettings.apply {
            // Synchronize the snap source collection with the map's operational layers using the snap
            // rules. Setting SnapSourceEnablingBehavior.SetFromRules will enable snapping for the
            // layers and sublayers specified in the snap rules.
            syncSourceSettings(snapRules, SnapSourceEnablingBehavior.SetFromRules)

            // Enable snapping for the graphics overlay as this will not be affected by the given
            // SnapSourceEnablingBehavior.setFromRules
            sourceSettings.first { sss -> sss.source == graphicsOverlay }.isEnabled = true
        }

        updateSnapSourceList()
    }


    /**
     * Updates the enabled value of the SnapSourceSettings object at the given index and rebuilds
     * the snap source list.
     */
    fun setSnapSourceCheckedValue(checkedValue: Boolean, index: Int) {
        // Set new value into appropriate property via index
        _snapSourcePropertyList.value[index].snapSourceSettings.isEnabled = checkedValue

        updateSnapSourceList()
    }

    /*
     * Updates the lists used by the UI to show SnapSourceSettings information.
    */
    private fun updateSnapSourceList() {
        // Get a new list of current properties from snapSettings
        val newSnapSourceList = currentSnapSourcePropertyList()

        // Update the backing list
        _snapSourcePropertyList.value = newSnapSourceList
    }

    /**
     * Returns a list of SnapSourceProperty objects based on the current snap sources.
     */
    private fun currentSnapSourcePropertyList(): List<SnapSourceProperty> {
        return buildList {
            geometryEditor.snapSettings.sourceSettings.forEach { sourceSettings ->
                when (sourceSettings.source) {
                    is GraphicsOverlay -> {
                        symbolSwatches[sourceSettings.ruleBehavior]?.let { swatch ->
                            add(SnapSourceProperty((sourceSettings.source as GraphicsOverlay).id, swatch, sourceSettings))
                        }

                        // Set the appropriate symbol for the layer based on the SnapRuleBehavior.
                        graphicsOverlay.renderer = SimpleRenderer(symbols[sourceSettings.ruleBehavior])
                    }
                    is SubtypeFeatureLayer -> {
                        sourceSettings.childSourceSettings.forEach { childSourceSettings ->
(childSourceSettings.source as? SubtypeSublayer)?.let { childSource ->
                                when (childSource.name) {
                                    distributionPipeName, servicePipeLayerName -> {
                                        symbolSwatches[childSourceSettings.ruleBehavior]?.let { swatch ->
                                            add(SnapSourceProperty(childSource.name, swatch, childSourceSettings))
                                        }

                                        // Set the appropriate symbol for the sublayer based on the SnapRuleBehavior.
                                        childSource.renderer =
                                            SimpleRenderer(symbols[childSourceSettings.ruleBehavior])
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }
        }
    }

    /**
     * Clears the selection on the layer and reinstates feature visibility, then resets the selected
     * feature and layer and any UI backing variables.
     */
    private fun resetSelections() {
        // Clear the existing selection and show the selected feature                                      
        (selectedFeature?.featureTable?.layer as? FeatureLayer)?.clearSelection()                          
        (selectedFeature?.featureTable?.layer as? FeatureLayer)?.setFeatureVisible(selectedFeature!!, true)

        // Reset the selected feature and layer
        selectedFeature = null
        _assetGroupNameState.value = "<Nothing selected>"
        _assetTypeNameState.value = "<Nothing selected>"
        _isEditButtonEnabled.value = false

        // Revert back to the default renderer for the distribution and service pipe layers and
        // graphics overlay
        distributionPipeLayer?.renderer = defaultDistributionRenderer
        servicePipeLayer?.renderer = defaultServiceRenderer
        graphicsOverlay.renderer = defaultGraphicRenderer

        // Clear the snap sources list
        _snapSourcePropertyList.value = emptyList()
    }

    /**
     * Adds required layers from the geodatabase to the map, setting the visibility of sublayers to
     * show only a small subset in order to avoid too much visual clutter.
     */
    private suspend fun addLayersToMapFromGeodatabase(application: Application) {
        // Only show the Distribution Pipe and Service Pipe sublayers in the pipeline layer. Also
        // store the default renderer for the these sublayers.
        val pipeLayer = SubtypeFeatureLayer(
            geodatabase.getFeatureTable(pipelineLayerName) as ArcGISFeatureTable
        )
        pipeLayer.load().getOrElse {
            messageDialogVM.showMessageDialog(
                "Error loading pipeline layer",
                it.message.toString()
            )
        }
        val distributionPipeName = application.getString(R.string.distribution_pipe_name)
        val servicePipeLayerName = application.getString(R.string.service_pipe_layer_name)
        pipeLayer.subtypeSublayers.forEach { sublayer ->
            when (sublayer.name) {
                distributionPipeName -> {
                    distributionPipeLayer = sublayer
                    defaultDistributionRenderer = sublayer.renderer
                }
                servicePipeLayerName -> {
                    servicePipeLayer = sublayer
                    defaultServiceRenderer = sublayer.renderer
                }
                else -> {
                    // Hide all other sublayers
                    sublayer.isVisible = false
                }
            }
        }
        arcGISMap.operationalLayers.add(pipeLayer)

        // Only show the Excess Flow Valve and Controllable Tee sublayers in the device layer
        val deviceLayer = SubtypeFeatureLayer(
            geodatabase.getFeatureTable(
                application.getString(R.string.device_layer_name)
            ) as ArcGISFeatureTable
        )
        deviceLayer.load().getOrThrow()
        val excessFlowValveName = application.getString(R.string.excess_flow_valve_name)
        val controllableTeeName = application.getString(R.string.controllable_tee_name)
        deviceLayer.subtypeSublayers.filter { sublayer ->
            sublayer.name != excessFlowValveName && sublayer.name != controllableTeeName
        }.forEach { sublayerToHide ->
            sublayerToHide.isVisible = false
        }
        arcGISMap.operationalLayers.add(deviceLayer)

        // Add the junction layer to the map
        arcGISMap.operationalLayers.add(
            SubtypeFeatureLayer(
                geodatabase.getFeatureTable(
                    application.getString(R.string.junction_layer_name)
                ) as ArcGISFeatureTable
            )
        )
    }

    /**
     * Create a swatch from the given symbol.
     */
    private suspend fun createSwatch(symbol: Symbol): BitmapDrawable {
        // Create a swatch from the symbol
        val swatch = symbol.createSwatch(
            screenScale = 30.0f,
            width = 4.0f,
            height = 4.0f//,
        ).getOrThrow()

        return swatch
    }
}
