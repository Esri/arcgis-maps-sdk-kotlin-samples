/* Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.showmagnifier.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import kotlinx.coroutines.flow.MutableStateFlow

class MapViewModel(application: Application) : AndroidViewModel(application) {
    // get an instance of the MapView state
    val mapViewState = MapViewState()
}

/**
 * Class that represents the MapView's current state
 */
class MapViewState {
    var arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic)
    var viewpoint: Viewpoint = Viewpoint(34.056295, -117.195800, 1000000.0)
    var interactionOptions: MapViewInteractionOptions = MapViewInteractionOptions(
        isMagnifierEnabled = true,
        allowMagnifierToPan = true
    )
}

