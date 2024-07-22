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

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.arcgisservices.LabelingPlacement
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.labeling.LabelDefinition
import com.arcgismaps.mapping.labeling.SimpleLabelExpression
import com.arcgismaps.mapping.layers.DynamicEntityLayer
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.realtime.ConnectionStatus
import com.arcgismaps.realtime.CustomDynamicEntityDataSource
import com.arcgismaps.realtime.DynamicEntityObservation
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.addcustomdynamicentitydatasource.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class MapViewModel(application: Application) : AndroidViewModel(application) {

    // Keep track of connected status string state.
    var connectionStatusString by mutableStateOf("")
        private set

    // Set connection status string in he UI.
    private fun updateConnectionStatusString(connectionStatus: String) {
        connectionStatusString = connectionStatus
    }

    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(
            R.string.app_name
        )
    }

    // Create a new custom feed provider that processes observations from a JSON file.
    // This takes the path to the simulation file, field name that will be used as the entity id,
    // and the delay between each observation that is processed.
    // In this example we are using a json file as our custom data source.
    // This field value should be a unique identifier for each entity.
    // Adjusting the value for the delay will change the speed at which the entities and their
    // observations are displayed.
    private val feedProvider = CustomEntityFeedProvider(
        fileName = "$provisionPath/AIS_MarineCadastre_SelectedVessels_CustomDataSource.jsonl",
        entityIdField = "MMSI",
        delayDuration = 10.milliseconds
    )

    private val dynamicEntityDataSource = CustomDynamicEntityDataSource(feedProvider).apply {
        // Observe the connection status of the custom data source.
        viewModelScope.launch {
            connectionStatus.collect { connectionStatus ->
                updateConnectionStatusString(
                    when (connectionStatus) {
                        is ConnectionStatus.Connected -> "Connected"
                        is ConnectionStatus.Disconnected -> "Disconnected"
                        is ConnectionStatus.Connecting -> "Connecting"
                        is ConnectionStatus.Failed -> "Failed"
                    }
                )
            }
        }
    }

    // Create the dynamic entity layer using the custom data source.
    private val dynamicEntityLayer = DynamicEntityLayer(dynamicEntityDataSource).apply {
        trackDisplayProperties.apply {
            // Set up the track display properties, these properties will be used to configure the appearance of the track line and previous observations.
            showPreviousObservations = true
            showTrackLine = true
            maximumObservations = 20
        }

        // Define the label expression to be used, in this case we will use the "VesselName" for each of the dynamic entities.
        val simpleLabelExpression = SimpleLabelExpression("[VesselName]")

        // Set the text symbol color and size for the labels.
        val labelSymbol = TextSymbol().apply {
            color = com.arcgismaps.Color.red
            size = 12.0F
        }

        // Add the label definition to the dynamic entity layer and enable labels.
        labelDefinitions.add(LabelDefinition(simpleLabelExpression, labelSymbol).apply {
            // Set the label position.
            placement = LabelingPlacement.PointAboveCenter
        })
        labelsEnabled = true
    }

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISOceans).apply {
        initialViewpoint = Viewpoint(47.984, -123.657, 3e6)
        // Add the dynamic entity layer to the map.
        operationalLayers.add(dynamicEntityLayer)
    }

    // Create a mapViewProxy that will be used to identify features in the MapView.
    // This should also be passed to the composable MapView this mapViewProxy is associated with.
    val mapViewProxy = MapViewProxy()

    fun dynamicEntityDataSourceConnect() =
        viewModelScope.launch { dynamicEntityDataSource.connect() }

    fun dynamicEntityDataSourceDisconnect() =
        viewModelScope.launch { dynamicEntityDataSource.disconnect() }

    // Keep track of the currently selected GeoElement.
    var selectedGeoElement by mutableStateOf<GeoElement?>(null)
        private set

    // Keep track of the most recent observation string.
    var observationString by mutableStateOf("")
        private set

    // Keep track of the Coroutine Scope where observations on being collected on, so that it can
    // be cancelled on subsequent identifies.
    private var observationsJob: Job? = null

    /**
     * Identifies the tapped screen coordinate in the provided [singleTapConfirmedEvent]
     */
    fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        viewModelScope.launch {
            // If collecting observations on a previous identify, now cancel and stop collecting.
            observationsJob?.cancelAndJoin()
            // identify the cluster in the feature layer on the tapped coordinate
            mapViewProxy.identify(
                dynamicEntityLayer as Layer,
                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                tolerance = 12.dp,
                maximumResults = 1
            ).onSuccess { result ->
                (result.geoElements.firstOrNull() as? DynamicEntityObservation)?.let { observation ->
                    // Set the identified dynamic entity, used to display the callout.
                    selectedGeoElement = observation.dynamicEntity
                    // Define a new CoroutineScope to collect observation events on.
                    observationsJob = launch(Dispatchers.IO) {
                        // Collect observation events and update the observation string accordingly.
                        observation.dynamicEntity?.dynamicEntityChangedEvent?.collect { dynamicEntityChangedInfo ->
                            // Parse the observation attributes, filter out empty values, and remove
                            // starting and ending {}s.
                            observationString =
                                dynamicEntityChangedInfo.receivedObservation?.attributes?.filter {
                                    it.value.toString().isNotEmpty()
                                }.toString().replaceFirst("{", " ").removeSuffix("}")
                                    .replace(",", "\n")
                        }
                    }
                    // If no observation is found, set the selectedGeoElement to null.
                } ?: run {
                    selectedGeoElement = null
                }
            }.onFailure { error ->
                Log.e(
                    javaClass.simpleName,
                    "Error identifying dynamic entity: ${error.message}"
                )
            }
        }
    }
}
