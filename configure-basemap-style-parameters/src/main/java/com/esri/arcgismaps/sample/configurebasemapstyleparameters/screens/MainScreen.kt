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

package com.esri.arcgismaps.sample.configurebasemapstyleparameters.screens

import android.app.Application
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.configurebasemapstyleparameters.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction1

/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sampleName: String) {
    // get the application context
    val application = LocalContext.current.applicationContext as Application
    // create a ViewModel to handle SceneView interactions
    val mapViewModel = MapViewModel(application)
    val languageStrategyOptions = mapViewModel.languageStrategyOptions
    val chosenStrategy: MutableState<String> = mapViewModel.chosenStrategy
    val specificLanguageOptions = mapViewModel.specificLanguageOptions
    val chosenLanguage = mapViewModel.chosenLanguage

    val setBasemap = mapViewModel::setNewBasemap

    val bottomSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize(),
                    arcGISMap = mapViewModel.map
                )
                Button(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    onClick = {
                        scope.launch {
                            showBottomSheet = true
                        }
                    },
                ) {
                    Text(
                        text = "Show controls"
                    )
                }
            }
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                    },
                    sheetState = bottomSheetState
                ) {
                    Controls(setBasemap, languageStrategyOptions, chosenStrategy, specificLanguageOptions, chosenLanguage)
                }
            }
        }
    )
}

/**
 * Displays messages to the user. This may be used to display instructions, portal info, or error messages.
 *
 */
@Composable
private fun Controls(
    setBasemap: KFunction1<String, Unit>,
    languageStrategyOptions: List<String>,
    chosenStrategy: MutableState<String>,
    specificLanguageOptions: List<String>,
    chosenLanguage: MutableState<String>
) {

    Box(
        Modifier
            .padding(8.dp)
            .height(200.dp)
            .wrapContentHeight()
    ) {


        Column {
            SetLanguageStrategy(setBasemap, languageStrategyOptions, chosenStrategy, chosenLanguage.value == "None")
            SetSpecificLanguage(setBasemap, specificLanguageOptions, chosenLanguage)
        }
    }
}

/**
 * Define the UI radio buttons for selecting language strategy: "Global" or "Local".
 */
@Composable
private fun SetLanguageStrategy(
    setBasemap: KFunction1<String, Unit>,
    languageStrategies: List<String>,
    chosenStrategy: MutableState<String>,
    enabled: Boolean
) {
    Text(
        text = "Set language strategy:"
    )

    languageStrategies.forEach { text ->
        Row(
            Modifier
                .fillMaxWidth()
                .selectable(
                    selected = (text == chosenStrategy.value),
                    onClick = {
                        if (enabled) {
                            chosenStrategy.value = text
                            setBasemap(text)
                        }
                    }
                )
        ) {
            RadioButton(
                selected = (text == chosenStrategy.value),
                onClick = {
                    if (enabled) {
                        chosenStrategy.value = text
                        setBasemap(text)
                    }
                },
                enabled = enabled
            )
            Text(
                text = text,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

/**
 * Define the UI dropdown menu for selecting specific language: "None", "Bulgarian", "Greek" or
 * "Turkish".
 */
@Composable
private fun SetSpecificLanguage(
    setBasemap: KFunction1<String, Unit>,
    specificLanguageOptions: List<String>,
    chosenLanguage: MutableState<String>
) {
    Text(
        text = "Set specific language:"
    )
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopStart)
            .padding(8.dp)
    ) {
        Text(
            chosenLanguage.value,
            modifier = Modifier
                .clickable(onClick = { expanded = true })
                .border(
                    2.dp,
                    Color.LightGray
                )
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .wrapContentSize()
        ) {
            specificLanguageOptions.forEachIndexed { index, specificLanguage ->
                DropdownMenuItem(
                    text = { Text(specificLanguage) },
                    onClick = {
                        setBasemap(specificLanguage)
                        chosenLanguage.value = specificLanguage
                        expanded = false
                    })
                // show a divider between dropdown menu options
                if (index < specificLanguageOptions.lastIndex) {
                    Divider()
                }
            }
        }
    }
}
