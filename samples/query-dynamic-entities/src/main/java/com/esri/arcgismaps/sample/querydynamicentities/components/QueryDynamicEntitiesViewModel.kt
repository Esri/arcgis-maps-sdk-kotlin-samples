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

package com.esri.arcgismaps.sample.querydynamicentities.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.labeling.LabelDefinition
import com.arcgismaps.mapping.labeling.SimpleLabelExpression
import com.arcgismaps.mapping.layers.DynamicEntityLayer
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.realtime.CustomDynamicEntityDataSource
import com.arcgismaps.realtime.DynamicEntity
import com.arcgismaps.realtime.DynamicEntityDataSourceInfo
import com.arcgismaps.realtime.DynamicEntityQueryParameters
import com.arcgismaps.arcgisservices.LabelingPlacement
import com.arcgismaps.data.Field
import com.arcgismaps.data.FieldType
import com.arcgismaps.data.SpatialRelationship
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.sin

class QueryDynamicEntitiesViewModel(application: Application) : AndroidViewModel(application) {

    // Map centered on Phoenix Sky Harbor International Airport
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = Viewpoint(center = phoenixAirport, scale = 1_266_500.0)
    }

    // Graphics overlay to display the 15-mile airport buffer when querying by geometry
    val graphicsOverlay = GraphicsOverlay()
    private val bufferGraphic = Graphic()

    // Dynamic entity data source and layer
    private val planeFeedProvider = PlaneFeedProvider()
    private val dynamicEntityDataSource = CustomDynamicEntityDataSource(planeFeedProvider)
    private val dynamicEntityLayer = DynamicEntityLayer(dynamicEntityDataSource).apply {
        trackDisplayProperties.apply {
            showPreviousObservations = true
            showTrackLine = true
            maximumObservations = 20
        }

        // Label flight numbers above entities
        val labelDefinition = LabelDefinition(
            labelExpression = SimpleLabelExpression("[flight_number]"),
            textSymbol = TextSymbol().apply {
                color = Color.red
                size = 12f
            }
        ).apply { placement = LabelingPlacement.PointAboveCenter }
        labelDefinitions.add(labelDefinition)
        labelsEnabled = true
    }

    // Message dialog for error handling
    val messageDialogVM = MessageDialogViewModel()

    // Query UI state flows
    private val _isQueryRunning = MutableStateFlow(false)
    val isQueryRunning = _isQueryRunning.asStateFlow()

    private val _queryResultEntities = MutableStateFlow<List<DynamicEntity>>(emptyList())
    val queryResultEntities = _queryResultEntities.asStateFlow()

    private val _resultLabel = MutableStateFlow("")
    val resultLabel = _resultLabel.asStateFlow()

    init {
        // Add dynamic entity layer to the map
        arcGISMap.operationalLayers.add(dynamicEntityLayer)

        // Prepare the buffer graphic and add to overlay; hide by default
        val redFill = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = Color.fromRgba(255, 0, 0, 64), // translucent red
            outline = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 1f)
        )
        bufferGraphic.symbol = redFill
        graphicsOverlay.graphics.add(bufferGraphic)
        graphicsOverlay.isVisible = false

        // Connect the data source (starts streaming simulated plane updates)
        viewModelScope.launch {
            dynamicEntityDataSource.connect().onFailure {
                messageDialogVM.showMessageDialog(
                    title = "Failed to connect data source",
                    description = it.message.toString()
                )
            }
        }
    }

    // Perform query: Flights within 15 miles of PHX
    fun queryFlightsWithinPhoenixBuffer() {
        viewModelScope.launch {
            _isQueryRunning.value = true
            // Create and display the 15-mile geodetic buffer
            val buffer = createPhoenixAirportBuffer()
            bufferGraphic.geometry = buffer
            graphicsOverlay.isVisible = true

            val parameters = DynamicEntityQueryParameters().apply {
                geometry = buffer
                spatialRelationship = SpatialRelationship.Intersects
            }

            val queryResult = dynamicEntityDataSource.queryDynamicEntities(parameters)
            queryResult.onSuccess { result ->
                // Update selection and UI result list
                val entities = result.toList()
                dynamicEntityLayer.clearSelection()
                dynamicEntityLayer.selectDynamicEntities(entities)
                _queryResultEntities.value = entities
                _resultLabel.value = "Flights within 15 miles of PHX"
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Query failed",
                    description = error.message.toString()
                )
            }
            _isQueryRunning.value = false
        }
    }

    // Perform query: Flights arriving in PHX (attribute query)
    fun queryFlightsArrivingInPHX() {
        viewModelScope.launch {
            _isQueryRunning.value = true
            // Hide the geometry overlay for attribute-only query
            graphicsOverlay.isVisible = false
            val parameters = DynamicEntityQueryParameters().apply {
                whereClause = "status = 'In flight' AND arrival_airport = 'PHX'"
            }

            val queryResult = dynamicEntityDataSource.queryDynamicEntities(parameters)
            queryResult.onSuccess { result ->
                val entities = result.toList()
                dynamicEntityLayer.clearSelection()
                dynamicEntityLayer.selectDynamicEntities(entities)
                _queryResultEntities.value = entities
                _resultLabel.value = "Flights arriving in PHX"
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Query failed",
                    description = error.message.toString()
                )
            }
            _isQueryRunning.value = false
        }
    }

    // Perform query: Flights with a specific flight number (track ID)
    fun queryFlightsWithNumber(flightNumber: String) {
        viewModelScope.launch {
            _isQueryRunning.value = true
            // Hide the geometry overlay for track ID-only query
            graphicsOverlay.isVisible = false
            val parameters = DynamicEntityQueryParameters().apply {
                trackIds.add(flightNumber)
            }

            val queryResult = dynamicEntityDataSource.queryDynamicEntities(parameters)
            queryResult.onSuccess { result ->
                val entities = result.toList()
                dynamicEntityLayer.clearSelection()
                dynamicEntityLayer.selectDynamicEntities(entities)
                _queryResultEntities.value = entities
                _resultLabel.value = "Flights matching number: $flightNumber"
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Query failed",
                    description = error.message.toString()
                )
            }
            _isQueryRunning.value = false
        }
    }

    // Clear selection and hide the buffer overlay
    fun resetDisplay() {
        dynamicEntityLayer.clearSelection()
        _queryResultEntities.value = emptyList()
        graphicsOverlay.isVisible = false
        _resultLabel.value = ""
    }

    // Create a 15-mile geodetic buffer around PHX airport
    private fun createPhoenixAirportBuffer(): Geometry {
        return GeometryEngine.bufferGeodeticOrNull(
            geometry = phoenixAirport,
            distance = 15.0,
            distanceUnit = LinearUnit.miles,
            maxDeviation = Double.NaN,
            curveType = GeodeticCurveType.Geodesic
        ) ?: phoenixAirport
    }

    companion object {
        // Phoenix Sky Harbor Intl Airport (longitude, latitude) in WGS84
        private val phoenixAirport = Point(
            -112.0101,
            33.4352,
            SpatialReference.wgs84()
        )
    }
}

// A simple simulated plane feed provider that emits dynamic entity observations around Phoenix.
private class PlaneFeedProvider : CustomDynamicEntityDataSource.EntityFeedProvider {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _feed = MutableSharedFlow<CustomDynamicEntityDataSource.FeedEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val feed: SharedFlow<CustomDynamicEntityDataSource.FeedEvent> = _feed.asSharedFlow()

    private var feedJob: Job? = null

    // Schema: matches the attributes used by the Swift sample
    private val schema: List<Field> by lazy {
        listOf(
            Field(FieldType.Text, "flight_number", "", 128),
            Field(FieldType.Text, "aircraft", "", 128),
            Field(FieldType.Float64, "altitude_feet", "", 8),
            Field(FieldType.Text, "arrival_airport", "", 32),
            Field(FieldType.Float64, "heading", "", 8),
            Field(FieldType.Float64, "speed", "", 8),
            Field(FieldType.Text, "status", "", 64)
        )
    }

    override suspend fun onLoad(): DynamicEntityDataSourceInfo {
        return DynamicEntityDataSourceInfo(
            entityIdFieldName = "flight_number",
            fields = schema
        ).apply { spatialReference = SpatialReference.wgs84() }
    }

    override suspend fun onConnect() {
        startEmitting()
    }

    override suspend fun onDisconnect() {
        feedJob?.cancelAndJoin()
        feedJob = null
    }

    private fun startEmitting() {
        // Create a set of simulated flight tracks
        val flights = buildList {
            repeat(20) { index -> add("Flight_${300 + index}") }
        }
        val bearings = flights.associateWith { (0..359).random().toDouble() }.toMutableMap()
        val speedsKts = flights.associateWith { (300..520).random().toDouble() }.toMutableMap()

        val phoenix = Point(-112.0101, 33.4352, SpatialReference.wgs84())
        val radiusDegrees = 0.18 // ~ 12-13 miles; movement will keep within ~15 miles
        var tick = 0

        feedJob = scope.launch(Dispatchers.IO) {
            try {
                while (true) {
                    tick++
                    flights.forEachIndexed { idx, flightId ->
                        // Update bearing slightly to simulate turning
                        bearings[flightId] = ((bearings[flightId] ?: 0.0) + (idx % 5 - 2)) % 360.0
                        val angle = (tick * 6 + idx * 18) % 360
                        val angleRad = toRadians(angle.toDouble())
                        val lon = phoenix.x + radiusDegrees * cos(angleRad)
                        val lat = phoenix.y + radiusDegrees * sin(angleRad)
                        val point = Point(lon, lat, SpatialReference.wgs84())

                        val attributes = mutableMapOf<String, Any?>(
                            "flight_number" to flightId,
                            "aircraft" to listOf("A320", "B737", "B738", "E175", "A321")[idx % 5],
                            "altitude_feet" to (14_000..36_000).random().toDouble(),
                            "arrival_airport" to "PHX",
                            "heading" to (bearings[flightId] ?: 0.0),
                            "speed" to (speedsKts[flightId] ?: 420.0),
                            "status" to "In flight"
                        )

                        _feed.tryEmit(
                            CustomDynamicEntityDataSource.FeedEvent.NewObservation(
                                point,
                                attributes
                            )
                        )
                    }
                    delay(100)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                withContext(NonCancellable) {
                    _feed.tryEmit(
                        CustomDynamicEntityDataSource.FeedEvent.ConnectionFailure(
                            e,
                            true
                        )
                    )
                }
            }
        }
    }
}
