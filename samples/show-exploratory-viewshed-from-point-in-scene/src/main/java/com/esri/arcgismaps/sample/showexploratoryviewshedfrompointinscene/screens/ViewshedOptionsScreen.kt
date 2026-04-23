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

package com.esri.arcgismaps.sample.showexploratoryviewshedfrompointinscene.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.showexploratoryviewshedfrompointinscene.components.ViewshedUiState

@Composable
fun ViewshedSlidersContent(
    viewshedUiState: ViewshedUiState,
    onHeadingChanged: (Float) -> Unit = {},
    onPitchChanged: (Float) -> Unit = {},
    onHorizontalAngleChanged: (Float) -> Unit = {},
    onVerticalAngleChanged: (Float) -> Unit = {},
    onMinDistanceChanged: (Float) -> Unit = {},
    onMaxDistanceChanged: (Float) -> Unit = {}
) {
    Column {
        HeadingSlider(viewshedUiState.heading, onHeadingChanged)
        PitchSlider(viewshedUiState.pitch, onPitchChanged)
        HorizontalAngleSlider(viewshedUiState.horizontalAngle, onHorizontalAngleChanged)
        VerticalAngleSlider(viewshedUiState.verticalAngle, onVerticalAngleChanged)
        MinimumDistanceSlider(viewshedUiState.minDistance, onMinDistanceChanged)
        MaximumDistanceSlider(viewshedUiState.maxDistance, onMaxDistanceChanged)
    }
}

@Composable
fun ViewshedSceneOptionsContent(
    viewshedUiState: ViewshedUiState,
    isFrustumVisible: (Boolean) -> Unit = {},
    isAnalysisVisible: (Boolean) -> Unit = {},
    onSetViewpointToAnalysisExtent: () -> Unit = {},
    onResetViewshedOptions: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FrustumCheckBox(viewshedUiState.isFrustumVisible, isFrustumVisible)
        AnalysisCheckBox(viewshedUiState.isAnalysisVisible, isAnalysisVisible)
        OutlinedButton(onClick = onSetViewpointToAnalysisExtent) {
            Text("Align camera with viewshed")
        }
        OutlinedButton(onClick = onResetViewshedOptions) {
            Text("Reset viewshed options")
        }

    }
}

@Composable
private fun HeadingSlider(heading: Float, onHeadingChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Heading",
        sliderValue = heading,
        sliderRangeValue = 0f..360f,
        onSliderValueChanged = onHeadingChanged
    )
}

@Composable
private fun PitchSlider(pitch: Float, onPitchChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Pitch",
        sliderValue = pitch,
        sliderRangeValue = 0f..180f,
        onSliderValueChanged = onPitchChanged
    )
}

@Composable
private fun HorizontalAngleSlider(horizontalAngle: Float, onHorizontalAngleChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Horizontal Angle",
        sliderValue = horizontalAngle,
        sliderRangeValue = 1f..120f,
        onSliderValueChanged = onHorizontalAngleChanged
    )
}

@Composable
private fun VerticalAngleSlider(verticalAngle: Float, onVerticalAngleChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Vertical Angle",
        sliderValue = verticalAngle,
        sliderRangeValue = 1f..120f,
        onSliderValueChanged = onVerticalAngleChanged
    )
}

@Composable
private fun MinimumDistanceSlider(minDistance: Float, onMinDistanceChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Minimum Distance",
        sliderValue = minDistance,
        sliderRangeValue = 0f..8999f,
        onSliderValueChanged = onMinDistanceChanged
    )
}

@Composable
private fun MaximumDistanceSlider(maxDistance: Float, onMaxDistanceChanged: (Float) -> Unit) {
    ViewshedSlider(
        title = "Maximum Distance",
        sliderValue = maxDistance,
        sliderRangeValue = 0f..9999f,
        onSliderValueChanged = onMaxDistanceChanged
    )
}

@Composable
fun FrustumCheckBox(
    isChecked: Boolean,
    isFrustumVisible: (Boolean) -> Unit
) {
    Row {
        Checkbox(
            checked = isChecked,
            onCheckedChange = isFrustumVisible,
        )
        Text(modifier = Modifier.padding(top = 10.dp), text = "Frustum Outline")
    }
}

@Composable
fun AnalysisCheckBox(
    isChecked: Boolean,
    isAnalysisVisible: (Boolean) -> Unit
) {
    Row {
        Checkbox(
            checked = isChecked,
            onCheckedChange = isAnalysisVisible,
        )
        Text(modifier = Modifier.padding(top = 10.dp), text = "Analysis Overlay")
    }
}
