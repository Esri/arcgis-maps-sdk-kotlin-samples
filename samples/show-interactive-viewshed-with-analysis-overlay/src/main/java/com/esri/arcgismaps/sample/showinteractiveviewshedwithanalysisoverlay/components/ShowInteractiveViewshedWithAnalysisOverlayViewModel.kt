/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.analysis.ContinuousField
import com.arcgismaps.analysis.ContinuousFieldFunction
import com.arcgismaps.analysis.interactive.FieldAnalysis
import com.arcgismaps.analysis.visibility.ViewshedFunction
import com.arcgismaps.analysis.visibility.ViewshedParameters
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.raster.Colormap
import com.arcgismaps.mapping.symbology.raster.ColormapRenderer
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.LongPressEvent
import com.arcgismaps.mapping.view.PanChangeEvent
import com.arcgismaps.mapping.view.PanChangeEvent.PanStatus
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.R
import kotlinx.coroutines.launch
import java.io.File

class ShowInteractiveViewshedWithAnalysisOverlayViewModel(app: Application) : AndroidViewModel(app) {
    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISImagery).apply {
            initialViewpoint = Viewpoint(55.610000, -5.200346, 100000.0)
        }
    )
    var analysisOverlay by mutableStateOf(AnalysisOverlay())
    var graphicsOverlay by mutableStateOf(GraphicsOverlay())

    val viewshedParameters by mutableStateOf(ViewshedParameters())

    val observerSymbol = SimpleMarkerSymbol(
        SimpleMarkerSymbolStyle.Circle,
        Color.blue,
        10.0f
    )

    lateinit var observerGraphic: Graphic

    var isDragging = false

    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() + File.separator + app.getString(
            R.string.show_interactive_viewshed_with_analysis_overlay_app_name
        ) + File.separator
    }

    private val filePath = provisionPath + app.getString(R.string.elevation_data_filename)

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        val initObserverPosition =
            Point(-579246.504, 7479619.947, 20.0, SpatialReference.webMercator())
        viewshedParameters.observerPosition = initObserverPosition
        viewshedParameters.targetHeight = 20.0
        viewshedParameters.maxRadius = 8000.0
        viewshedParameters.fieldOfView = 150.0
        viewshedParameters.heading = 10.0

        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }

            val filePaths = listOf(filePath)
            val continuousField = ContinuousField.createFromFiles(filePaths, 0).getOrThrow() //TODO: is getOrThrow appropriate?
            val continuousFieldFunction = ContinuousFieldFunction.create(continuousField)

            observerGraphic = Graphic(initObserverPosition, observerSymbol)
            graphicsOverlay.graphics.add(observerGraphic)

            val viewshedFunction = ViewshedFunction(continuousFieldFunction, viewshedParameters)
            val discreteViewshed = viewshedFunction.toDiscreteFieldFunction()

            // Prepare a colormap renderer to display the viewshed result
            val areaNotVisibleColor = Color.gray
            val areaVisibleColor = Color.fromRgba(136, 204, 132, 100) // translucent green
            val colors = listOf(areaNotVisibleColor, areaVisibleColor)
            val colormap = Colormap.create(colors)
            val colormapRenderer = ColormapRenderer(colormap)

            val analysis = FieldAnalysis(discreteViewshed, colormapRenderer)
            analysisOverlay.analyses.add(analysis)
        }
    }

    fun setObserverElevation(observerElevation: Float) {
        val oldPos = viewshedParameters.observerPosition
        val observerPosition = Point(oldPos!!.x, oldPos.y, observerElevation.toDouble())
        syncObserverPosition(observerPosition)
    }

    fun setTargetHeight(targetHeight: Float) {
        viewshedParameters.targetHeight = targetHeight.toDouble()
    }

    fun setMaxRadius(maxRadius: Float) {
        viewshedParameters.maxRadius = maxRadius.toDouble()
    }

    fun setFieldOfView(fieldOfView: Float) {
        viewshedParameters.fieldOfView = fieldOfView.toDouble()
    }

    fun setHeading(heading: Float) {
        viewshedParameters.heading = heading.toDouble()
    }

    fun setElevationSamplingInterval(elevationSamplingInterval: Double) {
        viewshedParameters.elevationSamplingInterval = when (elevationSamplingInterval) {
            0.0 -> null
            else -> elevationSamplingInterval
        }
    }

    fun onTap(mapPoint: Point?) {
        setNewObserverPosition(mapPoint)
    }

    fun onLongPress(event: LongPressEvent) {
        observerSymbol.color = Color.yellow
        isDragging = true
        setNewObserverPosition(event.mapPoint)
    }

    fun onPan(event: PanChangeEvent, mapViewProxy: MapViewProxy) {
        if (isDragging) {
            setNewObserverPosition(mapViewProxy.screenToLocationOrNull(event.screenCoordinate))
            if (event.status == PanStatus.End) {
                observerSymbol.color = Color.blue
                isDragging = false
            }
        }
    }

    private fun setNewObserverPosition(mapPoint: Point?) {
        if (mapPoint != null) {
            val observerPosition = when (viewshedParameters.observerPosition?.z) {
                null -> Point(mapPoint.x, mapPoint.y)
                else -> Point(mapPoint.x, mapPoint.y, viewshedParameters.observerPosition?.z)
            }
            syncObserverPosition(observerPosition)
        }
    }

    private fun syncObserverPosition(observerPosition: Point) {
        // Update the observer graphic geometry to the current observer position
        observerGraphic.geometry = observerPosition

        // Update the viewshed parameters to the current observer position, which triggers analysis
        viewshedParameters.observerPosition = observerPosition
    }
}
