/* Copyright 2024 Esri
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

package com.esri.arcgismaps.kotlin.sampleviewer.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample
import com.esri.arcgismaps.kotlin.sampleviewer.model.start
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.sampleList.DropdownItemData
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import kotlinx.coroutines.launch

/**
 * Row to display Sample information and its contents
 */
@Composable
fun SampleRow(
    sample: Sample,
    dropdownSampleItems: List<DropdownItemData>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(Modifier.clickable { scope.launch { sample.start(context) } }) {
        TitleAndIcons(sample, dropdownSampleItems)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TitleAndIcons(
    sample: Sample,
    dropdownSampleItems: List<DropdownItemData>
) {
    var expandedMenu by rememberSaveable { mutableStateOf(false) }
    var expandedDescription by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = sample.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            ExpandedDescriptionAnimation(expandedDescription, sample)
        }

        IconButton(onClick = { expandedDescription = !expandedDescription }) {
            Icon(
                imageVector = if (expandedDescription) Icons.Filled.Info else Icons.Outlined.Info,
                contentDescription = "Sample Info",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column {
            IconButton(onClick = { expandedMenu = !expandedMenu }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                modifier = Modifier
                    .height(IntrinsicSize.Max)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false }
            ) {
                dropdownSampleItems.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(
                                    space = 8.dp,
                                    alignment = Alignment.CenterHorizontally
                                )
                            ) {
                                Icon(
                                    painter = painterResource(id = option.icon),
                                    contentDescription = "${option.title} Icon",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            expandedMenu = option.title.lowercase().contains("favorite")
                            option.onClick()
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedDescriptionAnimation(
    expandedDescription: Boolean,
    sample: Sample
) {
    AnimatedVisibility(
        visible = expandedDescription,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = sample.metadata.description,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewSampleRow() {
    SampleAppTheme {
        val dropdownItemData = listOf<DropdownItemData>()
        SampleRow(
            sample = Sample.PREVIEW_INSTANCE,
            dropdownSampleItems = dropdownItemData
        )
    }
}
