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

package com.esri.arcgismaps.sample.showscalebar.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import kotlin.time.Duration

class ShowScaleBarViewModel(application: Application) : AndroidViewModel(application) {
    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
        initialViewpoint = Viewpoint(33.723271, -117.945793, 30452.0)
    }

    // Scale bar properties updated by the composable map view.
    var viewpoint by mutableStateOf<Viewpoint?>(null)
        private set
    var unitsPerDip by mutableDoubleStateOf(Double.NaN)
        private set
    var spatialReference by mutableStateOf<SpatialReference?>(null)
        private set

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Failed to load map",
                    description = error.message.toString()
                )
            }
        }
    }

    fun updateViewpoint(newViewpoint: Viewpoint?) {
        viewpoint = newViewpoint
    }

    fun updateUnitsPerDip(newUnitsPerDip: Double) {
        unitsPerDip = newUnitsPerDip
    }

    fun updateSpacialReference(newSpacialReference: SpatialReference?) {
        spatialReference = newSpacialReference
    }
}

fun Duration.durationToSeconds(): String {
    return if (this == Duration.INFINITE) "Infinite" else this.inWholeSeconds.toString()
}
