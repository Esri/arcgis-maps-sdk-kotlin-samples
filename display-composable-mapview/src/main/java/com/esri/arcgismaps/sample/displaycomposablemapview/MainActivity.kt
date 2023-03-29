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

package com.esri.arcgismaps.sample.displaycomposablemapview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.esri.arcgismaps.sample.displaycomposablemapview.theme.SampleAppTheme

class MainActivity : ComponentActivity() {

    private val viewpointRef1 = Viewpoint(39.8, -98.6, 10e7)
    private val viewpointRef2 = Viewpoint(39.8, 98.6, 10e7)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        setContent {
            SampleAppTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // a mutable or immutable state is computed by remember to stored the
                    // Composition during initial composition, and the stored value
                    // is returned during recomposition/state change.
                    var viewpoint by remember { mutableStateOf(viewpointRef1) }

                    // Composable function that wraps the MapView
                    MapViewWithCompose(
                        lifecycle = lifecycle,
                        arcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight),
                        viewpoint = viewpoint,
                        // lambda to retrieve the MapView's onSingleTapConfirmed
                        onSingleTap = {
                            // swap between two America and Asia viewpoints
                            viewpoint =
                                if (viewpoint == viewpointRef1) viewpointRef2 else viewpointRef1
                        }
                    )
                }
            }
        }
    }
}
