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

package com.esri.arcgismaps.sample.showportaluserinfo.screens

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.arcgismaps.toolkit.authentication.DialogAuthenticator
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showportaluserinfo.components.AuthenticationAppViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String, application: Application) {

    val authenticationAppViewModel = viewModel { AuthenticationAppViewModel(application) }
    val authenticatorState: AuthenticatorState = authenticationAppViewModel.authenticatorState

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                val infoText = authenticationAppViewModel.infoText.collectAsState().value
                val isLoading = authenticationAppViewModel.isLoading.collectAsState().value
                PortalDetails(
                    url = authenticationAppViewModel.url.collectAsState().value,
                    onSetUrl = authenticationAppViewModel::setUrl,
                    onSignOut = authenticationAppViewModel::signOut,
                    onLoadPortal = authenticationAppViewModel::loadPortal
                )
                InfoScreen(infoText = infoText,
                    username = authenticationAppViewModel.portalUserName.collectAsState().value,
                    email =  authenticationAppViewModel.emailID.collectAsState().value,
                    creationDate = authenticationAppViewModel.userCreationDate.collectAsState().value,
                    portalName = authenticationAppViewModel.portalName.collectAsState().value,
                    userThumbnail = authenticationAppViewModel.userThumbnail.collectAsState().value,
                    isLoading = isLoading)
            }
            DialogAuthenticator(authenticatorState = authenticatorState)
        }
    )
}

/**
 * Allows the user to enter a url and load a portal.
 * It uses OAuth under the hood, and has a button to clear credentials.
 *
 * @param url the string url to display in the text field
 * @param onSetUrl called when the url should be changed
 * @param onSignOut called when any stored credentials should be cleared
 * @param onLoadPortal called when the [url] should be loaded
 * @since 200.2.0
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PortalDetails(
    url: String,
    onSetUrl: (String) -> Unit,
    onSignOut: () -> Unit,
    onLoadPortal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // The Url text field
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = url,
            onValueChange = onSetUrl,
            label = { Text("Portal URL") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onAny = { onLoadPortal() }),
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current
            // Clear credential button
            Button(
                onClick = {
                    onSignOut()
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text(text = "Sign out")
            }
            // Load button
            Button(
                onClick = {
                    onLoadPortal()
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "Load")
            }
        }
    }
}

/**
 * Displays messages to the user. This may be used to display instructions, portal info, or error messages.
 *
 * @param infoText the text to display
 * @param username to display username
 * @param email to display email
 * @param creationDate to display creationDate
 * @param portalName to display portalName
 * @param userThumbnail to display userThumbnail
 * @param isLoading whether a progress indicator should be displayed
 * @since 200.2.0
 */
@Composable
private fun InfoScreen(
    infoText: String,
    username: String,
    email: String,
    creationDate: String,
    portalName: String,
    userThumbnail: Bitmap?,
    isLoading: Boolean
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(8.dp),
    ) {
        LazyColumn {
            item {
                Text(text = infoText, modifier = Modifier.padding( bottom = 10.dp))
                Divider()
                Row(modifier = Modifier.padding(10.dp)) {
                    Text(text = "Portal Name: ", fontWeight = FontWeight.Bold)
                    Text(text = portalName)
                }
                Divider()
                Row(modifier = Modifier.padding(10.dp)) {
                    Text(text = "Username: ", fontWeight = FontWeight.Bold)
                    Text(text = username)
                }
                Divider()
                Row(modifier = Modifier.padding(10.dp)) {
                    Text(text = "E-mail: ", fontWeight = FontWeight.Bold)
                    Text(text = email)
                }
                Divider()
                Row(modifier = Modifier.padding(10.dp)) {
                    Text(text = "Member Since: ", fontWeight = FontWeight.Bold)
                    Text(text = creationDate)
                }
                Divider()
                Row(modifier = Modifier.padding(10.dp)) {
                    Text(text = "Thumbnail: ", fontWeight = FontWeight.Bold)
                    if (userThumbnail != null) {
                        Image(
                            bitmap = userThumbnail.asImageBitmap(),
                            contentDescription = "User Thumbnail"
                        )
                    }
                }
                Divider()
            }
        }
        if (isLoading) CircularProgressIndicator()
    }
}