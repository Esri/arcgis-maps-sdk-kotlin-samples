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

package com.esri.arcgismaps.sample.authenticatewithoauth

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

private const val ACTIVITY_RESULT_REGISTRY_KEY = "KEY_ACTIVITY_RESULT_REGISTRY"

/**
 * A ViewModel responsible for launching an OAuth user sign in prompt and managing the
 * [OAuthUserSignIn] object associated with the OAuth prompt.
 * Uses [OAuthUserSignInActivity.Contract] to launch an `OAuthUserSignInActivity` and to receive
 * the result of the prompt. The result is used to complete the associated `OAuthUserSignIn`.
 */
class OAuthUserSignInViewModel(
    private val getRegistry : () -> ActivityResultRegistry
) : ViewModel(), DefaultLifecycleObserver {

    private lateinit var oAuthLauncher : ActivityResultLauncher<OAuthUserSignIn>

    private var pendingSignIn: OAuthUserSignIn? = null

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        oAuthLauncher = getRegistry().register(
            ACTIVITY_RESULT_REGISTRY_KEY,
            owner,
            OAuthUserSignInActivity.Contract()
        ) { redirectUri ->
            complete(redirectUri)
        }
    }

    fun promptForOAuthUserSignIn(oAuthUserSignIn: OAuthUserSignIn) {
        pendingSignIn = oAuthUserSignIn
        oAuthLauncher.launch(pendingSignIn!!)
    }

    private fun complete(redirectUri: String?) {
        pendingSignIn?.let { pendingSignIn ->
            redirectUri?.let { redirectUri ->
                pendingSignIn.complete(redirectUri)
            } ?: pendingSignIn.cancel()
        } ?: throw IllegalStateException("OAuthUserSignIn not available for completion")
    }

    companion object {
        /**
         * Gets the view model factory for this [OAuthUserSignInViewModel].
         */
        fun getFactory(
            getRegistry : () -> ActivityResultRegistry
        ) : ViewModelProvider.Factory  = viewModelFactory {
            initializer {
                OAuthUserSignInViewModel(getRegistry)
            }
        }
    }
}
