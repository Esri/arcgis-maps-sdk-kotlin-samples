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

package com.esri.arcgismaps.sample.setatmosphereeffectinscene.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.view.AtmosphereEffect
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.setatmosphereeffectinscene.components.SetAtmosphereEffectInSceneViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun SetAtmosphereEffectInSceneScreen(sampleName: String) {
    val viewModel: SetAtmosphereEffectInSceneViewModel = viewModel()

    // Observe the currently selected atmosphere effect
    val currentAtmosphereEffect by viewModel.atmosphereEffect.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { paddingValues ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
                verticalArrangement = Arrangement.Center) {
                // SceneView composable that displays the 3D Scene
                // The atmosphere effect parameter controls how the sky/atmosphere is rendered.
                SceneView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    arcGISScene = viewModel.arcGISScene,
                    atmosphereEffect = currentAtmosphereEffect
                )

                // A SingleChoiceSegmentedButtonRow with 3 choices to switch atmosphere effects.
                AtmosphereEffectSelector(
                    currentEffect = currentAtmosphereEffect,
                    onEffectSelected = viewModel::updateAtmosphereEffect
                )
            }

            // Display a dialog if the sample encounters an error
            viewModel.messageDialogVM.apply {
                if (dialogStatus) {
                    MessageDialog(
                        title = messageTitle,
                        description = messageDescription,
                        onDismissRequest = ::dismissDialog
                    )
                }
            }
        }
    )
}

@Composable
private fun AtmosphereEffectSelector(
    currentEffect: AtmosphereEffect,
    onEffectSelected: (AtmosphereEffect) -> Unit
) {
    // The list of atmosphere options displayed in the segmented control.
    val effectOptions = listOf(
        AtmosphereEffect.Realistic,
        AtmosphereEffect.HorizonOnly,
        AtmosphereEffect.None
    )

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        effectOptions.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = effectOptions.size),
                onClick = { onEffectSelected(effectOptions[index]) },
                selected = currentEffect == effectOptions[index]
            ) {
                Text(text = label.javaClass.simpleName)
            }
        }
    }
}
