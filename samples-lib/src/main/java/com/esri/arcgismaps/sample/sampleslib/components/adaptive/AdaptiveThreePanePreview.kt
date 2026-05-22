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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface

@Composable
internal fun PreviewMainPanePlaceholder(
    isSupportingPaneVisible: Boolean,
    isFloatingPaneVisible: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                ),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Main pane: GeoView",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Primary content area",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Supporting visible: $isSupportingPaneVisible",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Floating visible: $isFloatingPaneVisible",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun AdaptiveThreePanePreviewContent(config: ThreePaneConfig = ThreePaneConfig()) {
    SamplePreviewSurface {
        AdaptiveThreePane(
            modifier = Modifier.fillMaxSize(),
            config = config,
            supportingPaneTitle = "Supporting pane title",
            floatingPaneTitle = "Floating pane title",
            mainPane = { _, _ ->
                // Shows a placeholder GeoView preview instead
                SceneView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISScene = ArcGISScene(),
                )
            },
            supportingPane = { isFloatingPaneVisible, toggleFloatingPane ->
                PreviewCard {
                    Text(
                        text = "M3 UI components like Sliders, Segmented buttons, DropDown controls live here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        if (isFloatingPaneVisible) {
                            Button(onClick = toggleFloatingPane) {
                                Text("Close floating pane")
                            }
                        } else {
                            OutlinedButton(onClick = toggleFloatingPane) {
                                Text("Show floating pane")
                            }
                        }
                    }
                }
            },
            floatingPane = {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Quick action shortcuts:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(text = "• Reset viewpoint")
                    Text(text = "• Toggle labels")
                }
            },
        )
    }
}

@Composable
internal fun PreviewCard(
    title: String = "GeoView/Sample controls:",
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        content()
    }
}


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
