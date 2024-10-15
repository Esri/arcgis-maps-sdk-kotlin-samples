@file:OptIn(ExperimentalMaterial3Api::class)

package com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.about

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.esri.arcgismaps.kotlin.sampleviewer.BuildConfig
import com.esri.arcgismaps.kotlin.sampleviewer.R
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.SampleViewerTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

//TODO -- Scrolling not working on this screen for me


/**
 * Showcase information about the application.
 */
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            SampleViewerTopAppBar(
                title = "About",
                onBackPressed = { navController.popBackStack() }
            )
        },
        modifier = Modifier
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        AboutContent(innerPadding, context)
    }
}

@Composable
fun AboutContent(innerPadding: PaddingValues, context: Context) {
    Column(
        Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(Modifier.padding(20.dp)) {
            val size = (LocalConfiguration.current.screenWidthDp * 0.20).dp
            AboutIcon(context, size)
            TitleAndCopyrightSection(context)
            Spacer(modifier = Modifier.height(20.dp))
            VersionsSection(context)
            Spacer(modifier = Modifier.height(20.dp))
            PoweredBySection(context)
            Spacer(modifier = Modifier.height(20.dp))
            EsriCommunitySection(context)
            Spacer(modifier = Modifier.height(20.dp))
            GithubSection(context)
            Spacer(modifier = Modifier.height(20.dp))
            ApiDetailsSection(context)
        }
    }
}

@Composable
private fun AboutIcon(context: Context, size: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.size(size),
            painter = painterResource(R.drawable.arcgis_maps_sdks_64),
            contentDescription = context.getString(R.string.sdk_sample_icon_description)
        )
    }
}

@Composable
private fun TitleAndCopyrightSection(context: Context) {
    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = context.getString(R.string.about_title),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = context.getString(R.string.copyright_text),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun VersionsSection(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = context.getString(R.string.app_version),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = BuildConfig.VERSION_CODE.toString(),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = context.getString(R.string.SDK_version),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = BuildConfig.ARCGIS_VERSION,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

}

@Composable
private fun PoweredBySection(context: Context) {
    var isAcknowledgementsDialogVisible by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
            .padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = context.getString(R.string.powered_by),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        }
        Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/Esri/arcgis-runtime-toolkit-android")
                        )
                        context.startActivity(intent)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = context.getString(R.string.ArcGIS_Maps_SDK_Toolkit),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.keyboard_arrow_right),
                        contentDescription = context.getString(R.string.forward_button)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://developers.arcgis.com/kotlin/")
                        )
                        context.startActivity(intent)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = context.getString(R.string.ArcGIS_Maps_SDK_Kotlin),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.keyboard_arrow_right),
                        contentDescription = context.getString(R.string.forward_button)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAcknowledgementsDialogVisible = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "Acknowledgements",
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )
                Box(
                    modifier = Modifier
                        .size(48.dp) // Size of the clickable area
                        .padding(8.dp) // Padding inside the Box
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.keyboard_arrow_right),
                        contentDescription = context.getString(R.string.forward_button)
                    )
                }
            }
        }
        if (isAcknowledgementsDialogVisible) {
            AcknowledgementsDialog {
                isAcknowledgementsDialogVisible = false
            }
        }
    }
}

@Composable
private fun EsriCommunitySection(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://community.esri.com/t5/kotlin-maps-sdk-questions/bd-p/kotlin-maps-sdk-questions")
                )
                context.startActivity(intent)
            }
            .padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = context.getString(R.string.browse_and_discuss),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = context.getString(R.string.esri_community),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.keyboard_arrow_right),
                        contentDescription = context.getString(R.string.forward_button)
                    )
                }
            }
        }
    }
}

@Composable
fun AcknowledgementsDialog(onDismissRequest: () -> Unit) {
    BasicAlertDialog(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .clip(RoundedCornerShape(8.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val context = LocalContext.current
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "License Acknowledgements",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center

            )
            OutlinedButton(onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/showdownjs/showdown/blob/master/LICENSE")
                )
                context.startActivity(intent)
            }) {
                Text(text = "Showdown")
            }
            OutlinedButton(onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/highlightjs/highlight.js/blob/main/LICENSE")
                )
                context.startActivity(intent)
            }) {
                Text(text = "Highlight.js")
            }
        }
    }
}

@Composable
private fun GithubSection(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/Esri/arcgis-maps-sdk-kotlin-samples")
                )
                context.startActivity(intent)
            }
            .padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = context.getString(R.string.github_issue),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = context.getString(R.string.github_repo),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.keyboard_arrow_right),
                        contentDescription = context.getString(R.string.forward_button)
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiDetailsSection(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://developers.arcgis.com/kotlin/api-reference/")
                )
                context.startActivity(intent)
            }
            .padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = context.getString(R.string.API_details),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = context.getString(R.string.API_ref),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.keyboard_arrow_right),
                        contentDescription = context.getString(R.string.forward_button)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewAboutScreen() {
    SampleAppTheme {
        AboutScreen(navController = rememberNavController())
    }
}