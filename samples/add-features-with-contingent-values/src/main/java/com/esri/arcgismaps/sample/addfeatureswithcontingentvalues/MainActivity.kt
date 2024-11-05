/* Copyright 2022 Esri
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

package com.esri.arcgismaps.sample.addfeatureswithcontingentvalues

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
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
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.esri.arcgismaps.sample.addfeatureswithcontingentvalues.databinding.AddFeaturesWithContingentValuesActivityMainBinding
import com.esri.arcgismaps.sample.addfeatureswithcontingentvalues.databinding.AddFeatureLayoutBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: AddFeaturesWithContingentValuesActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.add_features_with_contingent_values_activity_main)
    }

    private val bottomSheetBinding by lazy {
        AddFeatureLayoutBinding.inflate(layoutInflater)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val provisionPath: String by lazy {
        getExternalFilesDir(null)?.path.toString() + File.separator + getString(R.string.add_features_with_contingent_values_app_name)
    }

    // mobile database containing offline feature data. geodatabase is closed on app exit
    private val geodatabase: Geodatabase by lazy {
        Geodatabase("${cacheDir.path}/ContingentValuesBirdNests.geodatabase")
    }

    // graphic overlay instance to add the feature graphic to the map
    private val graphicsOverlay = GraphicsOverlay()

    // instance of the contingent feature to be added to the map
    private var feature: ArcGISFeature? = null

    // instance of the feature table retrieved from the geodatabase, updates when new feature is added
    private var featureTable: ArcGISFeatureTable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)
        lifecycle.addObserver(mapView)

        // use the offline vector tiled layer as a basemap
        val fillmoreVectorTiledLayer = ArcGISVectorTiledLayer(
            "$provisionPath/FillmoreTopographicMap.vtpk"
        )

        mapView.apply {
            // set the basemap layer and the graphic overlay to the MapView
            map = ArcGISMap(Basemap(fillmoreVectorTiledLayer))
            graphicsOverlays.add(graphicsOverlay)
        }

        // add a listener to the MapView to detect when
        // a user has performed a single tap to add a new feature
        lifecycleScope.launch {
            mapView.onSingleTapConfirmed.collect {
                // open a bottom sheet view to add the feature
                it.mapPoint?.let { mapPoint -> createBottomSheet(mapPoint) }
            }
        }

        // create a temporary directory to use the geodatabase file
        createGeodatabaseCacheDirectory()

        lifecycleScope.launch {
            // retrieve and load the offline mobile geodatabase file from the cache directory
            geodatabase.load().getOrElse {
                showError("Error loading GeoDatabase: ${it.message}")
            }

            // get the first geodatabase feature table
            val featureTable = geodatabase.featureTables.firstOrNull()
                ?: return@launch showError("No feature table found in geodatabase")
            // load the geodatabase feature table
            featureTable.load().getOrElse {
                return@launch showError(it.message.toString())
            }

            // create and load the feature layer from the feature table
            val featureLayer = FeatureLayer.createWithFeatureTable(featureTable)
            // add the feature layer to the map
            mapView.map?.operationalLayers?.add(featureLayer)

            // set the map's viewpoint to the feature layer's full extent
            val extent = featureLayer.fullExtent
                ?: return@launch showError("Error retrieving extent of the feature layer")
            mapView.setViewpoint(Viewpoint(extent))

            // keep the instance of the featureTable
            this@MainActivity.featureTable = featureTable

            // add buffer graphics for the feature layer
            queryFeatures()
        }
    }

    /**
     * Geodatabase creates and uses various temporary files while processing a database,
     * which will need to be cleared before looking up the [geodatabase] again.
     * A copy of the original geodatabase file is created in the cache folder.
     */
    private fun createGeodatabaseCacheDirectory() {
        // clear cache directory
        File(cacheDir.path).deleteRecursively()
        // copy over the original Geodatabase file to be used in the temp cache directory
        File("$provisionPath/ContingentValuesBirdNests.geodatabase").copyTo(
            File("${cacheDir.path}/ContingentValuesBirdNests.geodatabase")
        )
    }

    /**
     * Create buffer graphics for the features and adds the graphics to
     * the [graphicsOverlay]
     */
    private suspend fun queryFeatures() {
        // clear the existing graphics
        graphicsOverlay.graphics.clear()

        // create buffer graphics for the features
        val queryParameters = QueryParameters().apply {
            // set the where clause to filter for buffer sizes greater than 0
            whereClause = "BufferSize > 0"
        }

        // query the features using the queryParameters on the featureTable
        val featureQueryResult = featureTable?.queryFeatures(queryParameters)?.getOrThrow()
        // call get on the future to get the result
        val featureResultList = featureQueryResult?.toList()

        if (!featureResultList.isNullOrEmpty()) {
            // create list of graphics for each query result
            val graphics = featureResultList.map { createGraphic(it) }
            // add the graphics to the graphics overlay
            graphicsOverlay.graphics.addAll(graphics)
        } else {
            showError("No features found with BufferSize > 0")
        }
    }

    /**
     * Create a graphic for the given [feature] and returns a Graphic with the features attributes
     */
    private fun createGraphic(feature: Feature): Graphic {
        // get the feature's buffer size
        val bufferSize = feature.attributes["BufferSize"] as Int
        // get a polygon using the feature's buffer size and geometry
        val polygon = feature.geometry?.let { GeometryEngine.bufferOrNull(it, bufferSize.toDouble()) }
        // create the outline for the buffers
        val lineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 2f)
        // create the buffer symbol
        val bufferSymbol = SimpleFillSymbol(
            SimpleFillSymbolStyle.ForwardDiagonal, Color.red, lineSymbol
        )
        // create an graphic using the geometry and fill symbol
        return Graphic(polygon, bufferSymbol)
    }

    /**
     * Creates a BottomSheetDialog view to handle contingent value interaction.
     * Once the contingent values have been set and the apply button is clicked,
     * the function will call validateContingency() to add the feature at the [mapPoint].
     */
    private fun createBottomSheet(mapPoint: Point) {
        // creates a new BottomSheetDialog
        val bottomSheet = BottomSheetDialog(this).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        // set up the first content value attribute
        setUpStatusAttributes()

        // clear and set bottom sheet content view to layout,
        // to be able to set the content view on each bottom sheet draw
        if (bottomSheetBinding.root.parent != null) {
            (bottomSheetBinding.root.parent as ViewGroup).removeAllViews()
        }

        // reset feature to null since this is a new feature
        feature = null

        bottomSheetBinding.apply {

            // reset bottom sheet values, this is needed to showcase contingent values behavior
            statusInputLayout.editText?.setText("")
            protectionInputLayout.editText?.setText("")
            selectedBuffer.text = ""
            protectionInputLayout.isEnabled = false
            bufferSeekBar.isEnabled = false
            bufferSeekBar.value = bufferSeekBar.valueFrom

            // set apply button to validate and apply contingency feature on map
            applyTv.setOnClickListener {
                // check if the contingent features set is valid and set it to the map if valid
                validateContingency(mapPoint)
                bottomSheet.dismiss()
            }

            // dismiss on cancel clicked
            cancelTv.setOnClickListener { bottomSheet.dismiss() }
        }

        // set the content view to the root of the binding layout
        bottomSheet.setContentView(bottomSheetBinding.root)
        // display the bottom sheet view
        bottomSheet.show()
    }

    /**
     *  Retrieve the status fields, add the fields to a ContingentValueDomain, and set the values to the spinner
     *  When status attribute selected, createFeature() is called.
     */
    private fun setUpStatusAttributes() {
        // get the first field by name
        val statusField = featureTable?.fields?.find { field -> field.name == "Status" }
        // get the field's domains as coded value domain
        val codedValueDomain = statusField?.domain as CodedValueDomain
        // get the coded value domain's coded values
        val statusCodedValues = codedValueDomain.codedValues
        // get the selected index if applicable
        val statusNames = statusCodedValues.map { it.name }
        // get the items to be added to the spinner adapter
        val adapter = ArrayAdapter(bottomSheetBinding.root.context, R.layout.list_item, statusNames)
        (bottomSheetBinding.statusInputLayout.editText as AutoCompleteTextView).apply {
            setAdapter(adapter)
            setOnItemClickListener { _, _, position, _ ->
                // get the CodedValue of the item selected, and create a feature needed for feature attributes
                createFeature(statusCodedValues[position])
            }
        }
    }

    /**
     * Set up the [feature] using the status attribute's coded value
     * by loading the [featureTable]'s Contingent Value Definition.
     * This function calls setUpProtectionAttributes() once the [feature] has been set
     */
    private fun createFeature(codedValue: CodedValue) {
        // get the contingent values definition from the feature table
        val contingentValueDefinition = featureTable?.contingentValuesDefinition
        if (contingentValueDefinition != null) {
            lifecycleScope.launch {
                // load the contingent values definition
                contingentValueDefinition.load().getOrElse {
                    showError("Error loading the ContingentValuesDefinition")
                }
                // create a feature from the feature table and set the initial attribute
                feature = featureTable?.createFeature() as ArcGISFeature
                feature?.attributes?.set("Status", codedValue.code)
                setUpProtectionAttributes()
            }
        } else {
            showError("Error retrieving ContingentValuesDefinition from the FeatureTable")
        }
    }

    /**
     *  Retrieve the protection attribute fields, add the fields to a ContingentCodedValue, and set the values to the spinner
     *  When status attribute selected, showBufferSeekbar() is called.
     */
    private fun setUpProtectionAttributes() {
        // set the bottom sheet view to enable the Protection attribute, and disable input elsewhere
        bottomSheetBinding.apply {
            protectionInputLayout.isEnabled = true
            bufferSeekBar.isEnabled = false
            bufferSeekBar.value = bufferSeekBar.valueFrom
            protectionInputLayout.editText?.setText("")
            selectedBuffer.text = ""
        }

        // get the contingent value results with the feature for the protection field
        val contingentValuesResult = feature?.let {
            featureTable?.getContingentValuesOrNull(it, "Protection")
        }

        // get the list of contingent values by field group
        val contingentValues = contingentValuesResult?.byFieldGroup?.get("ProtectionFieldGroup")

        // convert the list of ContingentValues to a list of CodedValue
        val protectionCodedValues =
            contingentValues?.map { (it as ContingentCodedValue).codedValue }
                ?: return showError("Error getting coded values by field group")

        // get the attribute names for each coded value
        val protectionNames = protectionCodedValues.map { it.name }

        // set the items to be added to the spinner adapter
        val adapter = ArrayAdapter(
            bottomSheetBinding.root.context, R.layout.list_item, protectionNames
        )

        // set the choices of protection attribute values
        (bottomSheetBinding.protectionInputLayout.editText as AutoCompleteTextView).apply {
            setAdapter(adapter)
            setOnItemClickListener { _, _, position, _ ->
                // set the protection CodedValue of the item selected
                feature?.attributes?.set("Protection", protectionCodedValues[position].code)
                // enable buffer seekbar
                showBufferSeekbar()
            }
        }
    }

    /**
     *  Retrieve the buffer attribute fields, add the fields
     *  to a ContingentRangeValue, and set the values to a SeekBar
     */
    private fun showBufferSeekbar() {
        // set the bottom sheet view to enable the buffer attribute
        bottomSheetBinding.apply {
            bufferSeekBar.isEnabled = true
            selectedBuffer.text = ""
        }

        // get the contingent value results using the feature and field
        val contingentValueResult = feature?.let {
            featureTable?.getContingentValuesOrNull(it, "BufferSize")
        }

        // get the contingent rang value of the buffer size field group
        val bufferSizeRangeValue = contingentValueResult?.byFieldGroup?.get("BufferSizeFieldGroup")
            ?.get(0) as ContingentRangeValue

        // set the minimum and maximum possible buffer sizes
        val minValue = bufferSizeRangeValue.minValue as Int
        val maxValue = bufferSizeRangeValue.maxValue as Int

        // check if there can be a max value, if not disable SeekBar
        // & set value to attribute size to 0
        if (maxValue > 0) {
            // get SeekBar instance from the binding layout
            bottomSheetBinding.bufferSeekBar.apply {
                // set the min, max and current value of the SeekBar
                valueFrom = minValue.toFloat()
                valueTo = maxValue.toFloat()
                value = valueFrom
                // set the initial attribute and the text to the min of the ContingentRangeValue
                feature?.attributes?.set("BufferSize", value.toInt())
                bottomSheetBinding.selectedBuffer.text = value.toInt().toString()
                // set the change listener to update the attribute value and the displayed value to the SeekBar position
                addOnChangeListener { _, value, _ ->
                    feature?.attributes?.set("BufferSize", value.toInt())
                    bottomSheetBinding.selectedBuffer.text = value.toInt().toString()
                }
            }
        } else {
            // max value is 0, so disable seekbar and update the attribute value accordingly
            bottomSheetBinding.apply {
                bufferSeekBar.isEnabled = false
                selectedBuffer.text = "0"
            }
            feature?.attributes?.set("BufferSize", 0)
        }
    }

    /**
     * Ensure that the selected values are a valid combination.
     * If contingencies are valid, then display [feature] on the [mapPoint]
     */
    private fun validateContingency(mapPoint: Point) {
        // check if all the features have been set
        if (featureTable == null) {
            showError("Input all values to add a feature to the map")
            return
        }

        // validate the feature's contingencies
        val contingencyViolations = feature?.let {
            featureTable?.validateContingencyConstraints(it)
        } ?: return showError("No feature attribute was selected")

        // if there are no contingency violations
        if (contingencyViolations.isEmpty()) {
            // the feature is valid and ready to add to the feature table
            // create a symbol to represent a bird's nest
            val symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.black, 11F)
            // add the graphic to the graphics overlay
            graphicsOverlay.graphics.add(Graphic(mapPoint, symbol))

            // set the geometry of the feature to the map point
            feature?.geometry = mapPoint

            // create the graphic of the feature
            val graphic = feature?.let { createGraphic(it) }
            // add the graphic to the graphics overlay
            graphic?.let { graphicsOverlay.graphics.add(it) }

            // add the feature to the feature table
            lifecycleScope.launch {
                feature?.let { featureTable?.addFeature(it) }
                feature?.load()?.getOrElse {
                    return@launch showError(it.message.toString())
                }
            }
        } else {
            showError("Invalid contingent values: " + (contingencyViolations.size) + " violations found.")
        }

    }

    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
