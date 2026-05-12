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

package com.esri.arcgismaps.sample.sampleslib.components.adaptive

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Heading row for the supporting pane.
 *
 * @param title Header text shown at the top of the supporting pane.
 * @param onClose Invoked when the close icon is tapped.
 */
@Composable
internal fun SupportingPaneHeader(
    title: String,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close $title",
            )
        }
    }
}

/**
 * Floating overlay container that can be dragged within the main pane bounds.
 *
 * Position is maintained as pixel offsets and clamped to remain fully visible as
 * parent or content sizes change.
 *
 * @param containerWidth Available width of the host area in pixels.
 * @param containerHeight Available height of the host area in pixels.
 * @param config Floating behavior and initial position fractions.
 * @param title Header text displayed on the floating card.
 * @param onDismiss Invoked when the floating card dismiss button is tapped.
 * @param content Slot for floating controls/content.
 */
@Composable
internal fun DraggableFloatingContainer(
    containerWidth: Int,
    containerHeight: Int,
    config: FloatingPaneConfig,
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var rawOffsetX by remember { mutableFloatStateOf(containerWidth * config.initialXFraction) }
    var rawOffsetY by remember { mutableFloatStateOf(containerHeight * config.initialYFraction) }

    val offsetX by animateFloatAsState(
        targetValue = rawOffsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "floatingPaneOffsetX",
    )
    val offsetY by animateFloatAsState(
        targetValue = rawOffsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "floatingPaneOffsetY",
    )

    var paneWidth by remember { mutableIntStateOf(0) }
    var paneHeight by remember { mutableIntStateOf(0) }

    LaunchedEffect(containerWidth, containerHeight) {
        // Keep the widget in bounds after rotations/resizes.
        rawOffsetX =
            rawOffsetX.coerceIn(0f, (containerWidth - paneWidth).toFloat().coerceAtLeast(0f))
        rawOffsetY =
            rawOffsetY.coerceIn(0f, (containerHeight - paneHeight).toFloat().coerceAtLeast(0f))
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .onSizeChanged { paneWidth = it.width; paneHeight = it.height }
            .pointerInput(containerWidth, containerHeight) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Clamp dragging so the card cannot be moved off-screen.
                        val maxX = (containerWidth - paneWidth).toFloat().coerceAtLeast(0f)
                        val maxY =
                            (containerHeight - paneHeight).toFloat().coerceAtLeast(0f)
                        rawOffsetX = (rawOffsetX + dragAmount.x).coerceIn(0f, maxX)
                        rawOffsetY = (rawOffsetY + dragAmount.y).coerceIn(0f, maxY)
                    },
                )
            },
    ) {
        FloatingWidgetCard(
            title = title,
            onDismiss = onDismiss,
            content = content,
        )
    }
}

/**
 * Visual card shell for floating controls.
 *
 * This is intentionally style-forward for samples: soft background,
 * compact header, and a clear dismiss action.
 *
 * @param title Header text shown in the floating widget card.
 * @param onDismiss Callback for the dismiss icon.
 * @param modifier Optional modifier for card positioning/styling by callers.
 * @param content Slot for custom controls rendered below the header.
 */
@Composable
internal fun FloatingWidgetCard(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .widthIn(max = 320.dp)
            .clip(RoundedCornerShape(16.dp))
            .animateContentSize()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f))
        )

        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss $title",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            content()
        }
    }
}

/**
 * Knobs for the adaptive three-pane.
 *
 * @property supportingPaneInitiallyOpen Whether the supporting pane starts open on first composition.
 * @property floatingPaneInitiallyVisible Whether the floating pane is visible on first composition.
 * @property floatingPane Configuration for initial position and drag behavior of the floating pane.
 * @property compactSupportingPaneHeightRatio Height ratio used for supporting pane when compact-stacked.
 */
data class ThreePaneConfig(
    val supportingPaneInitiallyOpen: Boolean = true,
    val floatingPaneInitiallyVisible: Boolean = false,
    val floatingPane: FloatingPaneConfig = FloatingPaneConfig(),
    val compactSupportingPaneHeightRatio: Float = 0.5f,
)

/**
 * Positioning options for the floating pane.
 *
 * @property initialXFraction Initial x offset as a fraction of the host width.
 * @property initialYFraction Initial y offset as a fraction of the host height.
 */
data class FloatingPaneConfig(
    val initialXFraction: Float = 0.05f,
    val initialYFraction: Float = 0.05f
)
