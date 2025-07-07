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

package com.esri.arcgismaps.sample.augmentrealitytocollectdata.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.ElevationSource
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class AugmentRealityToCollectDataViewModel(app: Application) : AndroidViewModel(app) {
    val basemap = Basemap(BasemapStyle.ArcGISHumanGeography)
    val arcGISScene = ArcGISScene(basemap).apply {
        // an elevation source is required for the scene to be placed at the correct elevation
        // if not used, the scene may appear far below the device position because the device position
        // is calculated with elevation
        baseSurface.elevationSources.add(ElevationSource.fromTerrain3dService())
        baseSurface.backgroundGrid.isVisible = false
        operationalLayers.add(ArcGISSceneLayer("https://services2.arcgis.com/ZQgQTuoyBrtmoGdP/arcgis/rest/services/AR_Tree_Survey/FeatureServer/0"))
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
//            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }
}
