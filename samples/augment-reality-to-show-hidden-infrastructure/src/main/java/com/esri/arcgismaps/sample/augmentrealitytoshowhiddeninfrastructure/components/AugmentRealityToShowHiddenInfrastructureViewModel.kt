package com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.mapping.view.geometryeditor.FreehandTool
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AugmentRealityToShowHiddenInfrastructureViewModel(app: Application) : AndroidViewModel(app) {
    // Map and overlays
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISImagery).apply {
        initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
    }
    val pipesGraphicsOverlay = GraphicsOverlay().apply {
        renderer = SimpleLineSymbol(
            style = SimpleLineSymbolStyle.Solid,
            color = Color.red,
            width = 2f
        ).let { renderer ->
            com.arcgismaps.mapping.symbology.SimpleRenderer(renderer)
        }
    }
    val mapViewProxy = MapViewProxy()

    // Location display
    val locationDisplay = LocationDisplay().apply {
        setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
        initialZoomScale = 1000.0
    }

    // Geometry editor for drawing pipes
    val geometryEditor = GeometryEditor().apply {
        tool = FreehandTool()
    }

    val pipeSymbol = SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Dash,
        color = Color.red,
        width = 2f
    )

    // State for UI
    private val _statusMessage = MutableStateFlow("Tap the map to add pipe points.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _canDelete = MutableStateFlow(false)
    val canDelete: StateFlow<Boolean> = _canDelete.asStateFlow()

    private val _canApplyEdits = MutableStateFlow(false)
    val canApplyEdits: StateFlow<Boolean> = _canApplyEdits.asStateFlow()

    private val _geometryEditorCanUndo = MutableStateFlow(false)
    val geometryEditorCanUndo: StateFlow<Boolean> = _geometryEditorCanUndo.asStateFlow()

    // For elevation dialog
    var isElevationDialogVisible by mutableStateOf(false)
    var elevationInput by mutableStateOf(0.0)

    // Store elevation offset for each pipe
    private val pipeElevationOffsets = mutableListOf<Double>()

    // Error dialog
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
            // Start location display
            locationDisplay.dataSource.start().onFailure {
                messageDialogVM.showMessageDialog("Failed to start location display", it.message ?: "")
            }
        }
        viewModelScope.launch {
            // Listen for geometry editor geometry changes
            geometryEditor.geometry.collect {
                val polyline = it as? Polyline
                val canApply = polyline?.parts?.any { part -> part.points.toList().size >= 2 } == true
                _canApplyEdits.value = canApply
                if (canApply) {
                    _statusMessage.value = "Tap the check mark to add the pipe."
                }
                _geometryEditorCanUndo.value = geometryEditor.canUndo.value
            }
        }
    }

    fun startDrawingPipe() {
       // geometryEditor.start()
        _statusMessage.value = "Tap the map to add pipe points."
        _canApplyEdits.value = false
        _geometryEditorCanUndo.value = false
    }

    fun undoOrDelete() {
        if (geometryEditor.canUndo.value) {
            geometryEditor.undo()
        } else {
            removeAllGraphics()
        }
    }

    fun removeAllGraphics() {
        pipesGraphicsOverlay.graphics.clear()
        pipeElevationOffsets.clear()
        _canDelete.value = false
        _statusMessage.value = "Tap the map to add pipe points."
    }

    fun applyPipe() {
        isElevationDialogVisible = true
    }

    fun confirmElevationInput(elevation: Double) {
        val geometry = geometryEditor.stop() as? Polyline ?: return
        val pipeGraphic = Graphic(geometry)
        pipesGraphicsOverlay.graphics.add(pipeGraphic)
        pipeElevationOffsets.add(elevation)
        _canDelete.value = true
        if (elevation < 0) {
            _statusMessage.value = "Pipe added ${elevation} meter(s) below surface.\nTap the camera to view the pipe(s) in AR."
        } else if (elevation == 0.0) {
            _statusMessage.value = "Pipe added at ground level.\nTap the camera to view the pipe(s) in AR."
        } else {
            _statusMessage.value = "Pipe added ${elevation} meter(s) above surface.\nTap the camera to view the pipe(s) in AR."
        }
        startDrawingPipe()
        isElevationDialogVisible = false
    }

    fun cancelElevationInput() {
        //geometryEditor.start()
        isElevationDialogVisible = false
    }

    // For AR: Expose the list of pipe graphics and their elevation offsets
    fun getPipeGraphicsWithElevations(): List<Pair<Graphic, Double>> {
        return pipesGraphicsOverlay.graphics.zip(pipeElevationOffsets)
    }
}
