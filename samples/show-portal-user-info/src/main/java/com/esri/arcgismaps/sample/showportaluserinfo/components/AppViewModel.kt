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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.showportaluserinfo.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppViewModel(private val application: Application) : AndroidViewModel(application) {

    val authenticatorState: AuthenticatorState = AuthenticatorState()

    // create a ViewModel to handle dialog interactions as a public val
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    private val noPortalInfoText = application.getString(R.string.no_portal_info)
    private val startInfoText = application.getString(R.string.start_info_text)
    private val arcGISUrl = application.getString(R.string.portal_url)
    private val oAuthUserConfiguration = OAuthUserConfiguration(
        arcGISUrl,
        // This client ID is for sample purposes only. For use of the Authenticator in your own app,
        // create your own client ID. For more info see: https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
        application.getString(R.string.oauth_client_id),
        application.getString(R.string.oauth_redirect_uri)
    )

    private val _portalUserName = MutableStateFlow(String())
    val portalUserName: StateFlow<String> = _portalUserName.asStateFlow()

    private val _emailID = MutableStateFlow(String())
    val emailID: StateFlow<String> = _emailID.asStateFlow()

    private val _userCreationDate = MutableStateFlow(String())
    val userCreationDate: StateFlow<String> = _userCreationDate.asStateFlow()

    private val _portalName = MutableStateFlow(String())
    val portalName: StateFlow<String> = _portalName.asStateFlow()

    private val defaultBitmap = BitmapFactory.decodeResource(application.resources, R.drawable.user)

    private val _userThumbnail: MutableStateFlow<Bitmap> = MutableStateFlow(defaultBitmap)
    val userThumbnail: StateFlow<Bitmap> = _userThumbnail.asStateFlow()

    private val _infoText: MutableStateFlow<String> = MutableStateFlow(startInfoText)
    val infoText: StateFlow<String> = _infoText.asStateFlow()

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _url: MutableStateFlow<String> = MutableStateFlow(arcGISUrl)
    val url: StateFlow<String> = _url.asStateFlow()
    fun setUrl(newUrl: String) {
        _url.value = newUrl
    }

    fun signOut() = viewModelScope.launch {
        _isLoading.value = true
        authenticatorState.signOut()
        _infoText.value = startInfoText
        _isLoading.value = false
        _portalUserName.value = ""
        _emailID.value = ""
        _userCreationDate.value = ""
        _portalName.value = ""
        _userThumbnail.value = defaultBitmap
    }

    fun loadPortal() = viewModelScope.launch {
        _isLoading.value = true
        authenticatorState.oAuthUserConfigurations = listOf(oAuthUserConfiguration)
        val portal = Portal(url.value, Portal.Connection.Authenticated)
        portal.load().also {
            _isLoading.value = false
        }.onFailure {
            messageDialogVM.showMessageDialog(application.getString(R.string.load_portal_fail), it.message.toString())
        }.onSuccess {
            portal.portalInfo?.apply {
                _portalUserName.value = this.user?.fullName ?: noPortalInfoText
                _emailID.value = this.user?.email ?: noPortalInfoText
                _portalName.value = this.portalName ?: noPortalInfoText
                // get the created date
                val date = Date.from(this.user?.creationDate)
                val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.US)
                _userCreationDate.value = dateFormat.format(date)
                this.user?.thumbnail?.load()?.onSuccess {
                    _userThumbnail.value = this.user?.thumbnail?.image?.bitmap ?: defaultBitmap
                }
            }
            _infoText.value = application.getString(R.string.load_portal_success)
        }
    }
}
