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

package com.esri.arcgismaps.sample.managefeatures.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.LoadStatus
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.CodedValueDomain
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.data.ServiceGeodatabase
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch


class ManageFeaturesViewModel(application: Application) : AndroidViewModel(application) {

    val mapViewProxy = MapViewProxy()

    // Hold a reference to the feature table.
    private var damageFeatureTable: ServiceFeatureTable? = null

    // Hold a reference to the feature layer.
    private var damageLayer: FeatureLayer? = null

    // Hold a reference to the selected feature.
    var selectedFeature: ArcGISFeature? by mutableStateOf(null)

    // The current feature operation to perform.
    var currentFeatureOperation by mutableStateOf(FeatureOperationType.CREATE)

    // The list of damage types to update the feature attribute.
    var damageTypeList: List<String> = mutableListOf()

    var currentDamageType by mutableStateOf("")

    // Create the map with streets basemap.
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
        // Zoom to the United States.
        initialViewpoint = Viewpoint(
            Point(x = -10800000.0, y = 4500000.0, spatialReference = SpatialReference.webMercator()), scale = 3e7
        )
    }

    // Create a snackbar message to display the result of feature operations.
    var snackBarMessage: String by mutableStateOf("")

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            // Create a service geodatabase from the feature service.
            val serviceGeodatabase =
                ServiceGeodatabase("https://sampleserver6.arcgisonline.com/arcgis/rest/services/DamageAssessment/FeatureServer/0")
            serviceGeodatabase.load().onSuccess {
                // Get the feature table from the service geodatabase referencing the Damage Assessment feature service.
                serviceGeodatabase.getTable(0)?.let { serviceFeatureTable ->
                    // Load the feature table to get the coded value domain for the attribute field.
                    serviceFeatureTable.load().onSuccess {
                        // Hold a reference to the feature table.
                        damageFeatureTable = serviceFeatureTable
                        // Get the field from the feature table that will be updated.
                        val typeDamageField = serviceFeatureTable.fields.first { it.name == "typdamage" }
                        // Get the coded value domain for the field.
                        val attributeDomain = typeDamageField.domain as CodedValueDomain
                        // Add the damage types to the list.
                        attributeDomain.codedValues.forEach {
                            damageTypeList += it.name
                        }
                        // Create a feature layer to visualize the features in the table.
                        FeatureLayer.createWithFeatureTable(serviceFeatureTable).let { featureLayer ->
                            // Hold a reference to the feature layer.
                            damageLayer = featureLayer
                            // Add it to the map.
                            arcGISMap.operationalLayers.add(featureLayer)
                            // Load the map.
                            arcGISMap.load().onFailure { error ->
                                messageDialogVM.showMessageDialog(
                                    "Failed to load map", error.message.toString()
                                )
                            }
                        }
                    }.onFailure { error ->
                        messageDialogVM.showMessageDialog(
                            "Failed to load feature table", error.message.toString()
                        )
                    }
                }
            }.onFailure { error ->
                // Show the message dialog and pass the error message to be displayed in the dialog.
                messageDialogVM.showMessageDialog(
                    "Failed to load service geodatabase", error.message.toString()
                )
            }
        }
    }

    /**
     * Directs the behaviour of tap's on the map view.
     */
    fun onTap(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        if (damageLayer?.loadStatus?.value != LoadStatus.Loaded) {
            snackBarMessage = "Layer not loaded!"
            return
        }
        when (currentFeatureOperation) {
            FeatureOperationType.CREATE -> createFeatureAt(singleTapConfirmedEvent.screenCoordinate)
            FeatureOperationType.DELETE -> deleteFeatureAt(singleTapConfirmedEvent.screenCoordinate)
            FeatureOperationType.UPDATE_ATTRIBUTE -> selectFeatureForAttributeEditAt(singleTapConfirmedEvent.screenCoordinate)
            FeatureOperationType.UPDATE_GEOMETRY -> updateFeatureGeometryAt(singleTapConfirmedEvent.screenCoordinate)
        }
    }

    /**
     * Set the current feature operation to perform based on the selected index from the dropdown. Also, reset feature
     * selection.
     */
    fun onFeatureOperationSelected(index: Int) {
        currentFeatureOperation = FeatureOperationType.entries[index]
        // Reset the selected feature when the operation changes.
        damageLayer?.clearSelection()
        selectedFeature = null
    }

    /**
     * Create a new feature at the tapped location with some default attributes
     */
    private fun createFeatureAt(screenCoordinate: ScreenCoordinate) {
        // Create the feature.
        val feature = damageFeatureTable?.createFeature()?.apply {
            // Get the normalized geometry for the tapped location and use it as the feature's geometry.
            mapViewProxy.screenToLocationOrNull(screenCoordinate)?.let { mapPoint ->
                geometry = GeometryEngine.normalizeCentralMeridian(mapPoint)
                // Set feature attributes.
                attributes["typdamage"] = "Minor"
                attributes["primcause"] = "Earthquake"
            }
        }
        feature?.let {
            viewModelScope.launch {
                // Add the feature to the table.
                damageFeatureTable?.addFeature(it)
                // Apply the edits to the service on the service geodatabase.
                damageFeatureTable?.serviceGeodatabase?.applyEdits()
                // Update the feature to get the updated objectid - a temporary ID is used before the feature is added.
                it.refresh()
                // Confirm feature addition.
                snackBarMessage = "Created feature ${it.attributes["objectid"]}"
            }
        }
    }

    /**
     * Selects a feature at the tapped location in preparation for deletion.
     */
    private fun deleteFeatureAt(screenCoordinate: ScreenCoordinate) {
        damageLayer?.let { damageLayer ->
            // Clear any existing selection.
            damageLayer.clearSelection()
            selectedFeature = null
            viewModelScope.launch {
                // Determine if a user tapped on a feature.
                mapViewProxy.identify(damageLayer, screenCoordinate, 10.dp).onSuccess { identifyResult ->
                    selectedFeature = (identifyResult.geoElements.firstOrNull() as? ArcGISFeature)?.also {
                        damageLayer.selectFeature(it)
                    }
                }
            }
        }
    }

    /**
     * Delete the selected feature from the feature table and service geodatabase.
     */
    fun deleteSelectedFeature() {
        selectedFeature?.let {
            // Get the feature's object id.
            val featureId = it.attributes["objectid"] as Long
            viewModelScope.launch {
                // Delete the feature from the feature table.
                damageFeatureTable?.deleteFeature(it)?.onSuccess {
                    snackBarMessage = "Deleted feature $featureId"
                }?.onFailure {
                    snackBarMessage = "Failed to delete feature $featureId"
                }
                // Apply the edits to the service geodatabase.
                damageFeatureTable?.serviceGeodatabase?.applyEdits()
                selectedFeature = null
            }
        }
    }

    /**
     * Selects a feature at the tapped location in preparation for attribute editing.
     */
    private fun selectFeatureForAttributeEditAt(screenCoordinate: ScreenCoordinate) {
        damageLayer?.let { damageLayer ->
            // Clear any existing selection.
            damageLayer.clearSelection()
            selectedFeature = null
            viewModelScope.launch {
                // Determine if a user tapped on a feature.
                mapViewProxy.identify(damageLayer, screenCoordinate, 10.dp).onSuccess { identifyResult ->
                    // Get the identified feature.
                    val identifiedFeature = identifyResult.geoElements.firstOrNull() as? ArcGISFeature
                    identifiedFeature?.let {
                        val currentAttributeValue = it.attributes["typdamage"] as String
                        currentDamageType = currentAttributeValue
                        selectedFeature = it.also {
                            damageLayer.selectFeature(it)
                        }
                    } ?: run {
                        // Clear the selected feature if no feature is found.
                        damageLayer.clearSelection()
                        selectedFeature = null
                        currentDamageType = ""
                    }
                }
            }
        }
    }

    /**
     * Update the attribute value of the selected feature to the new value from the new damage type.
     */
    fun onDamageTypeSelected(index: Int) {
        // Get the new value.
        currentDamageType = damageTypeList[index]
        selectedFeature?.let { selectedFeature ->
            viewModelScope.launch {
                // Load the feature.
                selectedFeature.load().onSuccess {
                    // Update the attribute value.
                    selectedFeature.attributes["typdamage"] = currentDamageType
                    // Update the table.
                    damageFeatureTable?.updateFeature(selectedFeature)
                    // Update the service on the service geodatabase.
                    damageFeatureTable?.serviceGeodatabase?.applyEdits()?.onSuccess {
                        snackBarMessage =
                            "Updated feature ${selectedFeature.attributes["objectid"]} to $currentDamageType"
                    }
                }
            }
        }
    }

    /**
     * Select a feature, if none is selected. If a feature is selected, update its geometry to the tapped location.
     */
    private fun updateFeatureGeometryAt(screenCoordinate: ScreenCoordinate) {

        damageLayer?.let { damageLayer ->
            when (selectedFeature) {
                // When no feature is selected.
                null -> {
                    viewModelScope.launch {
                        // Determine if a user tapped on a feature.
                        mapViewProxy.identify(damageLayer, screenCoordinate, 10.dp).onSuccess { identifyResult ->
                            // Get the identified feature.
                            val identifiedFeature = identifyResult.geoElements.firstOrNull() as? ArcGISFeature
                            identifiedFeature?.let {
                                selectedFeature = it.also {
                                    damageLayer.selectFeature(it)
                                }
                            }
                        }
                    }
                }
                // When a feature is selected, update its geometry to the tapped location.
                else -> {
                    mapViewProxy.screenToLocationOrNull(screenCoordinate)?.let { mapPoint ->
                        // Normalize the point - needed when the tapped location is over the international date line.
                        val destinationPoint = GeometryEngine.normalizeCentralMeridian(mapPoint)
                        viewModelScope.launch {
                            selectedFeature?.let { selectedFeature ->
                                // Load the feature.
                                selectedFeature.load().onSuccess {
                                    // Update the geometry of the selected feature.
                                    selectedFeature.geometry = destinationPoint
                                    // Apply the edit to the feature table.
                                    damageFeatureTable?.updateFeature(selectedFeature)
                                    // Push the update to the service with the service geodatabase.
                                    damageFeatureTable?.serviceGeodatabase?.applyEdits()?.onSuccess {
                                        snackBarMessage = "Moved feature ${selectedFeature.attributes["objectid"]}"
                                    }?.onFailure {
                                        snackBarMessage =
                                            "Failed to move feature ${selectedFeature.attributes["objectid"]}"
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

enum class FeatureOperationType(val operationName: String, val instruction: String) {
    CREATE("Create feature", "Tap on the map to create a new feature."),
    DELETE("Delete feature", "Select an existing feature to delete it."),
    UPDATE_ATTRIBUTE("Update attribute", "Select an existing feature to edit its attribute."),
    UPDATE_GEOMETRY("Update geometry", "Select an existing feature and tap the map to move it to a new position.")
}
