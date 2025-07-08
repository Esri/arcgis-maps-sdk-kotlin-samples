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

package com.esri.arcgismaps.sample.editfeatureswithfeaturelinkedannotation.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.AnnotationLayer
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.editfeatureswithfeaturelinkedannotation.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class EditFeaturesWithFeatureLinkedAnnotationViewModel(app: Application) : AndroidViewModel(app) {

    // Path to the geodatabase in the app's external files dir
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() + File.separator + app.getString(R.string.edit_features_with_feature_linked_annotation_app_name)
    }

    // Geodatabase and layer names
    private val geodatabaseFileName = "loudoun_anno.geodatabase"
    private val addressPointsTableName = "Loudoun_Address_Points_1"
    private val addressAnnoTableName = "Loudoun_Address_PointsAnno_1"
    private val parcelLinesTableName = "ParcelLines_1"
    private val parcelAnnoTableName = "ParcelLinesAnno_1"

    private val addressNumberAttribute = "AD_ADDRESS"
    private val streetNameAttribute = "ST_STR_NAM"

    // Map centered on Loudoun County, VA
    var arcGISMap = ArcGISMap(BasemapStyle.ArcGISLightGray).apply {
        initialViewpoint = Viewpoint(39.0204, -77.4159, 2256.0)
    }

    // Proxy for identify operations with the map view
    val mapViewProxy = MapViewProxy()

    // ViewModel for showing messages
    val messageDialogVM = MessageDialogViewModel()

    // UI state
    private val _instruction = MutableStateFlow<Instruction>(Instruction.SelectFeature)
    val instruction = _instruction.asStateFlow()

    // Currently selected feature (point or polyline)
    var selectedFeature: ArcGISFeature? by mutableStateOf(null)
        private set

    // Flag to show/hide the edit address dialog
    var showEditAddressDialog: Boolean by mutableStateOf(false)
        private set

    // For editing address
    var buildingNumber: Int by mutableIntStateOf(0)
        private set
    var streetName: String by mutableStateOf("")
        private set

    // Geodatabase and layers
    private var loudonGeodatabase: Geodatabase? = null
    private var addressFeatureLayer: FeatureLayer? = null
    private var parcelFeatureLayer: FeatureLayer? = null

    init {
        viewModelScope.launch {
            loadGeodatabaseAndLayers()
        }
    }

    /**
     * Load the geodatabase and its feature/annotation layers.
     * If the geodatabase is not found, show an error message.
     */
    private suspend fun loadGeodatabaseAndLayers() {
        // Check if the geodatabase file exists
        val gdbFile = File(provisionPath, geodatabaseFileName)
        if (!gdbFile.exists()) {
            messageDialogVM.showMessageDialog("Geodatabase not found", "Expected at: ${gdbFile.path}")
            return
        }
        // Create and load the geodatabase
        val geodatabase = Geodatabase(gdbFile.path)
        loudonGeodatabase = geodatabase
        geodatabase.load().onFailure {
            messageDialogVM.showMessageDialog(it)
            return
        }
        // Get feature tables from the geodatabase
        val addressTable = geodatabase.featureTables.find { it.tableName == addressPointsTableName }
        val parcelTable = geodatabase.featureTables.find { it.tableName == parcelLinesTableName }
        // Create feature layers from the tables
        addressFeatureLayer = addressTable?.let { FeatureLayer.createWithFeatureTable(it) }
        parcelFeatureLayer = parcelTable?.let { FeatureLayer.createWithFeatureTable(it) }
        // Get annotation tables from the geodatabase
        val addressAnnotationTable = geodatabase.annotationTables.find { it.tableName == addressAnnoTableName }
        val parcelAnnotationTable = geodatabase.annotationTables.find { it.tableName == parcelAnnoTableName }
        // Create annotation layers from the tables
        val addressAnnotationLayer = addressAnnotationTable?.let { AnnotationLayer(it) }
        val parcelAnnotationLayer = parcelAnnotationTable?.let { AnnotationLayer(it) }
        // Add layers to map
        arcGISMap.operationalLayers.clear()
        listOfNotNull(addressFeatureLayer, parcelFeatureLayer, addressAnnotationLayer, parcelAnnotationLayer).forEach {
            arcGISMap.operationalLayers.add(it)
        }
    }

    /**
     * Handle single tap on the map.
     * If a feature is already selected, treat as move confirmation.
     * Otherwise, identify the feature at the tapped location.
     */
    fun onMapSingleTap(screenCoordinate: ScreenCoordinate, mapPoint: Point) {
        // If a feature is already selected, treat as move confirmation
        if (selectedFeature != null) {
            // Only allow move for point or straight polyline
            val feature = selectedFeature ?: return
            if (feature.geometry is Point || isStraightPolyline(feature.geometry)) {
                onMoveFeatureConfirmed(feature, mapPoint)
            } else {
                clearSelection()
                _instruction.value = Instruction.SelectFeature
            }
            return
        }
        // Otherwise, identify feature at tap
        viewModelScope.launch {
            val results = mapViewProxy.identifyLayers(
                screenCoordinate = screenCoordinate, tolerance = 10.dp, returnPopupsOnly = false, maximumResults = 1
            ).getOrElse {
                messageDialogVM.showMessageDialog(it)
                return@launch
            }
            selectFirstFeature(results)
        }
    }

    /**
     * Select the first feature from the identify results.
     * If it's a point, show the edit address dialog.
     * If it's a straight polyline, allow moving it.
     * Otherwise, clear selection and show instruction to select a feature.
     */
    private fun selectFirstFeature(results: List<IdentifyLayerResult>) {
        clearSelection()
        // Prefer feature layers only
        val featureLayerResult = results.firstOrNull {
            it.layerContent is FeatureLayer && it.geoElements.isNotEmpty()
        } ?: return
        val featureLayer = featureLayerResult.layerContent as? FeatureLayer ?: return
        val feature = featureLayerResult.geoElements.firstOrNull() as? ArcGISFeature ?: return
        featureLayer.selectFeature(feature)
        selectedFeature = feature
        when (feature.geometry) {
            is Point -> {
                showEditAddressDialog = true
                // Prepare address editing
                val adAddress = feature.attributes[addressNumberAttribute] as Int
                val stStrNam = feature.attributes[streetNameAttribute]?.toString() ?: ""
                buildingNumber = adAddress
                streetName = stStrNam
                selectedFeature = feature
                _instruction.value = Instruction.MoveFeature
            }
            is Polyline -> {
                if (isStraightPolyline(feature.geometry)) {
                    _instruction.value = Instruction.MoveFeature
                } else {
                    // Only allow straight polylines
                    _instruction.value = Instruction.SelectStraightPolyline
                    clearSelection()
                }
            }
            else -> {
                _instruction.value = Instruction.SelectFeature
                clearSelection()
            }
        }
    }

    /**
     * Handle confirmation of address editing.
     * Update the feature's attributes and keep it selected so that it can be moved.
     */
    fun onEditAddressConfirmed(feature: ArcGISFeature?, newBuildingNumber: Int, newStreetName: String) {
        viewModelScope.launch {
            try {
                feature?.let { feature ->
                    feature.attributes[addressNumberAttribute] = newBuildingNumber
                    feature.attributes[streetNameAttribute] = newStreetName
                    feature.featureTable?.updateFeature(feature)?.onFailure { error ->
                        messageDialogVM.showMessageDialog(error)
                    }
                    // Keep the feature selected and update the instruction
                    buildingNumber = newBuildingNumber
                    streetName = newStreetName
                    _instruction.value = Instruction.MoveFeature
                }
                // Close the dialog
                showEditAddressDialog = false
            } catch (e: Exception) {
                messageDialogVM.showMessageDialog(e)
            }
        }
    }

    fun onMoveFeatureConfirmed(feature: ArcGISFeature, mapPoint: Point) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (val geom = feature.geometry) {
                    is Point -> {
                        feature.geometry = mapPoint
                    }
                    is Polyline -> {
                        // Move nearest vertex to tapPoint
                        val polyline = geom
                        val spatialRef = polyline.spatialReference ?: SpatialReference.wgs84()
                        val projectedTap = GeometryEngine.projectOrNull(mapPoint, spatialRef) ?: mapPoint
                        val nearestVertex = GeometryEngine.nearestVertex(polyline, projectedTap)
                        if (nearestVertex != null) {
                            val builder = PolylineBuilder(polyline)
                            val partIdx = nearestVertex.partIndex
                            val pointIdx = nearestVertex.pointIndex
                            builder.parts[partIdx].setPoint(pointIdx, projectedTap)
                            feature.geometry = builder.toGeometry()
                        }
                    }
                    else -> { }
                }
                feature.featureTable?.updateFeature(feature)?.onFailure {
                    messageDialogVM.showMessageDialog(it)
                }
                clearSelection()
                _instruction.value = Instruction.SelectFeature
            } catch (e: Exception) {
                messageDialogVM.showMessageDialog(e)
            }
        }
    }

    /**
     * Clear the selection and reset the selected feature.
     */
    fun clearSelection() {
        selectedFeature?.let { feature ->
            val layer = arcGISMap.operationalLayers.filterIsInstance<FeatureLayer>().firstOrNull {
                it.featureTable == feature.featureTable
            }
            layer?.clearSelection()
        }
        selectedFeature = null
        buildingNumber = 0
        streetName = ""
    }

    /**
     * Check if the geometry is a straight polyline (single-part, single-segment).
     * This is used to ensure only valid polylines can be moved.
     */
    private fun isStraightPolyline(geometry: Geometry?): Boolean {
        val polyline = geometry as? Polyline ?: return false
        // Only allow single-part, single-segment polylines
        return polyline.parts.size == 1 && polyline.parts.first().points.toList().size == 2
    }

    /**
     * Update the building number for address editing.
     */
    fun onBuildingNumberChange(newBuildingNumber: Int) {
        buildingNumber = newBuildingNumber
    }

    /**
     * Update the street name for address editing.
     */
    fun onStreetNameChange(newStreetName: String) {
        streetName = newStreetName
    }

    /**
     * Update the visibility of the edit address dialog.
     */
    fun onShowEditAddressDialogChange(show: Boolean) {
        showEditAddressDialog = show
    }
}

/**
 * Instructions for the user based on the current state of the map interaction.
 * These are used to guide the user on what actions to take next.
 */
sealed class Instruction(val message: String) {
    data object SelectFeature : Instruction("Select a point or straight polyline to edit.")
    data object SelectStraightPolyline : Instruction("Select straight (single segment) polylines only.")
    data object MoveFeature : Instruction("Tap on the map to move the feature.")
}
