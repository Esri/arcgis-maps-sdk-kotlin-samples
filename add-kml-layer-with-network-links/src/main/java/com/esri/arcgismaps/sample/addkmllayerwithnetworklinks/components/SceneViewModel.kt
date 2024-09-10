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

package com.esri.arcgismaps.sample.addkmllayerwithnetworklinks.components

import kotlinx.coroutines.launch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.kml.KmlDataset
import com.arcgismaps.mapping.layers.KmlLayer
import com.esri.arcgismaps.sample.addkmllayerwithnetworklinks.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel

class SceneViewModel(application: Application) : AndroidViewModel(application) {

    // create a KML dataset from a URL, then use it to create a KML layer
    val kmlDataset = KmlDataset(application.getString(R.string.kml_dataset_url))
    val kmlLayer = KmlLayer(kmlDataset)


    // create a scene with the imagery basemap, centred over Germany and the Netherlands
    val scene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
        initialViewpoint = Viewpoint(52.0, 7.0, 8_000_000.0)
    }

    // create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    init {
        viewModelScope.launch {

            // show a popup when any network link messages are received
            kmlDataset.kmlNetworkLinkMessageReceived.collect {
                messageDialogVM.showMessageDialog(
                    title = "KML Network Link Message",
                    description = it.message
                )
            }
        }

        viewModelScope.launch {

            // wait for the KML layer to load, then add it to the scene view
            kmlLayer.load().onSuccess {
                scene.operationalLayers.add(kmlLayer)
            }.onFailure { error ->
                // report errors if the KML layer failed to load
                messageDialogVM.showMessageDialog(
                    title = "Error",
                    description = error.message.toString()
                )
            }
        }
    }
}
