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
package com.esri.arcgismaps.sample.addcustomdynamicentitydatasource.components

import com.arcgismaps.data.Field
import com.arcgismaps.data.FieldType
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.realtime.CustomDynamicEntityDataSource
import com.arcgismaps.realtime.DynamicEntityDataSourceInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import kotlin.time.Duration

/**
 * Implements the [CustomDynamicEntityDataSource.EntityFeedProvider] interface on the [CustomDynamicEntityDataSource] to provide
 * feed events from a JSON file for the custom dynamic entity data source. Uses a buffered reader to
 * process lines in the file and emit feed events for each observation. Uses the [onConnect] to
 * start reading the file and [onDisconnect] to cancel the coroutine job as required.
 *
 * @param fileName The path to the simulation file.
 * @param entityIdField The field name that will be used as the entity id.
 * @param delayDuration The delay between each observation that is processed.
 */
class CustomEntityFeedProvider(
    fileName: String,
    private val entityIdField: String,
    private val delayDuration: Duration
) : CustomDynamicEntityDataSource.EntityFeedProvider {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val observationsFile = File(fileName)

    // Create a shared flow to emit feed events.
    private val _feed = MutableSharedFlow<CustomDynamicEntityDataSource.FeedEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Expose the feed as a shared flow.
    override val feed = _feed.asSharedFlow()

    // Keep track of the feed job to allow us to properly cancel it when needed.
    private var feedJob: Job? = null

    /**
     * Called when the data source is connected. Checks for presence of the observations file and
     * starts reading the file asynchronously. It is important to process the custom data source asynchronously and let the `onConnect` function return immediately.
     */
    override suspend fun onConnect() {
        if (!observationsFile.exists()) {
            throw IOException("Observations file does not exist.")
        }

        readObservationsFileAsync()
    }

    /**
     * Called when the data source is disconnected. Cancels the coroutine job that processes the custom data source.
     */
    override suspend fun onDisconnect() {
        feedJob?.cancelAndJoin()
        feedJob = null
    }

    /**
     * Called when the data source is loaded. Defines the Dynamic Entity Data Source info.
     */
    override suspend fun onLoad(): DynamicEntityDataSourceInfo {
        return DynamicEntityDataSourceInfo(entityIdField, schema).apply {
            spatialReference = SpatialReference.wgs84()
        }
    }

    /**
     * Reads the observations file asynchronously and emits feed events for each observation.
     */
    private fun readObservationsFileAsync() {
        feedJob = scope.launch(Dispatchers.IO) {
            try {
                // While no call to cancel the job has been made.
                observationsFile.bufferedReader().use { reader ->
                    // Read the next line from the file.
                    for (line in reader.lines()) {
                        // Adjusting the value for the delay will change the speed at which the
                        // entities and their observations are displayed.
                        delay(delayDuration)
                        // Emit the next observation.
                        _feed.tryEmit(processNextObservation(line))
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Don't swallow CancellationException to maintain structured concurrency when the coroutine is cancelled.
                    throw e
                }
                withContext(NonCancellable) {
                    // Signal that an error occurred. This will change the CustomDynamicEntityDataSource's connectionStatus to Failed.
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

    /**
     * Processes the given line from the observations file and returns a new observation.
     */
    private fun processNextObservation(readLine: String): CustomDynamicEntityDataSource.FeedEvent.NewObservation {

        // Get the next observation from the file and parse it as a JSON object.
        val jsonElement = Json.parseToJsonElement(readLine)

        // Get the x and y coordinates of the observation.
        val point =
            (jsonElement.jsonObject["geometry"] as? JsonObject)?.let { geometryJsonObject ->
                // Create a new MapPoint from the x and y coordinates of the observation.
                Point(
                    geometryJsonObject["x"]!!.jsonPrimitive.double,
                    geometryJsonObject["y"]!!.jsonPrimitive.double
                )
            }
        // Get the dictionary of attributes from the observation using the field names as keys.
        val attributes = mutableMapOf<String, Any?>()
        (jsonElement.jsonObject["attributes"] as? JsonObject)?.let { attributesJsonObject ->
            attributesJsonObject.entries.forEach { (key, value) ->
                if (value is JsonPrimitive) {
                    attributes[key] = value.contentOrNull
                }
            }
        }
        // Return a new observation with the point and attributes.
        return CustomDynamicEntityDataSource.FeedEvent.NewObservation(
            point,
            attributes
        )
    }

    /**
     * Defines the schema for the custom data source.
     */
   private val schema: List<Field> by lazy { 
        listOf(
            Field(FieldType.Text, "MMSI", "", 256),
            Field(FieldType.Float64, "BaseDateTime", "", 8),
            Field(FieldType.Float64, "LAT", "", 8),
            Field(FieldType.Float64, "LONG", "", 8),
            Field(FieldType.Float64, "SOG", "", 8),
            Field(FieldType.Float64, "COG", "", 8),
            Field(FieldType.Float64, "Heading", "", 8),
            Field(FieldType.Text, "VesselName", "", 256),
            Field(FieldType.Text, "IMO", "", 256),
            Field(FieldType.Text, "CallSign", "", 256),
            Field(FieldType.Text, "VesselType", "", 256),
            Field(FieldType.Text, "Status", "", 256),
            Field(FieldType.Float64, "Length", "", 8),
            Field(FieldType.Float64, "Width", "", 8),
            Field(FieldType.Text, "Cargo", "", 256),
            Field(FieldType.Text, "globalid", "", 256)
        )
    }
}
