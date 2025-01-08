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

package com.esri.arcgismaps.sample.traceutilitynetwork.components

import android.app.Application
import android.graphics.Point
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.httpcore.authentication.TokenCredential
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.UniqueValue
import com.arcgismaps.mapping.symbology.UniqueValueRenderer
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.utilitynetworks.UtilityDomainNetwork
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityNetwork
import com.arcgismaps.utilitynetworks.UtilityNetworkSourceType
import com.arcgismaps.utilitynetworks.UtilityTier
import com.arcgismaps.utilitynetworks.UtilityTraceParameters
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.net.URL

class TraceUtilityNetworkViewModel(application: Application) : AndroidViewModel(application) {

    // Create a message dialog view model for handling error messages.
    val messageDialogVM = MessageDialogViewModel()

    // MapView proxy used by the trace tool.
    val mapViewProxy = MapViewProxy()

    // Graphics overlay used by the trace tool.
    val graphicsOverlay = GraphicsOverlay()

    private var mediumVoltageTier: UtilityTier? = null


    private val sampleServer7: URL
        get() = URL("https://sampleserver7.arcgisonline.com")

    private val featureService: URL
        get() = URL("${sampleServer7}/server/rest/services/UtilityNetwork/NapervilleElectric/FeatureServer")

    private val samplePortal: URL
        get() = URL("${sampleServer7}/portal/sharing/rest")

    // The domain network for this sample.
    private val electricDistribution: UtilityDomainNetwork? =
        utilityNetwork.definition?.domainNetworks?.find { it.name == "ElectricDistribution" }

    // The URLs of the relevant feature layers for this sample.
    // The feature layers allow us to modify the visual rendering style of different elements in
    // the network.
    // - Note: The electric distribution line feature layer (ID 3) is placed on the map first,
    // followed by the electric distribution device feature layer (ID 0) so that the junction
    // based features in the latter feature layer are easier to select.
    private val featureLayerURLs: List<URL>
        get() = listOf(
            URL("$featureService/3"),
            URL("$featureService/0")
        )

    // The textual hint shown to the user.
    var hint: String = ""

    // The last element that was added to either the list of starting points or barriers.
    // When an element contains more than one terminal, the user should be presented with the
    // option to select a terminal. Keeping a reference to the last added element provides ease
    // of access to save the user's choice.
    var lastAddedElement: UtilityElement? = null

    // The last locations in the screen and map where a tap occurred.
    // Monitoring these values allows for an asynchronous identification task when they change.
    var lastSingleTap: Point? = null

    // Web-map contains the utility network and operational layers
    // on which trace results will be selected.
    val arcGISMap = ArcGISMap(
        PortalItem(
            portal = Portal.arcGISOnline(connection = Portal.Connection.Authenticated),
            itemId = "471eb0bf37074b1fbb972b1da70fb310"
        )
    )

    /// The utility tier for this sample.
    private var mediumVoltageRadial: UtilityTier? =
        electricDistribution?.tiers?.find { utilityTier: UtilityTier ->
            utilityTier.name == "Medium Voltage Radial"
        }

    // The utility network for this sample.
    private val utilityNetwork: UtilityNetwork = arcGISMap.utilityNetworks.first()

    // The parameters for the pending trace.
    // Important trace information like the trace type, starting points, and barriers is
    // contained within this value.
    var pendingTraceParameters: UtilityTraceParameters? = null

    // A Boolean value indicating whether the terminal selection menu is open.
    // When a utility element has more than one terminal, the user is presented with a menu of the
    // available terminal names.
    var terminalSelectorIsOpen = false

    // The current tracing related activity.
    var tracingActivity: TracingActivity? = null

    // The graphics overlay on which starting point and barrier symbols will be drawn.
    var points: GraphicsOverlay = GraphicsOverlay().apply {
        renderer = UniqueValueRenderer(
            fieldNames = PointType.entries.map { it.name },
            uniqueValues = listOf(
                UniqueValue(
                    symbol = barrierPointSymbol,
                    values = listOf(PointType.BARRIER)
                )
            ),
            defaultSymbol = startingPointSymbol
        )
    }

    // create symbols for the starting point and barriers
    private val startingPointSymbol: SimpleMarkerSymbol by lazy {
        SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.green, 25f)
    }
    private val barrierPointSymbol: SimpleMarkerSymbol by lazy {
        SimpleMarkerSymbol(SimpleMarkerSymbolStyle.X, Color.red, 25f)
    }

    init {
        // Define user credentials for authenticating with the service
        viewModelScope.launch {
            // A licensed user is required to perform utility network operations
            TokenCredential.create(
                url = "https://sampleserver7.arcgisonline.com/portal/sharing/rest",
                username = "viewer01",
                password = "I68VGU^nMurF"
            ).onSuccess { tokenCredential ->
                ArcGISEnvironment.authenticationManager.arcGISCredentialStore.add(tokenCredential)
                initializeUtilityNetwork()
            }.onFailure {
                messageDialogVM.showMessageDialog(
                    title = it.message.toString(),
                    description = it.cause.toString()
                )
            }
        }
    }

    private suspend fun initializeUtilityNetwork() {
        arcGISMap.load()

    }


    /**
     * Adds the provided utility [element] to the parameters of the pending trace and a corresponding
     * starting [point] or barrier graphic to the map.
     *
     * Adding custom attributes to the graphic allows us to apply different rendering styles
     * for starting point and barrier graphics.
     */
    private fun add(element: UtilityElement, point: Geometry) {
        if (pendingTraceParameters == null || tracingActivity !is TracingActivity.SettingPoints)
            return

        val pointType = (tracingActivity as TracingActivity.SettingPoints).pointType

        val graphic = Graphic(
            geometry = point,
            attributes = mapOf(PointType::class.java.simpleName to pointType)
        )

        when (pointType) {
            PointType.BARRIER -> pendingTraceParameters?.barriers?.add(element)
            PointType.START -> pendingTraceParameters?.startingLocations?.add(element)
        }

        points.graphics.add(graphic)
        lastAddedElement = element
    }

    /**
     * Adds a provided feature to the pending trace.
     * For junction features with more than one terminal,
     * the user should be prompted to pick a terminal.
     * For edge features,the fractional point along the feature's edge should be computed.
     */
    fun add(feature: ArcGISFeature, mapPoint: Point) {
        val element = utilityNetwork.createElementOrNull(arcGISFeature = feature)
        val geometry = feature.geometry
        val table = feature.featureTable as? ArcGISFeatureTable

        val networkSource = utilityNetwork.definition?.getNetworkSource(
            networkSourceName = table?.tableName.toString()
        )

        when (networkSource?.sourceType) {
            UtilityNetworkSourceType.Junction -> {
                add(element, at = geometry)
                if (element.assetType.terminalConfiguration?.terminals?.size ?: 0 > 1) {
                    terminalSelectorIsOpen = !terminalSelectorIsOpen
                }

            }

            UtilityNetworkSourceType.Edge -> {
                val line = GeometryEngine.makeGeometry(geometry, z = null) as? GeometryType.Polyline
                if (line != null) {
                    element.fractionAlongEdge = GeometryEngine.polyline(
                        line,
                        fractionalLengthClosestTo = mapPoint,
                        tolerance = -1
                    )
                    updateUserHint(
                        withMessage = String.format(
                            "fractionAlongEdge: %.3f",
                            element.fractionAlongEdge
                        )
                    )
                    add(element, at = mapPoint)

                }
            }

            null -> {
                // Handle unknown case if necessary
            }
        }

        if (element != null && geometry != null && table != null && networkSource != null) {

        } else {
            updateUserHint("An error occurred while adding element to the trace.")
        }
    }

    fun updateUserHint(message: String? = null) {
        if (message != null) {
            hint = message
        } else {
            when (tracingActivity) {
                TracingActivity.TraceCompleted -> {
                    hint = "Trace completed."
                }

                is TracingActivity.SettingPoints -> {
                    when (tracingActivity.pointType) {
                        PointType.START -> {
                            hint = "Tap on the map to add a start location."
                        }

                        PointType.BARRIER -> {
                            hint = "Tap on the map to add a barrier."
                        }
                    }
                }

                is TracingActivity.TraceFailed -> {
                    hint = "Trace failed.\n${tracingActivity.description}"
                }

                TracingActivity.TraceRunning -> {
                    hint = ""
                }

                null -> {
                    hint = ""
                }
            }
        }
    }

    // The types of points used during a utility network trace.
    enum class PointType(val label: String) {
        // A point which marks a location for a trace to stop.
        BARRIER("Barrier"),

        // A point which marks a location for a trace to begin.
        START("Start")
    }

    // The different states of a utility network trace.
    sealed class TracingActivity {
        // Starting points and barriers are being added.
        data class SettingPoints(val pointType: PointType) : TracingActivity()

        // The trace is running.
        data object TraceRunning : TracingActivity()

        // The trace completed successfully.
        data object TraceCompleted : TracingActivity()

        // The trace failed.
        data class TraceFailed(val description: String) : TracingActivity()


    }

}
