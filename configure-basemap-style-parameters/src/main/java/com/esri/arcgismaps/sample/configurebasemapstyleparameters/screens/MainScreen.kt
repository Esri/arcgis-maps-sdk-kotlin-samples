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
import androidx.compose.foundation.background
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
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.configurebasemapstyleparameters.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    // get the application context
    val application = LocalContext.current.applicationContext as Application
    // create a ViewModel to handle SceneView interactions
    val mapViewModel = MapViewModel(application)

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.map
                )
                Controls(mapViewModel)
            }
        }
    )
}

/**
 * Displays messages to the user. This may be used to display instructions, portal info, or error messages.
 *
 */
@Composable
private fun Controls(mapViewModel: MapViewModel) {
    mapViewModel.setNewBasemap("Local")
    Box(
        Modifier
            .padding(8.dp)
            .height(200.dp)
            .wrapContentHeight()
    ) {
        Column {
            SetLanguageStrategy(mapViewModel)
            SetSpecificLanguage(mapViewModel)
        }
    }
}


@Composable
private fun SetLanguageStrategy(mapViewModel: MapViewModel) {
    Text(
        text = "Set language strategy:"
    )
    val languageStrategies = listOf("Global", "Local")
    val (selectedStrategy, onStrategySelected) = remember { mutableStateOf(languageStrategies[1]) }
    languageStrategies.forEach { text ->
        Row(
            Modifier
                .fillMaxWidth()
                .selectable(
                    selected = (text == selectedStrategy),
                    onClick = {
                        onStrategySelected(text)
                    }
                )
        ) {
            RadioButton(
                selected = (text == selectedStrategy),
                onClick = {
                    onStrategySelected(text)
                    mapViewModel.setNewBasemap(text)
                }
            )
            Text(
                text = text,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun SetSpecificLanguage(mapViewModel: MapViewModel) {
    Text(
        text = "Set specific language:"
    )
    var expanded by remember { mutableStateOf(false) }
    val specificLanguages = listOf("None", "Bulgarian", "Greek", "Turkish")
    val (selectedLanguage, onLanguageSelected) = remember { mutableStateOf(specificLanguages[3]) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopStart)
            .padding(8.dp)
    ) {
        Text(
            selectedLanguage,
            modifier = Modifier
                .clickable(onClick = { expanded = true })
                .border(2.dp,
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
            specificLanguages.forEachIndexed { index, specificLanguage ->
                DropdownMenuItem(
                    text = { Text(specificLanguage) },
                    onClick = {
                        mapViewModel.setNewBasemap(specificLanguage)
                        onLanguageSelected(specificLanguage)
                        expanded = false
                    })
                // show a divider between dropdown menu options
                if (index < specificLanguages.lastIndex) {
                    Divider()
                }
            }
        }
    }
}
