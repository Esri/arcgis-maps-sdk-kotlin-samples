/*
 * COPYRIGHT 1995-2026 ESRI
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

package com.esri.arcgismaps.sample.showlineofsightanalysisinmap.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.showlineofsightanalysisinmap.components.LineOfSightUiState

@Composable
fun LineOfSightSupportingContent(
    lineOfSightUiState: LineOfSightUiState,
    onVisibilityFilterChanged: (Boolean) -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VisibilityFilterCheckbox(lineOfSightUiState.visibilityFilter, onVisibilityFilterChanged)
    }
}

@Composable
private fun VisibilityFilterCheckbox(
    isChecked: Boolean,
    onVisibilityFilterChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = isChecked,
                role = Role.Checkbox,
                onValueChange = onVisibilityFilterChanged,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = null,
        )
        Text(text = "Only observers with line of sight")
    }
}
