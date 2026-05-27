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

package com.esri.arcgismaps.sample.sampleslib.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.TopAppBarColors

/**
 * Static fallback values for [SampleTheme]'s composition locals.
 *
 * These are preview-safe defaults based on the generated light theme tokens.
 * Runtime code should use [SampleThemeDefaults] through [ProvideSampleThemeDefaults]
 * so values track the active [androidx.compose.material3.MaterialTheme].
 */
internal object DefaultSampleThemeTokens {
    val topAppBarColors: TopAppBarColors = TopAppBarColors(
        containerColor = primaryContainerLight,
        scrolledContainerColor = primaryContainerLight,
        navigationIconContentColor = onPrimaryContainerLight,
        titleContentColor = onPrimaryContainerLight,
        actionIconContentColor = onPrimaryContainerLight,
        subtitleContentColor = onPrimaryContainerLight
    )

    val cardColors: CardColors = CardColors(
        containerColor = surfaceContainerLowLight,
        contentColor = onSurfaceLight,
        disabledContainerColor = surfaceContainerLowLight,
        disabledContentColor = onSurfaceLight.copy(alpha = 0.38f)
    )
}