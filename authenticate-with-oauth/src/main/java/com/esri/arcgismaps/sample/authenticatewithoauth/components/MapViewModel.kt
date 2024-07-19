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

package com.esri.arcgismaps.sample.authenticatewithoauth.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy

class MapViewModel(application: Application) : AndroidViewModel(application) {

    // This should also be passed to the composable MapView this mapViewProxy is associated with.
    val mapViewProxy = MapViewProxy()

    // The AuthenticatorState is used to manage the state to handle authentication challenges.
    val authenticatorState = AuthenticatorState().apply {
        // Set up OAuth configuration if you want to use OAuth. Leaving the oAuthoUserConfiguration
        // null will instead see the user challenged for credentials in an AlertDialog.
        oAuthUserConfiguration = OAuthUserConfiguration(
            "https://www.arcgis.com",
            // This clientId is a unique identifier associated with an application registered with
            // the portal. Your app will need its own clientId which you can create here:
            // https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
            "lgAdHkYZYlwwfAhC",
            // The URL that the OAuth server will redirect to after the user has authenticated.
            "authenticate-with-oauth://auth",
        )
    }

    // Create a map from a portal item.
    val arcGISMap = ArcGISMap(
        PortalItem(
            // Setting the portal connection to "Authenticated" will issue an authentication
            // challenge.
            portal = Portal(
                url = "https://www.arcgis.com",
                connection = Portal.Connection.Authenticated
            ),
            itemId = "e5039444ef3c48b8a8fdc9227f9be7c1"
        )
    )
}
