/* Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.displaydimensions

import android.os.Bundle
import android.util.Log
import com.esri.arcgismaps.sample.sampleslib.BaseEdgeToEdgeActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.layers.DimensionLayer
import com.esri.arcgismaps.sample.displaydimensions.databinding.DimensionsDialogLayoutBinding
import com.esri.arcgismaps.sample.displaydimensions.databinding.DisplayDimensionsActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : BaseEdgeToEdgeActivity() {

    private val provisionPath: String by lazy {
        getExternalFilesDir(null)?.path.toString() + File.separator + getString(R.string.display_dimensions_app_name)
    }

    private val activityMainBinding: DisplayDimensionsActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.display_dimensions_activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val optionsButton by lazy {
        activityMainBinding.optionsButton
    }

    // keep an instance of the MapView's dimension layer
    private var dimensionLayer: DimensionLayer? = null

    // track if the layer is enabled
    private var isDimensionLayerEnabled: Boolean = true

    // track if the custom definition is enabled
    private var isDefinitionEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)
        lifecycle.addObserver(mapView)

        // check if the .mmpk file exits
        val mmpkFile = File(provisionPath, getString(R.string.file_name))
        if (!mmpkFile.exists()) return showError("Mobile map package file does not exist.")

        // create and load a mobile map package
        val mobileMapPackage = MobileMapPackage(mmpkFile.path)

        lifecycleScope.launch {
            // load the mobile map package
            mobileMapPackage.load().getOrElse {
                return@launch showError("Failed to load the mobile map package: ${it.message}")
            }
            // if the loaded mobile map package does not contain a map
            if (mobileMapPackage.maps.isEmpty()) {
                return@launch showError("Mobile map package does not contain a map")
            }

            // add the map from the mobile map package to the map view,
            // and set a min scale to maintain dimension readability
            mapView.map = mobileMapPackage.maps[0]
            mapView.map?.minScale = 35000.0

            // set the dimension layer within the map
            dimensionLayer = mapView.map?.operationalLayers?.firstOrNull { layer ->
                layer is DimensionLayer
            } as DimensionLayer

        }

        optionsButton.setOnClickListener {
            // inflate the dialog layout and get references to each of its components
            val dialogBinding = DimensionsDialogLayoutBinding.inflate(layoutInflater)
            dialogBinding.dimensionLayerSwitch.apply {
                isChecked = isDimensionLayerEnabled
                setOnCheckedChangeListener { _, isEnabled ->
                    // set the visibility of the dimension layer
                    dimensionLayer?.isVisible = isEnabled
                    isDimensionLayerEnabled = isEnabled
                }
            }
            dialogBinding.definitionSwitch.apply {
                isChecked = isDefinitionEnabled
                setOnCheckedChangeListener { _, isEnabled ->
                    // set a definition expression to show dimension lengths of
                    // greater than or equal to 450m when the checkbox is selected,
                    // or to reset the definition expression to show all
                    // dimension lengths when unselected
                    val defExpression = if (isEnabled) "DIMLENGTH >= 450" else ""
                    dimensionLayer?.definitionExpression = defExpression
                    isDefinitionEnabled = isEnabled
                }
            }

            // set up the dialog
            MaterialAlertDialogBuilder(this).apply {
                setView(dialogBinding.root)
                setTitle("${getString(R.string.settings)}: ${dimensionLayer?.name}")
            }.show()
        }
    }

    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
