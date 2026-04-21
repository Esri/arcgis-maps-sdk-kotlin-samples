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

package com.esri.arcgismaps.sample.displayadaptivescene.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DisplayAdaptiveSceneViewModel(app: Application) : AndroidViewModel(app) {
    private val cameraPresets = listOf(
        CameraPreset(heading = 18f, pitch = 68f, distance = 8_000f),
        CameraPreset(heading = 110f, pitch = 58f, distance = 5_500f),
        CameraPreset(heading = 245f, pitch = 42f, distance = 2_700f)
    )

    private val focusPoint = Point(
        x = -117.1958,
        y = 34.0563,
        spatialReference = SpatialReference.wgs84()
    )

    val sceneViewProxy = SceneViewProxy()

    val arcGISScene = ArcGISScene(BasemapStyle.ArcGISImageryStandard).apply {
        initialViewpoint = Viewpoint(
            latitude = focusPoint.y,
            longitude = focusPoint.x,
            scale = 18_000.0,
            camera = cameraFromState(AdaptiveSceneUiState())
        )
    }

    private val _uiState = MutableStateFlow(AdaptiveSceneUiState())
    val uiState: StateFlow<AdaptiveSceneUiState> = _uiState

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    fun setShowAtmosphere(isEnabled: Boolean) {
        _uiState.update { it.copy(showAtmosphere = isEnabled) }
    }

    fun setHeading(heading: Float) {
        updateCameraUi(heading = heading, selectedPresetIndex = null, issueCameraCommand = true)
    }

    fun setPitch(pitch: Float) {
        updateCameraUi(pitch = pitch, selectedPresetIndex = null, issueCameraCommand = true)
    }

    fun setDistance(distance: Float) {
        updateCameraUi(distance = distance, selectedPresetIndex = null, issueCameraCommand = true)
    }

    fun resetCamera() {
        val defaultPreset = cameraPresets.first()
        updateCameraUi(
            heading = defaultPreset.heading,
            pitch = defaultPreset.pitch,
            distance = defaultPreset.distance,
            selectedPresetIndex = 0,
            issueCameraCommand = true
        )
    }

    fun applyCameraPreset(index: Int) {
        val preset = cameraPresets.getOrNull(index) ?: return
        updateCameraUi(
            heading = preset.heading,
            pitch = preset.pitch,
            distance = preset.distance,
            selectedPresetIndex = index,
            issueCameraCommand = true
        )
    }

    fun onCurrentViewpointCameraChanged(camera: Camera) {
        _uiState.update {
            it.copy(
                currentCameraState = SceneCameraState(
                    x = camera.location.x,
                    y = camera.location.y,
                    z = camera.location.z ?: 0.0,
                    heading = camera.heading.toFloat(),
                    pitch = camera.pitch.toFloat(),
                    roll = camera.roll.toFloat()
                )
            )
        }
    }

    private fun updateCameraUi(
        heading: Float? = null,
        pitch: Float? = null,
        distance: Float? = null,
        selectedPresetIndex: Int? = _uiState.value.selectedPresetIndex,
        issueCameraCommand: Boolean = false,
    ) {
        _uiState.update {
            it.copy(
                cameraHeading = heading ?: it.cameraHeading,
                cameraPitch = pitch ?: it.cameraPitch,
                cameraDistance = distance ?: it.cameraDistance,
                selectedPresetIndex = selectedPresetIndex,
                cameraCommandId = if (issueCameraCommand) it.cameraCommandId + 1 else it.cameraCommandId
            )
        }
    }

    private fun cameraFromState(state: AdaptiveSceneUiState): Camera {
        return Camera(
            lookAtPoint = focusPoint,
            distance = state.cameraDistance.toDouble(),
            heading = state.cameraHeading.toDouble(),
            pitch = state.cameraPitch.toDouble(),
            roll = 0.0
        )
    }

}

private data class CameraPreset(
    val heading: Float,
    val pitch: Float,
    val distance: Float,
)

data class AdaptiveSceneUiState(
    val showAtmosphere: Boolean = true,
    val cameraHeading: Float = 18f,
    val cameraPitch: Float = 68f,
    val cameraDistance: Float = 8_000f,
    val selectedPresetIndex: Int? = 0,
    val currentCameraState: SceneCameraState? = null,
    val cameraCommandId: Long = 0L,
)

data class SceneCameraState(
    val x: Double,
    val y: Double,
    val z: Double,
    val heading: Float,
    val pitch: Float,
    val roll: Float,
)


