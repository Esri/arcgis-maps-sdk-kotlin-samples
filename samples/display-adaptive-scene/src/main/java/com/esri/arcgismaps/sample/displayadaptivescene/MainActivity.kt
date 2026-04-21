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

package com.esri.arcgismaps.sample.displayadaptivescene

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.displayadaptivescene.screens.DisplayAdaptiveSceneScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)

        setContent {
            SampleAppTheme {
                DisplayAdaptiveSceneApp()
            }
        }
    }

    @Composable
    private fun DisplayAdaptiveSceneApp() {
        Surface(color = MaterialTheme.colorScheme.background) {
            DisplayAdaptiveSceneScreen(
                sampleName = getString(R.string.display_adaptive_scene_app_name)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveThreePaneTemplate(
    modifier: Modifier = Modifier,
    supportingPaneInitiallyOpen: Boolean = true,
    mainPane: @Composable BoxScope.(
        isSupportingPaneVisible: Boolean,
        isFloatingPaneVisible: Boolean,
        openSupportingPane: () -> Unit,
    ) -> Unit,
    supportingPane: @Composable (
        closeSupportingPane: () -> Unit,
        isFloatingPaneVisible: Boolean,
        toggleFloatingPane: () -> Unit,
    ) -> Unit,
    floatingPane: @Composable (onDismiss: () -> Unit) -> Unit,
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)
    var isSupportingPaneOpen by rememberSaveable { mutableStateOf(supportingPaneInitiallyOpen) }
    var isFloatingPaneVisible by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier) {
        val baseDirective = calculatePaneScaffoldDirective(windowAdaptiveInfo)

        val maxSupportingPaneWidth = (maxWidth * (2f / 3f))
        val minSupportingPaneWidth = (maxWidth / 3f).coerceAtMost(maxSupportingPaneWidth)
        val defaultPreferredWidth = (maxWidth / 3f).coerceIn(minSupportingPaneWidth, maxSupportingPaneWidth)

        val openDirective = baseDirective.copy(defaultPanePreferredWidth = defaultPreferredWidth)
        val directive = if (isSupportingPaneOpen) {
            openDirective
        } else {
            openDirective.copy(maxHorizontalPartitions = 1, maxVerticalPartitions = 1)
        }

        val navigator = rememberSupportingPaneScaffoldNavigator<Any>(
            scaffoldDirective = directive,
            adaptStrategies = SupportingPaneScaffoldDefaults.adaptStrategies(
                mainPaneAdaptStrategy = AdaptStrategy.Hide,
                supportingPaneAdaptStrategy = AdaptStrategy.Reflow(SupportingPaneScaffoldRole.Main),
                extraPaneAdaptStrategy = AdaptStrategy.Hide,
            ),
        )

        // Set initial destination once; subsequent transitions are event-driven in handlers.
        LaunchedEffect(Unit) {
            navigator.navigateTo(
                if (isSupportingPaneOpen) {
                    SupportingPaneScaffoldRole.Supporting
                } else {
                    SupportingPaneScaffoldRole.Main
                }
            )
        }

        val isSupportingPaneVisible =
            navigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting] != PaneAdaptedValue.Hidden

        fun openSupportingPane() {
            if (isSupportingPaneOpen) return
            isSupportingPaneOpen = true
            scope.launch { navigator.navigateTo(SupportingPaneScaffoldRole.Supporting) }
        }

        fun closeSupportingPane() {
            if (!isSupportingPaneOpen) return
            isSupportingPaneOpen = false
            scope.launch { navigator.navigateTo(SupportingPaneScaffoldRole.Main) }
        }

        fun toggleFloatingPane() {
            isFloatingPaneVisible = !isFloatingPaneVisible
        }

        fun closeFloatingPane() {
            isFloatingPaneVisible = false
        }

        BackHandler(enabled = isFloatingPaneVisible || isSupportingPaneVisible) {
            if (isFloatingPaneVisible) {
                closeFloatingPane()
            } else if (isSupportingPaneVisible) {
                closeSupportingPane()
            }
        }

        SupportingPaneScaffold(
            directive = directive,
            value = navigator.scaffoldValue,
            modifier = Modifier.fillMaxSize(),
            mainPane = {
                AnimatedPane(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        mainPane(isSupportingPaneVisible, isFloatingPaneVisible, ::openSupportingPane)
                        if (isFloatingPaneVisible) {
                            floatingPane(::closeFloatingPane)
                        }
                    }
                }
            },
            supportingPane = {
                AnimatedPane(modifier = Modifier) {
                    supportingPane(::closeSupportingPane, isFloatingPaneVisible, ::toggleFloatingPane)
                }
            },
            paneExpansionState = rememberPaneExpansionState(navigator.scaffoldValue),
            paneExpansionDragHandle = { state ->
                val interactionSource = remember { MutableInteractionSource() }
                VerticalDragHandle(
                    modifier = Modifier.paneExpansionDraggable(
                        state = state,
                        minTouchTargetSize = LocalMinimumInteractiveComponentSize.current,
                        interactionSource = interactionSource
                    ),
                    interactionSource = interactionSource
                )
            }
        )
    }
}

