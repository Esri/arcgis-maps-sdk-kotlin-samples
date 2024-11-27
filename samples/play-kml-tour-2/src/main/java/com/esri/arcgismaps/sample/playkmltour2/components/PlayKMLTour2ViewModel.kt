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
import com.arcgismaps.mapping.kml.KmlTourStatus
import com.arcgismaps.mapping.layers.KmlLayer
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.collections.forEach
import androidx.compose.runtime.setValue
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy

class PlayKMLTour2ViewModel(application: Application) : AndroidViewModel(application) {
    private val surface = Surface().apply {
        elevationSources.add(ArcGISTiledElevationSource("https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"))
    }

    private val kmlDataSet = KmlDataset(application.getExternalFilesDir(null)?.path + "/Play KML tour/Esri_tour.kmz")
    private val kmlLayer = KmlLayer(kmlDataSet)

    val arcGISScene by mutableStateOf(
        ArcGISScene(BasemapStyle.ArcGISNavigationNight).apply {
            baseSurface = surface
            initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
            operationalLayers.add(kmlLayer)
        }
    )

    val sceneViewProxy = SceneViewProxy()

    private var kmlTour: KmlTour? = null
    private val kmlTourController = KmlTourController()

    private val _statusFlow: MutableStateFlow<KmlTourStatus> = MutableStateFlow(KmlTourStatus.NotInitialized)
    val statusFlow = _statusFlow.asStateFlow()

    private val _progressFlow: MutableStateFlow<Float> = MutableStateFlow(0.0f)
    val progressFlow = _progressFlow.asStateFlow()

    //val playingStatus = mutableStateOf(false)

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
                kmlTour = findFirstKMLTour(kmlDataSet.rootNodes)
                kmlTourController.tour = kmlTour

                if (kmlTour != null) {
                    collectKmlTourStatus(kmlTour!!)
                    collectProgress(kmlTourController)
                }
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load KML tour",
                    error.message.toString()
                )
            }
        }
    }

    //var job: Job? = null

    fun playOrPause() {
        if (kmlTour!!.status.value == KmlTourStatus.Playing) {
            kmlTourController.pause()
            //job?.cancel()
            //playingStatus.value = false
        } else {
            kmlTourController.play()
            //job = collectProgress(kmlTourController)
            //playingStatus.value = true
        }
    }

    fun reset() {
        kmlTourController.reset()
        arcGISScene.initialViewpoint?.let { sceneViewProxy.setViewpoint(it) }
        //playingStatus.value = false
    }

    private fun collectProgress(kmlTourController: KmlTourController) = viewModelScope.launch {
        kmlTourController.currentPosition.combine(kmlTourController.totalDuration) { currentPosition, totalDuration ->
            (currentPosition / totalDuration).toFloat()
        }.collect { progress -> _progressFlow.emit(progress) }
    }

    private fun collectKmlTourStatus(kmlTour: KmlTour) = viewModelScope.launch {
        kmlTour.status.collect { state -> _statusFlow.emit(state) }
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
