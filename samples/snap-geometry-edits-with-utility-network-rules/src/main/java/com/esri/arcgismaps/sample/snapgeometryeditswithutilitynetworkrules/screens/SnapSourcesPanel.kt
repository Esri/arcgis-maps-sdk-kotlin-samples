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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.sampleslib.theme.SampleTypography
import com.esri.arcgismaps.sample.snapgeometryeditswithutilitynetworkrules.components.SnapGeometryEditsWithUtilityNetworkRulesViewModel

@Composable
fun SnapSourcesPanel(
    snapSourcePropertyList: State<List<SnapGeometryEditsWithUtilityNetworkRulesViewModel.SnapSourceProperty>>,
    onSnapSourcePropertyChanged: (Boolean, Int) -> Unit = { _: Boolean, _: Int -> }
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .defaultMinSize(minHeight = 185.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row {
                Text(
                    text = "Snap sources and their SnapRuleBehavior",
                    fontWeight = FontWeight.ExtraBold
                )
            }
            if (snapSourcePropertyList.value.isEmpty()) {
                Text(text = "Tap a point feature to edit")
            }
            for (index in snapSourcePropertyList.value.indices) {
                val snapSourceProperty = snapSourcePropertyList.value[index]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = ssp.swatch.bitmap.asImageBitmap(),
                        contentDescription = "Symbol image"
                    )
                    Switch(
                        modifier = Modifier.padding(start = 8.dp),
                        checked = snapSourcePropertyList.value[index].snapSourceSettings.isEnabled,
                        onCheckedChange = { newValue ->
                            onSnapSourcePropertyChanged(newValue, index)
                        }
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        style = SampleTypography.bodyMedium,
                        text = "${ssp.name} (${ssp.snapSourceSettings.ruleBehavior})"
                    )
                }
            }
        }
    }
}
