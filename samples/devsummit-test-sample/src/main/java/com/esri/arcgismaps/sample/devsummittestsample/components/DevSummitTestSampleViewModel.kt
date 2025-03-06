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

package com.esri.arcgismaps.sample.devsummittestsample.components

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class DevSummitTestSampleViewModel(application: Application) : AndroidViewModel(application) {
    var map by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISNavigationNight).apply {
            initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
        }
    )

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()
    var statusMessage by mutableStateOf("")

    init {
        viewModelScope.launch {
            map.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map",
                    error.message.toString()
                )
            }
        }
    }

    fun refreshMap(reportCurrentStatus:(String) -> Unit) {
        viewModelScope.launch {
            val initTime = System.currentTimeMillis()
            val map = ArcGISMap(BasemapStyle.ArcGISNavigationNight).apply {
                initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
            }
            map.loadStatus.collect { status ->
                reportCurrentStatus(status.javaClass.simpleName)
                val currentTime = System.currentTimeMillis()
                if (status == LoadStatus.Loaded) {
                    setMessage("Time to load:${currentTime - initTime} ms")
                }
            }
        }
    }

    private fun setMessage(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }
}
