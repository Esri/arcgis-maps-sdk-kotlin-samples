package com.esri.arcgismaps.sample.addcustomdynamicentitydatasource.components

import android.util.Log
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
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

class SimulatedDataSource(
    private val scope: CoroutineScope,
    fileName: String,
    private val entityIdField: String,
    private val delayDuration: Long
) :
    CustomDynamicEntityDataSource.EntityFeedProvider {

    private val observationsFile = File(fileName)
    private val _feed = MutableSharedFlow<CustomDynamicEntityDataSource.FeedEvent>()
    override val feed: SharedFlow<CustomDynamicEntityDataSource.FeedEvent> = _feed
    private var emitJob: Job? = null
    
    override suspend fun onConnect() {
        emitJob = scope.launch(Dispatchers.IO) {
            if (!observationsFile.exists()) {
                _feed.emit(
                    CustomDynamicEntityDataSource.FeedEvent.ConnectionFailure(
                        IOException("Observations file does not exist."),
                        false
                    )
                )
                return@launch
            }

            try {

                // While no call to cancel the job has been made.
                while (isActive) {
                    observationsFile.bufferedReader().use { reader ->
                        // Read the next line from the file.
                        for (line in reader.lines()) {
                            // Delay the speed at which the the line.
                            delay(delayDuration)
                            // Emit the next observation.
                            _feed.emit(processNextObservation(line))
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                withContext(NonCancellable) {
                    _feed.emit(CustomDynamicEntityDataSource.FeedEvent.ConnectionFailure(e, true))
                }
            }
        }
    }

    override suspend fun onDisconnect() {
        emitJob?.cancelAndJoin()
        emitJob = null
    }

    override suspend fun onLoad(): DynamicEntityDataSourceInfo {
        return DynamicEntityDataSourceInfo(entityIdField, getSchema()).apply {
            spatialReference = SpatialReference.wgs84()
        }
    }

    private fun processNextObservation(readLine: String): CustomDynamicEntityDataSource.FeedEvent.NewObservation {

        try {
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
        } catch (ex: Exception) {
            Log.e("SimDataSource", "Error processing observation: $ex")
        }
        return TODO("Provide the return value")
    }
    private fun getSchema(): List<Field> {
        // Return a list of fields matching the attributes of each observation in the custom data source.
        return listOf(
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
