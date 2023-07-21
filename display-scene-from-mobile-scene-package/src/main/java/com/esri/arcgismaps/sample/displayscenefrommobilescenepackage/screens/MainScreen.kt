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

package com.esri.arcgismaps.sample.displayscenefrommobilescenepackage.screens

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.esri.arcgismaps.sample.displayscenefrommobilescenepackage.components.ComposeSceneView
import com.esri.arcgismaps.sample.displayscenefrommobilescenepackage.components.SceneViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String, application: Application) {
    // create a ViewModel to handle SceneView interactions
    val sampleCoroutineScope = rememberCoroutineScope()
    val sceneViewModel = remember { SceneViewModel(application, sampleCoroutineScope) }

    // observe the snackbar message from the viewmodel
    val snackbarMessage by sceneViewModel.snackbarMessage.collectAsState()

    // create a snackbarHostState to manage snackbar display
    val snackbarHostState = remember { SnackbarHostState() }

    // use LaunchedEffect to trigger the display of the snackbar when snackbarMessage is not empty.
    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage?.isNotEmpty() == true) {
            // show the snackbar and set the duration to short.
            snackbarHostState.showSnackbar(
                message = snackbarMessage!!,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                // composable function that wraps the SceneView
                ComposeSceneView(
                    modifier = Modifier.fillMaxSize(),
                    sceneViewModel = sceneViewModel,
                    onSingleTap = {
                        sceneViewModel.onSingleTap()
                    }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    )
}
