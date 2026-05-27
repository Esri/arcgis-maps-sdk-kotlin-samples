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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LocalSampleTopAppBarColors: ProvidableCompositionLocal<TopAppBarColors> = compositionLocalOf {
    DefaultSampleThemeTokens.topAppBarColors
}

internal val LocalSampleCardColors: ProvidableCompositionLocal<CardColors> = compositionLocalOf {
    DefaultSampleThemeTokens.cardColors
}

@Composable
internal fun ProvideSampleThemeDefaults(
    topAppBarColors: TopAppBarColors = SampleThemeDefaults.topAppBarColors(),
    cardColors: CardColors = SampleThemeDefaults.cardColors(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSampleTopAppBarColors provides topAppBarColors,
        LocalSampleCardColors provides cardColors,
        content = content
    )
}

object SampleTheme {
    val topAppBarColors: TopAppBarColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSampleTopAppBarColors.current

    val cardColors: CardColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSampleCardColors.current
}

object SampleThemeDefaults {
    @Composable
    fun topAppBarColors(
        containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
        titleContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        navigationIconContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        actionIconContentColor: Color = MaterialTheme.colorScheme.onPrimaryFixedVariant
    ): TopAppBarColors {
        return TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = titleContentColor,
            navigationIconContentColor = navigationIconContentColor,
            actionIconContentColor = actionIconContentColor
        )
    }

    @Composable
    fun cardColors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    ): CardColors {
        return CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )
    }
}
