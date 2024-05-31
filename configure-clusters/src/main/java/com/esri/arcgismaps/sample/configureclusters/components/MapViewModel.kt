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

package com.esri.arcgismaps.sample.configureclusters.components

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.Color
import com.arcgismaps.arcgisservices.LabelingPlacement
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.labeling.LabelDefinition
import com.arcgismaps.mapping.labeling.SimpleLabelExpression
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.popup.PopupDefinition
import com.arcgismaps.mapping.reduction.AggregateField
import com.arcgismaps.mapping.reduction.AggregateStatisticType
import com.arcgismaps.mapping.reduction.ClusteringFeatureReduction
import com.arcgismaps.mapping.symbology.ClassBreak
import com.arcgismaps.mapping.symbology.ClassBreaksRenderer
import com.arcgismaps.mapping.symbology.HorizontalAlignment
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.symbology.VerticalAlignment
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


class MapViewModel(application: Application, private val sampleCoroutineScope: CoroutineScope) :
    AndroidViewModel(application) {

    private val clusteringFeatureReduction = createCustomFeatureReduction()

    // Create a mapViewProxy that will be used to identify features in the MapView.
    // This should also be passed to the composable MapView this mapViewProxy is associated with.
    val mapViewProxy = MapViewProxy()

    // Keep track of the feature layer that will be used to identify features in the MapView.
    var featureLayer: FeatureLayer? = null

    // Create a map with a feature layer that contains building data.
    val arcGISMap = ArcGISMap(
        PortalItem(
            Portal("https://www.arcgis.com"),
            "aa44e79a4836413c89908e1afdace2ea"
        )
    ).apply {
        initialViewpoint = Viewpoint(47.38, 8.53, 8e4)
        sampleCoroutineScope.launch {
            load().onSuccess {
                // Apply the custom feature reduction to the first feature layer.
                featureLayer = operationalLayers.first() as FeatureLayer
                featureLayer?.featureReduction = clusteringFeatureReduction
            }.onFailure {
                Log.e("MapViewModel", "Failed to load feature layer", it)
            }
        }
    }


    private fun createCustomFeatureReduction(): ClusteringFeatureReduction {
        // Create a class breaks renderer to apply to the custom feature reduction.
        val classBreaksRenderer = ClassBreaksRenderer().apply {
            // Define the field to use for the class breaks renderer.
            // Note that this field name must match the name of an aggregate field contained in the clustering feature reduction's aggregate fields property.
            fieldName = "Average Building Height"
            val colors = listOf(
                Color.fromRgba(4, 251, 255),
                Color.fromRgba(44, 211, 255),
                Color.fromRgba(74, 181, 255),
                Color.fromRgba(120, 135, 255),
                Color.fromRgba(165, 90, 255),
                Color.fromRgba(194, 61, 255),
                Color.fromRgba(224, 31, 255),
                Color.fromRgba(254, 1, 255)
            )
            // Add a class break for each intended value range and define a symbol to display for features in that range.
            // In this case, the average building height ranges from 0 to 8 storeys.
            // For each cluster of features with a given average building height, a symbol is defined with a specified color.
            classBreaks.add(
                ClassBreak("0", "0", 0.0, 1.0, SimpleMarkerSymbol().apply { color = colors[0] })
            )
            classBreaks.add(
                ClassBreak("1", "1", 1.0, 2.0, SimpleMarkerSymbol().apply { color = colors[1] })
            )
            classBreaks.add(
                ClassBreak("2", "2", 2.0, 3.0, SimpleMarkerSymbol().apply { color = colors[2] })
            )
            classBreaks.add(
                ClassBreak("3", "3", 3.0, 4.0, SimpleMarkerSymbol().apply { color = colors[3] })
            )
            classBreaks.add(
                ClassBreak("4", "4", 4.0, 5.0, SimpleMarkerSymbol().apply { color = colors[4] })
            )
            classBreaks.add(
                ClassBreak("5", "5", 5.0, 6.0, SimpleMarkerSymbol().apply { color = colors[5] })
            )
            classBreaks.add(
                ClassBreak("6", "6", 6.0, 7.0, SimpleMarkerSymbol().apply { color = colors[6] })
            )
            classBreaks.add(
                ClassBreak("7", "7", 7.0, 8.0, SimpleMarkerSymbol().apply { color = colors[7] })
            )

            // Define a default symbol to use for features that do not fall within any of the ranges defined by the class breaks.
            defaultSymbol = SimpleMarkerSymbol().apply { color = Color.red }
        }

        // Create a new clustering feature reduction using the class breaks renderer.
        return ClusteringFeatureReduction(classBreaksRenderer).apply {
            // Set the feature reduction's aggregate fields. Note that the field names must match the names of fields in the feature layer's dataset.
            // The aggregate fields summarize values based on the defined aggregate statistic type.
            aggregateFields.add(
                AggregateField(
                    "Total Residential Buildings",
                    "Residential_Buildings",
                    AggregateStatisticType.Sum
                )
            )
            aggregateFields.add(
                AggregateField(
                    "Average Building Height",
                    "Most_common_number_of_storeys",
                    AggregateStatisticType.Mode
                )
            )

            // Enable the feature reduction.
            isEnabled = true

            // Create a label definition with a simple label expression.
            val simpleLabelExpression = SimpleLabelExpression("[cluster_count]");
            val textSymbol = TextSymbol(
                "",
                Color.black,
                12.0f,
                HorizontalAlignment.Center,
                VerticalAlignment.Middle
            )
            val labelDefinition = LabelDefinition(simpleLabelExpression, textSymbol).apply {
                placement = LabelingPlacement.PointCenterCenter
            }

            // Add the label definition to the feature reduction.
            labelDefinitions.add(labelDefinition)

            // Set the popup definition for the custom feature reduction.
            popupDefinition = PopupDefinition(this)
            // Set values for the feature reduction's cluster minimum and maximum symbol sizes.
            // Note that the default values for Max and Min symbol size are 70 and 12 respectively.
            minSymbolSize = 5.0
            maxSymbolSize = 90.0
        }

    }

    /**
     * Identifies the tapped screen coordinate in the provided [singleTapConfirmedEvent]
     */
    fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        sampleCoroutineScope.launch {
            // identify the cluster in the feature layer on the tapped coordinate
            mapViewProxy.identify(
                featureLayer as Layer,
                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                tolerance = 12.dp,
                returnPopupsOnly = true,
                maximumResults = 1
            ).onSuccess {
                if (it.popups.isEmpty()) {
                    updateShowPopUpContentState(false)
                } else {
                    updateShowPopUpContentState(true)
                    updatePopUpTitleState(it.popups.first().title)
                    updatePopUpInfoState(it.popups.first().geoElement.attributes)
                }
            }
        }
    }

    var showClusterLabels by mutableStateOf(true)
        private set

    fun updateShowClusterLabelState(show: Boolean) {
        showClusterLabels = show
        clusteringFeatureReduction.showLabels = showClusterLabels
    }

    // Note that the default value for cluster radius is 60.
    // Increasing the cluster radius increases the number of features that are grouped together into a cluster.
    val clusterRadiusOptions = listOf(30, 45, 60, 75, 90)
    var clusterRadius by mutableIntStateOf(clusterRadiusOptions[2])
        private set

    fun updateClusterRadiusState(index: Int) {
        clusterRadius = clusterRadiusOptions[index]
        clusteringFeatureReduction.radius = clusterRadius.toDouble()
    }

    // Note that the default value for max scale is 0.
    // The max scale value is the maximum scale at which clustering is applied.
    val clusterMaxScaleOptions = listOf(0, 1000, 5000, 10000, 50000, 100000, 500000)
    var clusterMaxScale by mutableIntStateOf(clusterMaxScaleOptions[0])
        private set

    fun updateClusterMaxScaleState(index: Int) {
        clusterMaxScale = clusterMaxScaleOptions[index]
        clusteringFeatureReduction.maxScale = clusterMaxScale.toDouble()
    }

    var showPopUpContent by mutableStateOf(false)
        private set

    fun updateShowPopUpContentState(show: Boolean) {
        showPopUpContent = show
    }

    var popUpTitle by mutableStateOf("")
        private set

    fun updatePopUpTitleState(title: String) {
        popUpTitle = title
    }

    var popUpInfo by mutableStateOf<Map<String, Any?>>(emptyMap())
        private set

    fun updatePopUpInfoState(info: Map<String, Any?>) {
        popUpInfo = info
    }
}

