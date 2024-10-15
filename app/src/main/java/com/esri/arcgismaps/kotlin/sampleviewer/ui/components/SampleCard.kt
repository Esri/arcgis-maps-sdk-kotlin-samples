package com.esri.arcgismaps.kotlin.sampleviewer.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.esri.arcgismaps.kotlin.sampleviewer.model.CodeFile
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample
import com.esri.arcgismaps.kotlin.sampleviewer.model.SampleCategory
import com.esri.arcgismaps.kotlin.sampleviewer.model.SampleMetadata
import com.esri.arcgismaps.kotlin.sampleviewer.model.startSample
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.sampleList.DropdownItemData
import com.esri.arcgismaps.kotlin.sampleviewer.ui.theme.SampleAppTheme

/**
 * Row to display Sample information and its contents
 */
@Composable
fun SampleRow(
    sample: Sample,
    dropdownSampleItems: List<DropdownItemData>
) {
    val context = LocalContext.current
    Column(Modifier.clickable { sample.startSample(context) }) {
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp),
    ) {
        Text(
            text = sample.name,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp),
            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(onClick = { expandedDescription = !expandedDescription }) {
            Icon(
                imageVector = if (expandedDescription) Icons.Filled.Info else Icons.Outlined.Info,
                contentDescription = "Sample Description",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column {
            IconButton(
                onClick = { expandedMenu = !expandedMenu },
            ) {
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
                onDismissRequest = { expandedMenu = false },
            ) {
                dropdownSampleItems.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = option.icon),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            expandedMenu = option.title.lowercase() == "favorite" || option.title.lowercase() == "unfavorite"
                            option.onClick()
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }

    ExpandedDescriptionAnimation(expandedDescription, sample)
}

@Composable
private fun ExpandedDescriptionAnimation(
    expandedDescription: Boolean,
    sample: Sample
) {
    AnimatedVisibility(
        visible = expandedDescription,
        enter = expandVertically(),
        exit = shrinkVertically(
            animationSpec = tween(
                durationMillis = 1000
            )
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, bottom = 8.dp, end = 32.dp)
        ) {
            Text(
                text = sample.metadata.description,
                textAlign = TextAlign.Start,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                color = Color.Gray,
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
            Sample(
                name = "Analyze hotspots",
                codeFiles = listOf(
                    CodeFile(
                        "", ""
                    )
                ),
                url = "",
                readMe = "",
                screenshotURL = "",
                metadata = SampleMetadata(
                    codePaths = listOf(""),
                    description = "",
                    formalName = "Analyze hotspots",
                    ignore = false,
                    imagePaths = listOf(""),
                    keywords = listOf(""),
                    language = "",
                    relevantApis = listOf(""),
                    sampleCategory = SampleCategory.ANALYSIS,
                    title = "Analyze hotspots"
                ),
                isFavorite = false,
                mainActivity = ""
            ),
            dropdownItemData
        )
    }
}