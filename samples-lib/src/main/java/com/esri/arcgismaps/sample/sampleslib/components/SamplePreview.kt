/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.sampleslib.components

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

/**
 * Helper composable to apply sample theme to the given [content] for previews.
 */
@Composable
fun SamplePreviewSurface(content: @Composable () -> Unit) {
    SampleAppTheme {
        Surface { content() }
    }
}
