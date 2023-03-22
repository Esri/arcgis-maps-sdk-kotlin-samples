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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.Lifecycle
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

private const val AUTHORIZE_URL_KEY = "KEY_INTENT_EXTRA_AUTHORIZE_URL"
private const val CUSTOM_TABS_WAS_LAUNCHED_KEY = "KEY_INTENT_EXTRA_CUSTOM_TABS_WAS_LAUNCHED"
private const val OAUTH_RESPONSE_URI_KEY = "KEY_INTENT_EXTRA_OAUTH_RESPONSE_URI"
private const val REDIRECT_URL_KEY = "KEY_INTENT_EXTRA_REDIRECT_URL"

private const val RESULT_CODE_SUCCESS = 1
private const val RESULT_CODE_CANCELED = 2

/**
 * An activity that is responsible for launching a CustomTabs activity and to receive and process
 * the redirect intent as a result of a user completing the CustomTabs prompt.
 */
class OAuthUserSignInActivity : AppCompatActivity() {

    private var customTabsWasLaunched = false
    private lateinit var redirectUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // redirect URL should be a valid string since we are adding it in the ActivityResultContract
        redirectUrl = intent.getStringExtra(REDIRECT_URL_KEY).toString()

        if (savedInstanceState != null) {
            customTabsWasLaunched = savedInstanceState.getBoolean(
                CUSTOM_TABS_WAS_LAUNCHED_KEY
            )
        }

        if (!customTabsWasLaunched) {
            val authorizeUrl = intent.getStringExtra(AUTHORIZE_URL_KEY)
            authorizeUrl?.let {
                launchCustomTabs(it)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(CUSTOM_TABS_WAS_LAUNCHED_KEY, customTabsWasLaunched)
    }

    override fun onNewIntent(customTabsIntent: Intent) {
        super.onNewIntent(customTabsIntent)
        // get the OAuth authorized URI returned from the custom tab
        customTabsIntent.data?.let { uri ->
            // the authorization code to generate the OAuth token, for example
            // in this sample app: "my-ags-app://auth?code=<AUTHORIZED_CODE>"
            val authorizationCode = uri.toString()
            // check if the URI matches with the OAuthUserConfiguration's redirectUrl
            if (authorizationCode.startsWith(redirectUrl)) {
                val intent = Intent().apply {
                    putExtra(OAUTH_RESPONSE_URI_KEY, authorizationCode)
                }
                setResult(RESULT_CODE_SUCCESS, intent)
            } else {
                // the uri likely contains an error, for example if the user hits the cancel button
                // on the custom tab prompt
                setResult(RESULT_CODE_CANCELED, Intent())
            }
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && lifecycle.currentState == Lifecycle.State.RESUMED) {
            // if we got here the user must have pressed the back button or the x button while the
            // custom tab was visible - finish by cancelling OAuth sign in
            setResult(RESULT_CODE_CANCELED, Intent())
            finish()
        }
    }

    /**
     * Launches the custom tabs activity with the provided [authorizeUrl].
     */
    private fun launchCustomTabs(authorizeUrl: String) {
        customTabsWasLaunched = true
        val intent = CustomTabsIntent.Builder().build().apply {
            intent.data = Uri.parse(authorizeUrl)
        }
        startActivity(intent.intent)
    }

    /**
     * An ActivityResultContract that takes a [OAuthUserSignIn] as input and returns a nullable
     * string as output. The output string represents a redirect URI as the result of an OAuth user
     * sign in prompt, or null if OAuth user sign in failed. This contract can be used to launch the
     * [OAuthUserSignInActivity] for a result.
     * See [Getting a result from an activity](https://developer.android.com/training/basics/intents/result)
     * for more details.
     */
    class Contract : ActivityResultContract<OAuthUserSignIn, String?>() {
        override fun createIntent(context: Context, input: OAuthUserSignIn) =
            run {
                Intent(context, OAuthUserSignInActivity::class.java).apply {
                    putExtra(AUTHORIZE_URL_KEY, input.authorizeUrl)
                    putExtra(REDIRECT_URL_KEY, input.oAuthUserConfiguration.redirectUrl)
                }
            }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            return if (resultCode == RESULT_CODE_SUCCESS) {
                intent?.getStringExtra(OAUTH_RESPONSE_URI_KEY)
            } else {
                null
            }
        }
    }
}
