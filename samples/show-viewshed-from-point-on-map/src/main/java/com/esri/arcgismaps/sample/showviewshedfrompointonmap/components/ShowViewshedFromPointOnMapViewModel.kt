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

package com.esri.arcgismaps.sample.showviewshedfrompointonmap.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.data.FeatureCollectionTable
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.tasks.geoprocessing.GeoprocessingExecutionType
import com.arcgismaps.tasks.geoprocessing.GeoprocessingJob
import com.arcgismaps.tasks.geoprocessing.GeoprocessingParameters
import com.arcgismaps.tasks.geoprocessing.GeoprocessingTask
import com.arcgismaps.tasks.geoprocessing.geoprocessingparameters.GeoprocessingFeatures
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShowViewshedFromPointOnMapViewModel(application: Application) :
    AndroidViewModel(application) {

    // ArcGISMap with a topographic basemap
    val arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = Viewpoint(
            latitude = 45.379,
            longitude = 6.849,
            scale = 144447.0
        )
    }

    // Used by the composable MapView for viewpoint changes
    val mapviewProxy = MapViewProxy()

    // Graphics overlay for the red marker at the tapped location
    val inputGraphicsOverlay = GraphicsOverlay().apply {
        renderer = SimpleRenderer(
            symbol = SimpleMarkerSymbol(
                style = SimpleMarkerSymbolStyle.Circle,
                color = Color.red,
                size = 10f
            )
        )
    }

    // Graphics overlay for displaying the resulting viewshed polygons
    val resultGraphicsOverlay = GraphicsOverlay().apply {
        renderer = SimpleRenderer(
            symbol = SimpleFillSymbol(
                style = SimpleFillSymbolStyle.Solid,
                color = Color.fromRgba(r = 255, g = 165, b = 0, a = 100)
            )
        )
    }

    // GeoprocessingTask pointing to the Viewshed service URL
    private val geoprocessingTask = GeoprocessingTask(
        url = "https://sampleserver6.arcgisonline.com/arcgis/rest/services/Elevation/ESRI_Elevation_World/GPServer/Viewshed"
    )

    // Running GeoprocessingJob for cancellation/cleanup
    private var geoprocessingJob: GeoprocessingJob? = null

    // State flows for controlling UI
    private val _isGeoprocessingInProgress = MutableStateFlow(false)
    val isGeoprocessingInProgress = _isGeoprocessingInProgress.asStateFlow()

    // Message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Handles the [singleTapConfirmedEvent] by retrieving the tapped [Point] to
     * cancel and run a new viewshed geoprocessing job.
     */
    fun onMapTapped(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        val tapPoint = singleTapConfirmedEvent.mapPoint
            ?: return messageDialogVM.showMessageDialog("Unable to retrieve tapped point")
        viewModelScope.launch {
            // Clear existing overlays and cancel any running job
            clearOverlays()
            geoprocessingJob?.cancel()
            // Add a new red marker to the map at the tapped point
            addTapMarker(tapPoint)
            // Start the geoprocessing job to obtain the viewshed polygons
            _isGeoprocessingInProgress.value = true
            calculateViewshed(tapPoint)
            _isGeoprocessingInProgress.value = false
        }
    }

    /**
     * Perform the viewshed calculation on the geoprocessing service
     * for the given [tapPoint].
     */
    private suspend fun calculateViewshed(tapPoint: Point) {
        // Create an empty FeatureCollectionTable for the tapped location
        val table = FeatureCollectionTable(
            fields = emptyList(),
            geometryType = GeometryType.Point,
            spatialReference = tapPoint.spatialReference
        )

        // Create a new feature with the tapped geometry and add to the table
        val newFeature = table.createFeature().also { it.geometry = tapPoint }
        table.addFeature(newFeature)

        // Create geoprocessing parameters for a synchronous execution
        val geoprocessingParameters = GeoprocessingParameters(
            geoprocessingExecutionType = GeoprocessingExecutionType.SynchronousExecute
        ).apply {
            processSpatialReference = tapPoint.spatialReference
            outputSpatialReference = tapPoint.spatialReference
            // Provide the tapped point as "Input_Observation_Point"
            inputs["Input_Observation_Point"] = GeoprocessingFeatures(table)
        }

        // Create a new job
        geoprocessingJob = geoprocessingTask.createJob(geoprocessingParameters)

        // Start and await the result
        geoprocessingJob?.start()

        val gpResult = geoprocessingJob?.result()?.getOrElse {
            return messageDialogVM.showMessageDialog(it)
        }

        // Get the output features for the viewshed polygon
        val viewshedFeatureSet = gpResult?.outputs?.get("Viewshed_Result") as? GeoprocessingFeatures
            ?: return messageDialogVM.showMessageDialog("No viewshed result found in the geoprocessing job.")
        val featureSet = viewshedFeatureSet.features
            ?: return messageDialogVM.showMessageDialog("Geoprocessing feature set is null.")

        // Add each resulting feature geometry as a graphic to resultGraphicsOverlay
        val resultGraphics = featureSet.mapNotNull { feature ->
            feature.geometry?.let { Graphic(it) }
        }

        // Add the graphics to the overlay and set the map's viewpoint to its extent
        resultGraphicsOverlay.graphics.addAll(resultGraphics)
        resultGraphicsOverlay.extent?.let { resultExtent ->
            mapviewProxy.setViewpointGeometry(
                boundingGeometry = resultExtent,
                paddingInDips = 20.0
            )
        }
    }

    /**
     * Place a simple red marker graphic at the tapped location.
     */
    private fun addTapMarker(tapPoint: Point) {
        val graphic = Graphic(tapPoint)
        inputGraphicsOverlay.graphics.add(graphic)
    }

    /**
     * Clear any previous marker or result polygons from the map.
     */
    private fun clearOverlays() {
        inputGraphicsOverlay.graphics.clear()
        resultGraphicsOverlay.graphics.clear()
    }
}
