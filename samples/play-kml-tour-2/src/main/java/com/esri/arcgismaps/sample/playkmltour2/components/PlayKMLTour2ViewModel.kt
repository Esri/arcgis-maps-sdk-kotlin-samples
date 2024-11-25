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

package com.esri.arcgismaps.sample.playkmltour2.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.kml.KmlContainer
import com.arcgismaps.mapping.kml.KmlDataset
import com.arcgismaps.mapping.kml.KmlNode
import com.arcgismaps.mapping.kml.KmlTour
import com.arcgismaps.mapping.kml.KmlTourController
import com.arcgismaps.mapping.layers.KmlLayer
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import kotlin.collections.forEach

class PlayKMLTour2ViewModel(application: Application) : AndroidViewModel(application) {
    val surface = Surface().apply {
        elevationSources.add(ArcGISTiledElevationSource("https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"))
    }

    val kmlDataSet = KmlDataset(application.getExternalFilesDir(null)?.path + "/Play KML tour/Esri_tour.kmz")
    val kmlLayer = KmlLayer(kmlDataSet)

    val arcGISScene by mutableStateOf(
        ArcGISScene(BasemapStyle.ArcGISNavigationNight).apply {
            baseSurface = surface
            initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
            operationalLayers.add(kmlLayer)
        }
    )

    var kmlTour: KmlTour? = null
    val kmlTourController = KmlTourController()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISScene.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load scene",
                    error.message.toString()
                )
            }
            kmlLayer.load().onSuccess {
                println("Hello!!!!!!!!!!!!")
                kmlTour = findFirstKMLTour(kmlDataSet.rootNodes)
                kmlTourController.tour = kmlTour
            }.onFailure { error ->
                println("Error!!!!!!!!!!!!")
            }
        }
    }

    /**
     * Recursively searches for the first KML tour in a list of [kmlNodes].
     * Returns the first [KmlTour], or null if there are no tours.
     */
    private fun findFirstKMLTour(kmlNodes: List<KmlNode>): KmlTour? {
        kmlNodes.forEach { node ->
            if (node is KmlTour)
                return node
            else if (node is KmlContainer)
                return findFirstKMLTour(node.childNodes)
        }
        return null
    }
}
