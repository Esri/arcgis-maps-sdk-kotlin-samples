/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.addfeatureswithcontingentvalues.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.data.CodedValue
import com.arcgismaps.data.CodedValueDomain
import com.arcgismaps.data.ContingentCodedValue
import com.arcgismaps.data.ContingentRangeValue
import com.arcgismaps.data.Feature
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISVectorTiledLayer
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.esri.arcgismaps.sample.addfeatureswithcontingentvalues.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapViewModel(application: Application) : AndroidViewModel(application) {
    // Create an empty map, to be updated once data is loaded from the feature table
    var arcGISMap by mutableStateOf(ArcGISMap())

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // graphics overlay used to add feature graphics to the map
    val graphicsOverlay = GraphicsOverlay()

    // state flow of UI state
    private val _featureEditState = MutableStateFlow(FeatureEditState())
    val featureEditState = _featureEditState.asStateFlow()

    private val cacheDir: File = application.cacheDir

    private val provisionPath: String = application.getExternalFilesDir(null)?.path.toString() +
            File.separator + application.getString(R.string.add_features_with_contingent_values_app_name)

    // offline vector tiled layer to be used as a basemap
    private val fillmoreVectorTileLayer = ArcGISVectorTiledLayer("$provisionPath/FillmoreTopographicMap.vtpk")

    // mobile database containing offline feature data
    private val geodatabase = Geodatabase("${cacheDir.path}/ContingentValuesBirdNests.geodatabase")

    // instance of the contingent feature to be added to the map
    private var feature: ArcGISFeature? = null

    // instance of the feature table retrieved from the geodatabase, updates when new feature is added
    private var featureTable: ArcGISFeatureTable? = null

    init {
        // create a temporary directory for use with the geodatabase file
        createGeodatabaseCacheDir()

        viewModelScope.launch {
            // retrieve and load the offline mobile geodatabase file from the cache directory
            geodatabase.load().getOrElse {
                messageDialogVM.showMessageDialog(
                    "Error loading GeoDatabase",
                    it.message.toString()
                )
            }

            // get the first geodatabase feature table
            val featureTable = geodatabase.featureTables.firstOrNull()
                ?: return@launch messageDialogVM.showMessageDialog(
                    "Error",
                    "No feature table found in geodatabase"
                )

            // load the geodatabase feature table
            featureTable.load().getOrElse {
                return@launch messageDialogVM.showMessageDialog(
                    "Error loading feature table",
                    it.message.toString()
                )
            }

            // get the contingent values definition from the feature table and load it
            featureTable.contingentValuesDefinition.load().getOrElse {
                messageDialogVM.showMessageDialog(
                    "Error",
                    "Error loading the contingent values definition"
                )
            }

            // create and load the feature layer from the feature table
            val featureLayer = FeatureLayer.createWithFeatureTable(featureTable)

            // get the full extent of the feature layer
            val extent = featureLayer.fullExtent
                ?: return@launch messageDialogVM.showMessageDialog(
                    "Error",
                    "Error retrieving extent of the feature layer"
                )

            // set the basemap to the offline vector tiled layer, and viewpoint to the feature layer extent
            arcGISMap = ArcGISMap(Basemap(fillmoreVectorTileLayer)).apply {
                initialViewpoint = Viewpoint(boundingGeometry = extent as Geometry)
            }
            arcGISMap.operationalLayers.add(featureLayer)

            // keep the instance of the feature table
            this@MapViewModel.featureTable = featureTable

            // add buffer graphics for the feature layer
            showBufferGraphics()

            // get status attributes for new features
            _featureEditState.value = _featureEditState.value.copy(statusAttributes = featureTable.statusFieldCodedValues())
        }

    }

    /**
     * Geodatabase creates and uses various temporary files while processing a database,
     * which will need to be cleared before looking up the [geodatabase] again.
     * A copy of the original geodatabase file is created in the cache folder.
     */
    private fun createGeodatabaseCacheDir() {
        // clear cache directory
        File(cacheDir.path).deleteRecursively()
        // copy over the original Geodatabase file to be used in the temp cache directory
        File("$provisionPath/ContingentValuesBirdNests.geodatabase").copyTo(
            File("${cacheDir.path}/ContingentValuesBirdNests.geodatabase")
        )
    }

    /**
     * Creates buffer graphics for features in the [featureTable], and adds the graphics to
     * the [graphicsOverlay].
     */
    private suspend fun showBufferGraphics() {
        // clear the existing graphics
        graphicsOverlay.graphics.clear()

        // create buffer graphics for features which need them
        val queryParameters = QueryParameters().apply {
            whereClause = "BufferSize > 0"
        }

        featureTable?.let {
            // query the features using the queryParameters on the featureTable
            val featureQueryResult = it.queryFeatures(queryParameters).getOrNull()
            val featureResultList = featureQueryResult?.toList()

            if (!featureResultList.isNullOrEmpty()) {
                // create list of graphics for each query result
                val graphics = featureResultList.map { createBufferGraphic(it) }
                // add the graphics to the graphics overlay
                graphicsOverlay.graphics.addAll(graphics)
            } else {
                messageDialogVM.showMessageDialog(
                    "Error",
                    "No features found with BufferSize > 0"
                )
            }
        }
    }

    /**
     * Creates and returns a graphic using the attributes of the given [feature].
     */
    private fun createBufferGraphic(feature: Feature): Graphic {
        // create the outline for the buffer symbol
        val lineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 2f)
        // create the buffer symbol
        val bufferSymbol =
            SimpleFillSymbol(SimpleFillSymbolStyle.ForwardDiagonal, Color.red, lineSymbol)
        // get the feature's buffer size
        val bufferSize = feature.attributes["BufferSize"] as Int
        // get a polygon using the feature's buffer size and geometry
        val polygon =
            feature.geometry?.let { GeometryEngine.bufferOrNull(it, bufferSize.toDouble()) }

        // create a graphic using the geometry and fill symbol
        return Graphic(polygon, bufferSymbol)
    }

    /**
     * Retrieve the status fields, add the fields to a ContingentValueDomain.
     * Used to display options in the UI.
     */
    private fun ArcGISFeatureTable.statusFieldCodedValues(): List<CodedValue> {
        val statusField = fields.find { field -> field.name == "Status" }
        val codedValueDomain = statusField?.domain as CodedValueDomain
        return codedValueDomain.codedValues
    }

    fun onStatusAttributeSelected(codedValue: CodedValue) = featureTable?.let { featureTable ->
        viewModelScope.launch {

            feature = featureTable.createFeature() as ArcGISFeature
            feature?.attributes?.set("Status", codedValue.code)

            featureTable.let {
                _featureEditState.value = FeatureEditState(
                    status = codedValue,
                    statusAttributes = it.statusFieldCodedValues(),
                    protectionAttributes = it.protectionFieldCodedValues()
                )
            }
        }
    }

    private fun ArcGISFeatureTable.protectionFieldCodedValues(): List<CodedValue> {
        // get the contingent value results with the feature for the protection field
        val contingentValuesResult = feature?.let {
            getContingentValuesOrNull(it, "Protection")
        }

        // get the list of contingent values by field group
        val contingentValues = contingentValuesResult?.byFieldGroup?.get("ProtectionFieldGroup")

        // convert the list of ContingentValues to a list of CodedValue
        val protectionCodedValues: List<CodedValue> =
            contingentValues?.map { (it as ContingentCodedValue).codedValue }
                ?: listOf<CodedValue>().also {
                    messageDialogVM.showMessageDialog(
                        "Error",
                        "Error getting coded values by field group"
                    )
                }

        return protectionCodedValues
    }

    fun onProtectionAttributeSelected(codedValue: CodedValue) {
        feature?.attributes?.set("Protection", codedValue.code)
        _featureEditState.value = _featureEditState.value.copy(
            protection = codedValue,
            bufferRange = featureTable?.bufferRange()
        )
    }

    private fun ArcGISFeatureTable.bufferRange(): ContingentRangeValue? {
        val contingentValueResult = feature?.let {
            getContingentValuesOrNull(it, "BufferSize")
        }

        return contingentValueResult?.byFieldGroup?.get("BufferSizeFieldGroup")?.get(0) as? ContingentRangeValue
    }

    fun onBufferSizeSelected(bufferSize: Int) {
        feature?.attributes?.set("BufferSize", bufferSize)
        _featureEditState.value = _featureEditState.value.copy(buffer = bufferSize)
    }

    /**
     * Ensure that the selected values are a valid combination.
     * If contingencies are valid, then display [feature] on the [mapPoint]
     */
    fun validateContingency(mapPoint: Point) {
        val resources = getApplication<Application>().resources
        // check if all the features have been set
        if (featureTable == null) {
            messageDialogVM.showMessageDialog(resources.getString(R.string.input_all_values))
            return
        }

        // validate the feature's contingencies
        val contingencyViolations = feature?.let {
            featureTable?.validateContingencyConstraints(it)
        } ?: return messageDialogVM.showMessageDialog(resources.getString(R.string.no_feature_created))

        // if there are no contingency violations the feature is valid and ready to add to the feature table
        if (contingencyViolations.isEmpty()) {

            // set the geometry of the feature to the map point
            feature?.geometry = mapPoint

            // create the buffer graphic for the feature
            val bufferGraphic = feature?.let { createBufferGraphic(it) }

            // add the graphic to the graphics overlay
            bufferGraphic?.let { graphicsOverlay.graphics.add(it) }

            // add the feature to the feature table
            viewModelScope.launch {
                feature?.let { featureTable?.addFeature(it) }
            }
        } else {
            val violations = contingencyViolations.joinToString(separator = "\n") {
                violation -> violation.fieldGroup.name
            }
            messageDialogVM.showMessageDialog(
                "Invalid contingent values",
                "${contingencyViolations.size} violations found:\n" + violations
            )
        }

    }

    /**
     * Clears feature and attributes held in the view model to avoid inconsistent state
     * after feature is created, fails to create, etc.
     */
    fun clearFeature() {
        feature = null
        _featureEditState.value = featureTable?.let {FeatureEditState(statusAttributes = it.statusFieldCodedValues())}
            ?: return messageDialogVM.showMessageDialog("Feature Table not loaded")
    }

}

/**
 * Currently selected status, protection, and buffer attributes for the feature under construction,
 * used to update the UI.
 */
data class FeatureEditState(
    val status: CodedValue? = null,
    val protection: CodedValue? = null,
    val buffer: Int = 0,
    val statusAttributes: List<CodedValue> = listOf(),
    val protectionAttributes: List<CodedValue> = listOf(),
    val bufferRange: ContingentRangeValue? = null
)
