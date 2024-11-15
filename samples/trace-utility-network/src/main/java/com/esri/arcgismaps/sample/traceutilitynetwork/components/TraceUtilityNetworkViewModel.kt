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

package com.esri.arcgismaps.sample.traceutilitynetwork.components

import android.app.Application
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.TokenCredential
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.utilitynetworks.TraceState
import kotlinx.coroutines.launch

class TraceUtilityNetworkViewModel(application: Application) : AndroidViewModel(application) {

    val arcGISMap = ArcGISMap(
        PortalItem(
            portal = Portal.arcGISOnline(connection = Portal.Connection.Authenticated),
            itemId = "471eb0bf37074b1fbb972b1da70fb310"
        )
    )

    val mapViewProxy = MapViewProxy()

    val graphicsOverlay = GraphicsOverlay()

    val traceState = TraceState(arcGISMap, graphicsOverlay, mapViewProxy)

    init {
        viewModelScope.launch {
            val tokenCred = TokenCredential.create(
                url = "https://sampleserver7.arcgisonline.com/portal/sharing/rest",
                username = "viewer01",
                password = "I68VGU^nMurF"
            ).getOrThrow()

            ArcGISEnvironment.authenticationManager.arcGISCredentialStore.add(tokenCred)

            arcGISMap.load()
        }
    }
}
