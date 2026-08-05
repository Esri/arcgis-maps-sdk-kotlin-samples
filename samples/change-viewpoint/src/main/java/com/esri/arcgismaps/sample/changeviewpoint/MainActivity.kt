/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.changeviewpoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.changeviewpoint.screens.ChangeViewpointScreen
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SampleAppTheme {
                ChangeViewpointApp()
            }
        }
    }

    @Composable
    private fun ChangeViewpointApp() {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChangeViewpointScreen(
                sampleName = getString(R.string.change_viewpoint_app_name)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChangeViewpointScreenPreview() {
    SampleAppTheme {
        ChangeViewpointScreen(
            sampleName = "Change Viewpoint"
        )
    }
}
