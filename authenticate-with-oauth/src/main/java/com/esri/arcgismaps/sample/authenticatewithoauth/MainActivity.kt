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

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserCredential
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.portal.Portal
import com.arcgismaps.portal.PortalItem
import com.esri.arcgismaps.sample.authenticatewithoauth.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    // to view the traffic layer in the portal, you must enter valid ArcGIS Online credentials.
    private val portal by lazy {
        Portal(getString(R.string.portal_url), Portal.Connection.Authenticated)
    }

    private val oAuthConfiguration by lazy {
        OAuthUserConfiguration(
            portalUrl = portal.url,
            clientId = getString(R.string.oauth_client_id),
            redirectUrl = getString(R.string.oauth_redirect_uri)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // initialize the OAuth sign in view model to receive
        // the result from the activity result contract
        val oAuthUserSignInViewModel = ViewModelProvider(
            owner = this,
            factory = OAuthUserSignInViewModel.getFactory { activityResultRegistry }
        )[OAuthUserSignInViewModel::class.java]

        lifecycle.addObserver(oAuthUserSignInViewModel)
        lifecycle.addObserver(mapView)

        setUpArcGISAuthenticationChallengeHandler(oAuthUserSignInViewModel)

        // check if the portal can be loaded
        lifecycleScope.launch {
            portal.load().onSuccess {
                AlertDialog.Builder(this@MainActivity).setMessage(
                    "Portal succeeded to load, portal user: ${portal.user?.username}"
                ).create().show()
                // authentication complete, display PortalItem
                mapView.apply {
                    visibility = View.VISIBLE
                    map = ArcGISMap(PortalItem(portal.url))
                }
            }.onFailure { throwable ->
                // authentication failed, display error message
                activityMainBinding.authFailedMessage.apply {
                    visibility = View.VISIBLE
                    text = String.format("Portal failed to load, ${throwable.message}")
                }
            }
        }
    }

    /**
     * Sets up the [ArcGISAuthenticationChallengeHandler] to create an
     * [OAuthUserCredential] by launching a browser page to perform a OAuth user
     * login prompt using [oAuthUserSignInViewModel]
     */
    private fun setUpArcGISAuthenticationChallengeHandler(oAuthUserSignInViewModel: OAuthUserSignInViewModel) {
        ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
            ArcGISAuthenticationChallengeHandler { challenge ->
                if (oAuthConfiguration.canBeUsedForUrl(challenge.requestUrl)) {
                    val oAuthUserCredential =
                        OAuthUserCredential.create(oAuthConfiguration) { oAuthUserSignIn ->
                            oAuthUserSignInViewModel.promptForOAuthUserSignIn(oAuthUserSignIn)
                        }.getOrThrow()

                    ArcGISAuthenticationChallengeResponse.ContinueWithCredential(oAuthUserCredential)
                } else {
                    ArcGISAuthenticationChallengeResponse.ContinueAndFailWithError(
                        UnsupportedOperationException()
                    )
                }
            }
    }
}
