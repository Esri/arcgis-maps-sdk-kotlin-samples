/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.showlineofsightanalysisinmap.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShowLineOfSightAnalysisInMapViewModel(app: Application) : AndroidViewModel(app) {
    private val initVisibilityFilter = true
    private val initLineOfSightUiState = LineOfSightUiState(
        visibilityFilter = initVisibilityFilter
    )

    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISNavigationNight).apply {
            initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
        }
    )

    private val _lineOfSightUiState = MutableStateFlow(initLineOfSightUiState)
    val lineOfSightUiState = _lineOfSightUiState.asStateFlow()

    fun setVisibilityFilter(value: Boolean) {
        _lineOfSightUiState.update { it.copy(visibilityFilter = value) } // update UI state
    }

    // Create a message dialog view model for handling error messages
    // TODO: do we need this? test it for load failure
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }
}

data class LineOfSightUiState(
    val visibilityFilter: Boolean
)
