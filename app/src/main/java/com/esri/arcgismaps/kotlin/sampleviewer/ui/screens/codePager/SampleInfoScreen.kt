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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.esri.arcgismaps.kotlin.sampleviewer.R
import com.esri.arcgismaps.kotlin.sampleviewer.model.DefaultSampleInfoRepository
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.CodeView
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.ReadmeView

/**
 * Shows both README and Code for each sample.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleInfoScreen(
    onBackPressed: () -> Unit,
    sampleName: String,
    optionPosition: Int
) {
    val sampleData = DefaultSampleInfoRepository.getSampleByName(sampleName)
    val codePagerTitles = mutableListOf<String>()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    sampleData.let { sample ->
        codePagerTitles.add("README.md")
        for (codeFile in sample.codeFiles) {
            codePagerTitles.add(codeFile.name)
        }
        Scaffold(
            topBar = {
                CodePagerTopAppBar(
                    scrollBehavior = scrollBehavior,
                    onBackPressed = onBackPressed,
                    title = sample.name
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
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
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CodePagerTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onBackPressed: () -> Unit,
    title: String
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onPrimary
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = LocalContext.current.getString(R.string.backButton),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
    )
}

@Composable
fun CodePagerBar(selectedFileIndex: Int, fileList: List<String>, onFileClicked: (Int) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier = Modifier
        .clickable { expanded = true }
        .fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CodePageRow(title = fileList[selectedFileIndex], iconId = getIconId(fileList[selectedFileIndex]))
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Icon(
            painter = painterResource(
                id = iconId
            ),
            contentDescription = null,
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
                val screenshotURL by remember { mutableStateOf(sample.screenshotURL) }
                AsyncImage(
                    model = screenshotURL,
                    contentDescription = null,
                    modifier = Modifier
                        .height(200.dp)
                        .width(350.dp)
                        .padding(8.dp)
                )
                val markdownText by remember { mutableStateOf(sample.readMe) }
                ReadmeView(markdownText = markdownText)
            }
        } else {
            CodeView(
                code = sample.codeFiles
                    .find {
                        it.name == codePagerTitles[selectedFileIndex]
                    }.let {
                        it?.code ?: ""
                    }
            )
        }
    }
}

fun getIconId(selectedFile: String): Int {
    return when (selectedFile.lowercase().contains(".kt")) {
        true -> R.drawable.ic_kotlin
        else -> R.drawable.ic_readme
    }
}
