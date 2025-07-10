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

package com.esri.arcgismaps.sample.applyfunctiontorasterfromservice

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.raster.ImageServiceRaster
import com.arcgismaps.raster.Raster
import com.arcgismaps.raster.RasterFunction
import com.esri.arcgismaps.sample.applyfunctiontorasterfromservice.databinding.ApplyFunctionToRasterFromServiceActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val activityMainBinding: ApplyFunctionToRasterFromServiceActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.apply_function_to_raster_from_service_activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val imageServiceRaster: ImageServiceRaster by lazy {
        ImageServiceRaster(getString(R.string.image_service_raster_url))
    }

    private val imageRasterLayer: RasterLayer by lazy {
        RasterLayer(imageServiceRaster)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)
        lifecycle.addObserver(mapView)

        // create and add a map with a dark gray basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISDarkGray)
        mapView.map = map

        // add the imageRasterLayer to the map
        addImageRasterLayer()

        activityMainBinding.apply {
            rasterButton.setOnClickListener {
                // update the raster with simplified hillshade
                applyRasterFunction()
                resetButton.isEnabled = true
                rasterButton.isEnabled = false
            }
            resetButton.setOnClickListener {
                // reset map to back to the RasterLayer
                addImageRasterLayer()
                resetButton.isEnabled = false
                rasterButton.isEnabled = true
            }
        }
    }

    /**
     * Adds the image raster layer to the map and set's the viewpoint
     * to the image server raster's bounding geometry
     */
    private fun addImageRasterLayer() {
        // clear and add the imageRasterLayer to the map
        mapView.map?.operationalLayers?.apply {
            clear()
            add(imageRasterLayer)
        }

        // collect the load status of the RasterLayer
        lifecycleScope.launch {
            imageRasterLayer.loadStatus.collect { loadStatus ->
                if (loadStatus == LoadStatus.Loaded) {
                    // get the center point of the image service raster
                    val extentEnvelope = imageServiceRaster.serviceInfo?.fullExtent
                        ?: return@collect showError("Error retrieving the ArcGISImageServiceInfo")
                    // set the viewpoint of the map to the envelope
                    mapView.setViewpointGeometry(extentEnvelope)
                } else if (loadStatus is LoadStatus.FailedToLoad) {
                    showError("Error loading image raster layer: ${loadStatus.error.message}")
                }
            }
        }
    }

    /**
     * Create a hillshade layer using a custom JSON raster function.
     */
    private fun applyRasterFunction() {
        // create raster function from json string
        val rasterFunction = RasterFunction.fromJsonOrNull(getString(R.string.hillshade_simplified))
            ?: return showError("Error creating a raster function object from JSON")

        // get parameter name value pairs used by hillside
        val rasterFunctionArguments = rasterFunction.arguments
            ?: return showError("Raster function arguments is null")

        // get a list of raster names associated with the raster function
        val rasterNames = rasterFunctionArguments.rasterNames
        // check if raster function arguments contains raster variable names
        if(rasterNames.isNotEmpty()){
            // using the first raster variable name
            rasterFunctionArguments.setRaster(rasterNames[0], imageServiceRaster)
            // create raster as raster layer
            val hillshadeRaster = Raster.createWithRasterFunction(rasterFunction)
            val hillshadeLayer = RasterLayer(hillshadeRaster)
            // clear and add the layer to the map
            mapView.map?.operationalLayers?.add(hillshadeLayer)
        } else{
            showError("Raster function arguments does not contain raster variable names")
        }

    }

    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
