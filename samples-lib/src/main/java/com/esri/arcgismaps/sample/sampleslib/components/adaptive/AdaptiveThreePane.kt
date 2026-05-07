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

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.PaneExpansionStateKey
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Reusable adaptive scaffold for samples that need three layers of UI:
 * - a main content pane (typically a GeoView),
 * - a supporting pane (tools, lists, settings),
 * - an optional floating pane for quick controls.
 *
 * The layout shifts automatically between compact and wide screens. In compact mode,
 * the supporting pane is stacked; in wider mode, it is arranged side-by-side and can be resized.
 *
 * This is meant for sample apps where screen real estate and discoverability matter:
 * users can focus on main content, temporarily open tools, and optionally keep quick options nearby.
 *
 * @param modifier Modifier applied to the root layout container.
 * @param config Behavior and initial-state knobs for supporting/floating panes.
 * @param supportingPaneTitle Title shown in the supporting pane header and accessibility labels.
 * @param floatingPaneTitle Title shown in the optional floating pane card header.
 * @param mainPane Main content slot. Receives current visibility flags so the content can adapt.
 * @param supportingPane Supporting content slot. Receives floating-pane state and a toggle action.
 * @param floatingPane Optional floating content slot. Pass null to disable floating options.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveThreePane(
    modifier: Modifier = Modifier,
    config: ThreePaneConfig = ThreePaneConfig(),
    supportingPaneTitle: String,
    floatingPaneTitle: String = "Options",
    mainPane: @Composable BoxScope.(
        isSupportingPaneVisible: Boolean,
        isFloatingPaneVisible: Boolean,
    ) -> Unit,
    supportingPane: @Composable ColumnScope.(
        isFloatingPaneVisible: Boolean,
        toggleFloatingPane: () -> Unit,
    ) -> Unit,
    floatingPane: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)
    val hapticFeedback = LocalHapticFeedback.current
    val isInPreview = LocalInspectionMode.current

    var isSupportingPaneOpen by rememberSaveable { mutableStateOf(config.supportingPaneInitiallyOpen) }
    var isFloatingPaneVisible by rememberSaveable { mutableStateOf(config.floatingPaneInitiallyVisible) }
    val scope = rememberCoroutineScope()

    var lastRestorableAnchorIndex by rememberSaveable {
        mutableIntStateOf(DEFAULT_EXPANSION_ANCHOR_INDEX)
    }

    BoxWithConstraints(modifier = modifier) {
        val baseDirective = calculatePaneScaffoldDirective(windowAdaptiveInfo)
        val isCompactWidth = maxWidth < 600.dp
        val compactSupportingPaneHeightRatio =
            config.compactSupportingPaneHeightRatio.coerceIn(0.25f, 0.75f)
        val preferredSupportingWidth = maxWidth / 3f

        // Build a directive tuned for the current width class.
        // Compact: vertical stacking when supporting pane is open.
        // Wide: side-by-side panes with a preferred supporting width.
        val directive = if (isCompactWidth) {
            baseDirective.copy(
                maxHorizontalPartitions = 1,
                horizontalPartitionSpacerSize = 0.dp,
                maxVerticalPartitions = if (isSupportingPaneOpen) 2 else 1,
                verticalPartitionSpacerSize = 0.dp,
                defaultPanePreferredHeight = maxHeight * compactSupportingPaneHeightRatio,
            )
        } else {
            if (isSupportingPaneOpen) {
                baseDirective.copy(defaultPanePreferredWidth = preferredSupportingWidth)
            } else {
                baseDirective.copy(
                    maxHorizontalPartitions = 1,
                    maxVerticalPartitions = 1,
                    defaultPanePreferredWidth = preferredSupportingWidth,
                )
            }
        }

        // Navigator owns which pane role is currently visible.
        val navigator = rememberSupportingPaneScaffoldNavigator<Any>(
            scaffoldDirective = directive,
            adaptStrategies = SupportingPaneScaffoldDefaults.adaptStrategies(
                mainPaneAdaptStrategy = AdaptStrategy.Hide,
                supportingPaneAdaptStrategy = AdaptStrategy.Reflow(reflowUnder = SupportingPaneScaffoldRole.Main),
                extraPaneAdaptStrategy = AdaptStrategy.Hide,
            ),
        )

        LaunchedEffect(Unit) {
            // Sync navigator state with saveable pane-open flag on first composition.
            navigator.navigateTo(
                if (isSupportingPaneOpen) SupportingPaneScaffoldRole.Supporting
                else SupportingPaneScaffoldRole.Main
            )
        }

        val isSupportingPaneVisible =
            navigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting] != PaneAdaptedValue.Hidden
        val expansionAnchors = remember {
            listOf(
                PaneExpansionAnchor.Proportion(1f / 3f),
                PaneExpansionAnchor.Proportion(1f / 2f),
                PaneExpansionAnchor.Proportion(2f / 3f),
                PaneExpansionAnchor.Proportion(4f / 5f),
            )
        }

        fun anchorProportion(index: Int): Float = expansionAnchors[index].proportion

        val expansionState = rememberPaneExpansionState(
            key = PaneExpansionStateKey.Default,
            anchors = expansionAnchors,
            initialAnchoredIndex = DEFAULT_EXPANSION_ANCHOR_INDEX,
        )

        fun currentAnchorIndex(): Int = expansionAnchors.indexOf(expansionState.currentAnchor)

        fun openSupportingPane() {
            if (isSupportingPaneVisible) return
            // Restore to the last user-friendly anchor when reopening,
            // avoiding the final "auto-close" anchor.
            val reopenAnchorIndex = lastRestorableAnchorIndex.coerceIn(
                0,
                AUTO_CLOSE_EXPANSION_ANCHOR_INDEX - 1,
            )
            expansionState.setFirstPaneProportion(anchorProportion(reopenAnchorIndex))
            isSupportingPaneOpen = true
            scope.launch { navigator.navigateTo(SupportingPaneScaffoldRole.Supporting) }
        }

        val closeSupportingPane: () -> Unit = {
            if (isSupportingPaneVisible) {
                val settledIndex = currentAnchorIndex()
                // Remember where the user left the splitter so reopen feels natural.
                lastRestorableAnchorIndex = when {
                    settledIndex in 0 until AUTO_CLOSE_EXPANSION_ANCHOR_INDEX -> settledIndex
                    else -> lastRestorableAnchorIndex
                }
                isSupportingPaneOpen = false
                scope.launch { navigator.navigateTo(SupportingPaneScaffoldRole.Main) }
            }
        }

        val toggleFloatingPane: () -> Unit = { isFloatingPaneVisible = !isFloatingPaneVisible }
        val closeFloatingPane: () -> Unit = { isFloatingPaneVisible = false }

        BackHandler(enabled = isFloatingPaneVisible || isSupportingPaneVisible) {
            when {
                isFloatingPaneVisible -> closeFloatingPane()
                isSupportingPaneVisible -> closeSupportingPane()
            }
        }

        val dragHandleInteractionSource = remember { MutableInteractionSource() }
        val isDragHandleDragged by dragHandleInteractionSource.collectIsDraggedAsState()
        var previousSettledAnchorIndex by remember { mutableIntStateOf(-1) }
        var hasUserDraggedSinceLastSettle by remember { mutableStateOf(false) }

        var scaffoldWidthPx by remember { mutableIntStateOf(0) }
        var supportingPaneWidthPx by remember { mutableIntStateOf(Int.MAX_VALUE) }

        LaunchedEffect(isDragHandleDragged) {
            if (isDragHandleDragged) hasUserDraggedSinceLastSettle = true
        }

        LaunchedEffect(isCompactWidth, isSupportingPaneOpen) {
            if (isCompactWidth || !isSupportingPaneOpen) return@LaunchedEffect

            // On wide layouts, auto-close the supporting pane if it becomes very narrow
            // after user dragging. This keeps the layout from getting stuck in a sliver state.
            snapshotFlow {
                val sw = scaffoldWidthPx
                val pw = supportingPaneWidthPx
                sw > 0 &&
                        isSupportingPaneVisible &&
                        isSupportingPaneOpen &&
                        hasUserDraggedSinceLastSettle &&
                        (1f - pw.toFloat() / sw.toFloat()) > WIDE_CLOSE_PROPORTION_THRESHOLD
            }
                .distinctUntilChanged()
                .filter { it }
                .collect { closeSupportingPane() }
        }

        LaunchedEffect(expansionState.currentAnchor) {
            val settledAnchorIndex = currentAnchorIndex()
            if (settledAnchorIndex == -1) return@LaunchedEffect

            if (!isSupportingPaneOpen) {
                hasUserDraggedSinceLastSettle = false
                return@LaunchedEffect
            }

            if (settledAnchorIndex == AUTO_CLOSE_EXPANSION_ANCHOR_INDEX) {
                // Dragging to the last anchor is treated as an explicit close gesture.
                lastRestorableAnchorIndex = DEFAULT_EXPANSION_ANCHOR_INDEX
                hasUserDraggedSinceLastSettle = false
                closeSupportingPane()
                return@LaunchedEffect
            }

            // Give light haptics when the drag handle settles to a new anchor.
            if (hasUserDraggedSinceLastSettle && previousSettledAnchorIndex != settledAnchorIndex) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            previousSettledAnchorIndex = settledAnchorIndex
            lastRestorableAnchorIndex = settledAnchorIndex
            hasUserDraggedSinceLastSettle = false
        }

        SupportingPaneScaffold(
            directive = directive,
            value = navigator.scaffoldValue,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { scaffoldWidthPx = it.width },
            mainPane = {
                AnimatedPane(modifier = Modifier.fillMaxSize()) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .animateContentSize()
                    ) {
                        val mainPaneWidthPx = constraints.maxWidth
                        val mainPaneHeightPx = constraints.maxHeight
                        if (isInPreview) {
                            PreviewMainPanePlaceholder(
                                isSupportingPaneVisible = isSupportingPaneVisible,
                                isFloatingPaneVisible = isFloatingPaneVisible,
                            )
                        } else {
                            mainPane(isSupportingPaneVisible, isFloatingPaneVisible)
                        }
                        if (floatingPane != null) {
                            AnimatedVisibility(
                                visible = isFloatingPaneVisible,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                DraggableFloatingContainer(
                                    containerWidth = mainPaneWidthPx,
                                    containerHeight = mainPaneHeightPx,
                                    config = config.floatingPane,
                                    title = floatingPaneTitle,
                                    onDismiss = closeFloatingPane,
                                    content = floatingPane,
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = !isSupportingPaneVisible,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 36.dp, end = 24.dp),
                        ) {
                            FloatingActionButton(onClick = ::openSupportingPane) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "Open $supportingPaneTitle",
                                )
                            }
                        }
                    }
                }
            },
            supportingPane = {
                AnimatedPane(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { supportingPaneWidthPx = it.width },
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SupportingPaneHeader(
                                title = supportingPaneTitle,
                                onClose = closeSupportingPane,
                            )
                            HorizontalDivider()
                            supportingPane(isFloatingPaneVisible, toggleFloatingPane)
                        }
                    }
                }
            },
            paneExpansionState = if (isCompactWidth) null else expansionState,
            paneExpansionDragHandle = { _ ->
                if (!isCompactWidth) {
                    // Splitter handle is only shown on wide layouts where panes are side-by-side.
                    VerticalDragHandle(
                        modifier = Modifier.paneExpansionDraggable(
                            state = expansionState,
                            minTouchTargetSize = LocalMinimumInteractiveComponentSize.current,
                            interactionSource = dragHandleInteractionSource,
                        ),
                        interactionSource = dragHandleInteractionSource,
                    )
                }
            },
        )
    }
}

// Default split anchor used when the user has no prior pane size preference.
private const val DEFAULT_EXPANSION_ANCHOR_INDEX = 2

// Right-most anchor that acts as a close gesture target.
private const val AUTO_CLOSE_EXPANSION_ANCHOR_INDEX = 3

// Auto-close threshold when supporting pane shrinks to a thin strip.
private const val WIDE_CLOSE_PROPORTION_THRESHOLD = 0.80f

@Preview(
    name = "Pane preview - main only day",
    widthDp = 411,
    heightDp = 891,
    showBackground = true,
)
@Preview(
    name = "Pane preview - main only night",
    widthDp = 411,
    heightDp = 891,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AdaptiveThreePaneMainOnlyPreview() {
    AdaptiveThreePanePreviewContent(
        config = ThreePaneConfig(supportingPaneInitiallyOpen = false),
    )
}

@Preview(
    name = "Pane preview - supporting open",
    widthDp = 411,
    heightDp = 891,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AdaptiveThreePaneSupportingPanePreview() {
    AdaptiveThreePanePreviewContent(
        config = ThreePaneConfig(supportingPaneInitiallyOpen = true),
    )
}

@Preview(
    name = "Pane preview - floating visible",
    widthDp = 891,
    heightDp = 411,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AdaptiveThreePaneFloatingPanePreview() {
    AdaptiveThreePanePreviewContent(
        config = ThreePaneConfig(
            supportingPaneInitiallyOpen = true,
            floatingPaneInitiallyVisible = true
        ),
    )
}

@Preview(
    name = "Phone portrait - compact 50%",
    widthDp = 411,
    heightDp = 891,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AdaptiveThreePanePhonePortraitPreview() {
    AdaptiveThreePanePreviewContent(
        config = ThreePaneConfig(
            supportingPaneInitiallyOpen = true,
            compactSupportingPaneHeightRatio = 0.5f,
        ),
    )
}

@Preview(
    name = "Phone landscape - compact 35%",
    widthDp = 891,
    heightDp = 411,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AdaptiveThreePanePhoneLandscapePreview() {
    AdaptiveThreePanePreviewContent(
        config = ThreePaneConfig(
            supportingPaneInitiallyOpen = true,
            compactSupportingPaneHeightRatio = 0.35f
        ),
    )
}

@Preview(
    name = "Tablet portrait - supporting open",
    widthDp = 800,
    heightDp = 1280,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AdaptiveThreePaneTabletPortraitPreview() {
    AdaptiveThreePanePreviewContent(
        config = ThreePaneConfig(
            supportingPaneInitiallyOpen = true,
            compactSupportingPaneHeightRatio = 0.6f
        ),
    )
}

@Preview(
    name = "Tablet landscape - floating visible",
    widthDp = 1280,
    heightDp = 800,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AdaptiveThreePaneTabletLandscapePreview() {
    AdaptiveThreePanePreviewContent(
        config = ThreePaneConfig(
            supportingPaneInitiallyOpen = true,
            floatingPaneInitiallyVisible = true
        ),
    )
}

@Preview(
    name = "Compact ratio 65%",
    widthDp = 411,
    heightDp = 891,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AdaptiveThreePaneCompactRatioPreview() {
    AdaptiveThreePanePreviewContent(
        config = ThreePaneConfig(
            supportingPaneInitiallyOpen = true,
            compactSupportingPaneHeightRatio = 0.65f
        )
    )
}
