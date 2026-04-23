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

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

/**
 * Helper composable to apply sample theme to the given [content] for previews.
 */
@Composable
fun SamplePreviewSurface(content: @Composable () -> Unit) {
    SampleAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) { content() }
    }
}

/**
 * Standard sample multi-preview for common adaptive form factors in light and dark modes.
 */
@Preview(name = "Pixel 7a - Light", device = Devices.PIXEL_7A, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Pixel 7a - Dark", device = Devices.PIXEL_7A, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Pixel 9 Pro XL - Light", device = Devices.PIXEL_9_PRO_XL, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Pixel 9 Pro XL - Dark", device = Devices.PIXEL_9_PRO_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Pixel Fold - Light", device = Devices.PIXEL_FOLD, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Pixel Fold - Dark", device = Devices.PIXEL_FOLD, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Pixel Tablet - Light", device = Devices.PIXEL_TABLET, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Pixel Tablet - Dark", device = Devices.PIXEL_TABLET, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class SampleDeviceLightDarkPreview

