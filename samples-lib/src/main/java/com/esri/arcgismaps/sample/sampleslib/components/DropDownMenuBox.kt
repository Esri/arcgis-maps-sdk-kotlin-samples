/* Copyright 2025 Esri
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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.esri.arcgismaps.sample.sampleslib.components

import android.content.res.Configuration
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

/**
 * Composable component to simplify the usage of an [ExposedDropdownMenuBox].
 */
@Composable
fun DropDownMenuBox(
    modifier: Modifier = Modifier,
    textFieldValue: String,
    textFieldLabel: String,
    dropDownItemList: List<String>,
    onIndexSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            label = { Text(textFieldLabel) },
            value = textFieldValue,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            dropDownItemList.forEachIndexed { index, scalebarStyle ->
                DropdownMenuItem(
                    text = { Text(scalebarStyle) },
                    onClick = {
                        onIndexSelected(index)
                        expanded = false
                    })
                // Show a divider between dropdown menu options
                if (index < dropDownItemList.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDropDownMenuBox() {
    val dropDownItems = listOf("Option #1", "Option #2", "Option #3")
    SampleAppTheme {
        DropDownMenuBox(
            textFieldValue = dropDownItems[0],
            textFieldLabel = "Choose one of the dropdown options",
            dropDownItemList = dropDownItems,
            onIndexSelected = { _ -> /*HandleDropDownSelection*/ }
        )
    }
}
