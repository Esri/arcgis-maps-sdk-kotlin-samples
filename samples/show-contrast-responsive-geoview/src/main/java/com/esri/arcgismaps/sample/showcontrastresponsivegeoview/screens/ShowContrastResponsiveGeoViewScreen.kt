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

package com.esri.arcgismaps.sample.showcontrastresponsivegeoview.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDeviceLightDarkPreview
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.components.adaptive.AdaptiveThreePane
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.R
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.BasemapLayerRole
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.ContrastAppearance
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.ContrastMode
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.ContrastUiState
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.DeviceContrastSettings
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.GeoViewType
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.ShowContrastResponsiveGeoViewViewModel
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.rememberDeviceContrastSettings

/**
 * Entry composable that owns the ViewModel and passes stateless UI data into the scaffold.
 */
@Composable
fun ShowContrastResponsiveGeoViewScreen() {
    val viewModel: ShowContrastResponsiveGeoViewViewModel = viewModel()
    val contrastUiState by viewModel.contrastUiState.collectAsStateWithLifecycle()
    val deviceContrastSettings = rememberDeviceContrastSettings()
    val automaticAppearance = remember(deviceContrastSettings) {
        deviceContrastSettings.toAppearance()
    }
    val effectiveAppearance = if (contrastUiState.contrastMode == ContrastMode.Automatic) {
        automaticAppearance
    } else {
        contrastUiState.manualAppearance
    }

    LaunchedEffect(effectiveAppearance) {
        viewModel.updateGeoViewAppearance(effectiveAppearance)
    }

    MainScreenScaffold(
        contrastUiState = contrastUiState,
        onContrastModeChanged = viewModel::updateContrastMode,
        onManualAppearanceChanged = viewModel::updateManualAppearance,
        onGeoViewTypeChanged = viewModel::updateGeoViewType,
        onReferenceLayerVisibilityChanged = viewModel::updateReferenceLayerVisibility,
        mainPaneContent = {
            if (contrastUiState.geoViewType == GeoViewType.Map) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(),
                    arcGISMap = viewModel.arcGISMap
                )
            } else {
                SceneView(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(),
                    arcGISScene = viewModel.arcGISScene
                )
            }
        }
    )

    viewModel.messageDialogVM.apply {
        if (dialogStatus) {
            MessageDialog(
                title = messageTitle,
                description = messageDescription,
                onDismissRequest = ::dismissDialog
            )
        }
    }
}

/**
 * Previewable scaffold composable with no direct ViewModel or ArcGIS object dependencies.
 */
@Composable
private fun MainScreenScaffold(
    contrastUiState: ContrastUiState,
    onContrastModeChanged: (ContrastMode) -> Unit = {},
    onManualAppearanceChanged: (ContrastAppearance) -> Unit = {},
    onGeoViewTypeChanged: (GeoViewType) -> Unit = {},
    onReferenceLayerVisibilityChanged: (Boolean) -> Unit = {},
    mainPaneContent: @Composable BoxScope.() -> Unit,
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.show_contrast_responsive_geoview_app_name)) },
        content = { paddingValues ->
            AdaptiveThreePane(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                supportingPaneTitle = "Contrast Options",
                floatingPaneTitle = "Sample State",
                mainPane = { _, _ -> mainPaneContent() },
                supportingPane = { _, _ ->
                    ContrastSupportingContent(
                        contrastUiState = contrastUiState,
                        onContrastModeChanged = onContrastModeChanged,
                        onManualAppearanceChanged = onManualAppearanceChanged,
                        onGeoViewTypeChanged = onGeoViewTypeChanged,
                        onReferenceLayerVisibilityChanged = onReferenceLayerVisibilityChanged
                    )
                }
            )
        }
    )
}

@Composable
private fun ContrastSupportingContent(
    contrastUiState: ContrastUiState,
    onContrastModeChanged: (ContrastMode) -> Unit,
    onManualAppearanceChanged: (ContrastAppearance) -> Unit,
    onGeoViewTypeChanged: (GeoViewType) -> Unit,
    onReferenceLayerVisibilityChanged: (Boolean) -> Unit
) {
    SelectionSection(title = "Select GeoView") {
        ToggleButtonRow(
            options = GeoViewType.entries,
            selectedOption = contrastUiState.geoViewType,
            optionLabel = GeoViewType::displayName,
            onOptionSelected = onGeoViewTypeChanged
        )
    }

    HorizontalDivider()

    if (contrastUiState.availableLayers.any { it.role == BasemapLayerRole.Reference }) {
        HorizontalDivider()
        ReferenceLayerToggleRow(
            referenceLayersVisible = contrastUiState.referenceLayersVisible,
            onReferenceLayerVisibilityChanged = onReferenceLayerVisibilityChanged
        )
    }

    HorizontalDivider()

    SelectionSection(title = "Select visual contrast mode") {
        ContrastMode.entries.forEach { mode ->
            SelectionRow(
                title = mode.displayName,
                description = if (mode == ContrastMode.Automatic) {
                    "Use device light, dark, and high-contrast settings to auto-select web map."
                } else {
                    "Choose one of the four web maps manually."
                },
                selected = contrastUiState.contrastMode == mode,
                onClick = { onContrastModeChanged(mode) }
            )
        }
    }

    if (contrastUiState.contrastMode == ContrastMode.Manual) {
        HorizontalDivider()
        SelectionSection(title = "Manual GeoView contrast") {
            ContrastAppearance.entries.forEach { appearance ->
                SelectionRow(
                    title = appearance.displayName,
                    description = appearance.description,
                    selected = contrastUiState.manualAppearance == appearance,
                    onClick = { onManualAppearanceChanged(appearance) }
                )
            }
        }
    }
}

@Composable
private fun ReferenceLayerToggleRow(
    referenceLayersVisible: Boolean,
    onReferenceLayerVisibilityChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Reference layers",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (referenceLayersVisible) {
                    "Labels and boundary reference layers are visible."
                } else {
                    "Labels and boundary reference layers are hidden."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = referenceLayersVisible,
            onCheckedChange = onReferenceLayerVisibilityChanged
        )
    }
}

@Composable
private fun SelectionSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium
    )
    content()
}

@Composable
private fun <T> ToggleButtonRow(
    options: List<T>,
    selectedOption: T,
    optionLabel: (T) -> String,
    onOptionSelected: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val selected = selectedOption == option
            if (selected) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onOptionSelected(option) }
                ) {
                    Text(optionLabel(option))
                }
            } else {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onOptionSelected(option) }
                ) {
                    Text(optionLabel(option))
                }
            }
        }
    }
}

@Composable
private fun SelectionRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val ContrastMode.displayName: String
    get() = when (this) {
        ContrastMode.Automatic -> "Automatic"
        ContrastMode.Manual -> "Manual"
    }

private val GeoViewType.displayName: String
    get() = when (this) {
        GeoViewType.Map -> "MapView"
        GeoViewType.Scene -> "SceneView"
    }

private val ContrastAppearance.displayName: String
    get() = when (this) {
        ContrastAppearance.Light -> "Light"
        ContrastAppearance.Dark -> "Dark"
        ContrastAppearance.HighContrastLight -> "High contrast light"
        ContrastAppearance.HighContrastDark -> "High contrast dark"
    }

private val ContrastAppearance.description: String
    get() = when (this) {
        ContrastAppearance.Light -> "Regular light web map for regular light theme."
        ContrastAppearance.Dark -> "Regular dark web map for regular dark theme."
        ContrastAppearance.HighContrastLight -> "High-contrast light web map for enhanced light theme."
        ContrastAppearance.HighContrastDark -> "High-contrast dark web map for enhanced dark theme."
    }

private fun DeviceContrastSettings.toAppearance(): ContrastAppearance {
    return when {
        isHighContrastEnabled && isDarkTheme -> ContrastAppearance.HighContrastDark
        isHighContrastEnabled -> ContrastAppearance.HighContrastLight
        isDarkTheme -> ContrastAppearance.Dark
        else -> ContrastAppearance.Light
    }
}

@SampleDeviceLightDarkPreview
@Composable
fun MainScreenPreview() {
    SamplePreviewSurface {
        MainScreenScaffold(
            contrastUiState = ContrastUiState(
                contrastMode = ContrastMode.Manual,
                manualAppearance = ContrastAppearance.HighContrastDark,
                geoViewType = GeoViewType.Map
            ),
            mainPaneContent = {}
        )
    }
}
