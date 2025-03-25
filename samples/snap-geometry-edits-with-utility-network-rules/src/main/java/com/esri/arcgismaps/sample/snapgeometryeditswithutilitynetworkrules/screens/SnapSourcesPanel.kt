/*
 * COPYRIGHT 1995-2025 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */
package com.esri.arcgismaps.sample.snapgeometryeditswithutilitynetworkrules.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.snapgeometryeditswithutilitynetworkrules.components.SnapGeometryEditsWithUtilityNetworkRulesViewModel

@Composable
fun SnapSourcesPanel(
    snapSourcePropertyList: List<SnapGeometryEditsWithUtilityNetworkRulesViewModel.SnapSourceProperty>,
    onSnapSourcePropertyChanged: (Boolean, Int) -> Unit,
    isGeometryEditorStarted: Boolean,
    canGeometryEditorUndo: Boolean,
    onDiscardGeometryChanges: () -> Unit,
    onSaveGeometryChanges: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                enabled = isGeometryEditorStarted,
                onClick = onDiscardGeometryChanges
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Discard")
            }
            Text(
                text = "Snap source settings",
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(
                enabled = (isGeometryEditorStarted && canGeometryEditorUndo),
                onClick = onSaveGeometryChanges
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
            }
        }

        for (index in snapSourcePropertyList.indices) {
            val snapSourceProperty = snapSourcePropertyList[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    bitmap = snapSourceProperty.swatch.bitmap.asImageBitmap(),
                    contentDescription = "Symbol image"
                )
                Switch(
                    checked = snapSourcePropertyList[index].snapSourceSettings.isEnabled,
                    onCheckedChange = { newValue ->
                        onSnapSourcePropertyChanged(newValue, index)
                    }
                )
                Text(
                    style = MaterialTheme.typography.bodySmall,
                    text = "${snapSourceProperty.name} (${snapSourceProperty.snapSourceSettings.ruleBehavior})",
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun SnapSourcesPanelPreview() {
    SampleAppTheme {
        Surface {
            SnapSourcesPanel(
                snapSourcePropertyList = listOf(),
                onSnapSourcePropertyChanged = { _, _ -> },
                isGeometryEditorStarted = true,
                canGeometryEditorUndo = true,
                onDiscardGeometryChanges = {},
                onSaveGeometryChanges = {}
            )
        }
    }
}
