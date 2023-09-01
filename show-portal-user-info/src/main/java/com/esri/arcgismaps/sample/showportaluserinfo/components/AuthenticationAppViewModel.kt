/*
 *
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.esri.arcgismaps.sample.showportaluserinfo.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.arcgismaps.toolkit.authentication.signOut
import com.esri.arcgismaps.sample.showportaluserinfo.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthenticationAppViewModel(application: Application) : AndroidViewModel(application) {

    val authenticatorState: AuthenticatorState = AuthenticatorState()

    private val noPortalInfoText = application.getString(R.string.no_portal_info)
    private val startInfoText = application.getString(R.string.start_info_text)
    private val arcGISUrl = "https://www.arcgis.com"
    private val oAuthUserConfiguration = OAuthUserConfiguration(
        arcGISUrl,
        // This client ID is for demo purposes only. For use of the Authenticator in your own app,
        // create your own client ID. For more info see: https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
        "lgAdHkYZYlwwfAhC",
        "my-ags-app://auth"
    )

    private val _infoText: MutableStateFlow<String> = MutableStateFlow(startInfoText)
    val infoText: StateFlow<String> = _infoText.asStateFlow()

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _url: MutableStateFlow<String> = MutableStateFlow(arcGISUrl)
    val url: StateFlow<String> = _url.asStateFlow()
    fun setUrl(newUrl: String) { _url.value = newUrl }

    fun signOut() = viewModelScope.launch {
        _isLoading.value = true
        ArcGISEnvironment.authenticationManager.signOut()
        _infoText.value = startInfoText
        _isLoading.value = false
    }

    fun loadPortal() = viewModelScope.launch {
        _isLoading.value = true
        authenticatorState.oAuthUserConfiguration = oAuthUserConfiguration
        val portal = Portal(url.value, Portal.Connection.Authenticated)
        portal.load().also {
            _isLoading.value = false
        }.onFailure {
            _infoText.value = it.toString()
        }.onSuccess {
            val text = portal.portalInfo?.let {
                it.user?.username
            } ?: noPortalInfoText
            _infoText.value = text
        }
    }
}
