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

package com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.codePager

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.esri.arcgismaps.kotlin.sampleviewer.R
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.CodeView
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.ReadmeView
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.SampleViewerTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

/**
 * Shows both README and Code for each sample.
 */
@Composable
fun SampleInfoScreen(
    sample: Sample,
    optionPosition: Int,
    popBackStack: () -> Unit
) {
    val codePagerTitles = mutableListOf<String>()
    codePagerTitles.add("README.md")
    sample.codeFiles.forEach {
        codePagerTitles.add(it.name)
    }
    Scaffold(
        topBar = { SampleViewerTopAppBar(title = sample.name, popBackStack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxSize(),
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape = MaterialTheme.shapes.extraLarge)
                .padding(innerPadding)
        ) {
            CodePageView(codePagerTitles, sample, optionPosition)
        }
    }
}

@Composable
fun CodePagerBar(selectedFileIndex: Int, fileList: List<String>, onFileClicked: (Int) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier = Modifier
        .clickable { expanded = true }
        .fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CodePageRow(
                title = fileList[selectedFileIndex],
                iconId = getIconId(fileList[selectedFileIndex])
            )
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = "Dropdown icon",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        DropdownMenu(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            expanded = expanded,
            onDismissRequest = { expanded = false }) {
            fileList.forEachIndexed { index, file ->
                DropdownMenuItem(modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    text = {
                        CodePageRow(title = file, iconId = getIconId(file))
                    }, onClick = {
                        onFileClicked(index)
                        expanded = false
                    })
            }
        }
    }
}

@Composable
fun CodePageRow(title: String, iconId: Int) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = "File icon",
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CodePageView(
    codePagerTitles: List<String>,
    sample: Sample,
    optionPosition: Int
) {
    var selectedFileIndex by remember { mutableIntStateOf(optionPosition) }
    Column {
        CodePagerBar(selectedFileIndex, codePagerTitles, onFileClicked = {
            selectedFileIndex = it
        })
        if (codePagerTitles[selectedFileIndex].contains(".md")) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val markdownText by remember { mutableStateOf(sample.readMe) }
                val screenshotURL by remember { mutableStateOf(sample.screenshotURL) }
                AsyncImage(
                    modifier = Modifier
                        .height(200.dp)
                        .width(350.dp)
                        .padding(8.dp),
                    model = screenshotURL,
                    contentDescription = "Sample screenshot"
                )
                ReadmeView(markdownText = markdownText)
            }
        } else {
            CodeView(
                code = sample.codeFiles
                    .find { it.name == codePagerTitles[selectedFileIndex] }?.code ?: ""
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
fun PreviewSampleInfoScreen() {
    SampleAppTheme {
        SampleInfoScreen(
            sample = Sample.PREVIEW_INSTANCE,
            optionPosition = 0,
            popBackStack = {}
        )
    }
}

fun getIconId(selectedFile: String): Int {
    return when (selectedFile.lowercase().contains(".kt")) {
        true -> R.drawable.ic_kotlin
        else -> R.drawable.ic_readme
    }
}
