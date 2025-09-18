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

package com.esri.arcgismaps.sample.setinitialviewpoint.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

/**
 * ViewModel for the SetInitialViewpoint sample.
 *
 * Exposes an ArcGISMap with an initial viewpoint defined by a bounding Envelope.
 */
class SetInitialViewpointViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * ArcGISMap configured with an imagery basemap and an initial viewpoint.
     * The initial viewpoint is set using a bounding geometry (Envelope) in Web Mercator.
     * The ArcGISMap instance itself is created here but loaded in the init.
     */
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISImageryStandard).apply {
            initialViewpoint = Viewpoint(
                boundingGeometry = Envelope(
                    /* Coordinates representing the area around Potash Ponds (Web Mercator) */
                    -12211308.778729,
                    4645116.003309,
                    -12208257.879667,
                    4650542.535773,
                    spatialReference = SpatialReference.webMercator()
                )
            )
        }


    // Message dialog VM to show errors to the user
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the map and report errors through the message dialog
        viewModelScope.launch {
            arcGISMap.load().onFailure { throwable ->
                messageDialogVM.showMessageDialog(throwable)
            }
        }
    }
}
