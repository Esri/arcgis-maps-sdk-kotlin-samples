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

package com.esri.arcgismaps.sample.sampleslib.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Composable component used to display a custom bottom sheet for samples.
 * The bottom sheet can display any @Composable content passed to the [bottomSheetContent],
 * and the visibility can be toggled using [isVisible]
 */
@Composable
fun BottomSheet(
    isVisible: Boolean,
    bottomSheetContent: @Composable() () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = isVisible,
            enter = slideInVertically{height -> height} + fadeIn(),
            exit = slideOutVertically{height -> height} + fadeOut()
        ) {
            bottomSheetContent()
        }
    }
}
