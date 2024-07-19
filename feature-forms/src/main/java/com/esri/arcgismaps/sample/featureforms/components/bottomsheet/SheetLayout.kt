/*
 * Copyright 2023 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.esri.arcgismaps.sample.featureforms.components.bottomsheet

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints.Companion.Infinity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import kotlin.math.roundToInt

/**
 * A custom layout that places the [sheetContent] in the center of the screen if the current
 * orientation is portrait. The [sheetContent] is shown as a side sheet on the right side of the
 * screen if the orientation is landscape and the [WindowSizeClass.windowWidthSizeClass] is
 * [WindowWidthSizeClass.EXPANDED] as provided by [windowSizeClass].
 *
 * @param windowSizeClass The current [WindowSizeClass].
 * @param sheetOffsetY An offset in pixels for the [sheetContent] in the Y axis.
 * @param modifier The [Modifier]
 * @param maxWidth A maximum width if specified will be enforced only when the orientation is portrait
 * and the [WindowSizeClass.windowWidthSizeClass] is not [WindowWidthSizeClass.EXPANDED]. Otherwise
 * this is set to [Infinity] which indicates to the maximum width available.
 * @param sheetContent The sheet content lambda which is passed the width and height of the layout in pixels.
 */
@Composable
fun SheetLayout(
    windowSizeClass: WindowSizeClass,
    sheetOffsetY: () -> Float,
    modifier: Modifier = Modifier,
    maxWidth: Dp = Infinity.dp,
    sheetContent: @Composable (Int, Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val showAsSideSheet = windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
        && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // convert the max width from dp into pixels
    val maxWidthInPx = with(LocalDensity.current) {
        maxWidth.roundToPx()
    }

    SubcomposeLayout(modifier = modifier) { constraints ->
        val layoutWidth = if (showAsSideSheet) {
            // set the max width to 40% of the available size
            constraints.maxWidth * 2/5
        } else {
            // set the max width to the lesser of the available size or the maxWidth
            Integer.min(constraints.maxWidth, maxWidthInPx)
        }
        // use all the available height
        val layoutHeight = constraints.maxHeight
        // measure the sheet content with the constraints
        val sheetPlaceable = subcompose(0) {
            sheetContent(layoutWidth, layoutHeight)
        }[0].measure(
            constraints.copy(
                maxWidth = layoutWidth,
                maxHeight = layoutHeight
            )
        )
        val sheetOffsetX = if (showAsSideSheet) {
            // anchor on right edge of the screen
            Integer.max(0, (constraints.maxWidth - sheetPlaceable.width))
        } else {
            // anchor in the center of the screen
            Integer.max(0, (constraints.maxWidth - sheetPlaceable.width) / 2)
        }
        layout(layoutWidth, layoutHeight) {
            sheetPlaceable.place(sheetOffsetX, sheetOffsetY().roundToInt())
        }
    }
}
