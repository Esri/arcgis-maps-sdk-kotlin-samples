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
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.esri.arcgismaps.kotlin.sampleviewer.R
import com.esri.arcgismaps.kotlin.sampleviewer.model.DefaultSampleInfoRepository
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.CodeView
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.MarkdownTextView
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.SampleViewerTopAppBar
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.about.AboutScreen
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.ReadmeView

/**
 * Shows both README and Code for each sample.
 */
@Composable
fun SampleInfoScreen(
    onBackPressed: () -> Unit,
    sampleName: String,
    optionPosition: Int
) {
    // TODO: Can we pass in this sample as an argument?
    //  The preview does not render due this call
    val sampleData = DefaultSampleInfoRepository.getSampleByName(sampleName)
    val codePagerTitles = mutableListOf<String>()

    sampleData.let { sample ->
        codePagerTitles.add("README.md")
        for (codeFile in sample.codeFiles) {
            codePagerTitles.add(codeFile.name)
        }
        Scaffold(
            topBar = {
                SampleViewerTopAppBar(
                    title = sample.name,
                    onBackPressed = onBackPressed
                )
            },
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

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
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
fun PreviewSampleInfoScreen() {
    SampleAppTheme {
        // TODO
    }
}

fun getIconId(selectedFile: String): Int {
    return when (selectedFile.lowercase().contains(".kt")) {
        true -> R.drawable.ic_kotlin
        else -> R.drawable.ic_readme
    }
}