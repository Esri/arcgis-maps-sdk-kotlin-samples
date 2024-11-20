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

package com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.esri.arcgismaps.kotlin.sampleviewer.R
import com.esri.arcgismaps.kotlin.sampleviewer.model.Category
import com.esri.arcgismaps.kotlin.sampleviewer.model.SampleCategory
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.CategoryCard
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.stylegraphicswithsymbols.BuildConfig

/**
 * The main SampleViewer app screen which showcases the list all sample categories,
 * saved favorites, and an app wide searching interface.
 */
@Composable
fun HomeCategoryScreen(
    onNavigateToAbout: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCategory: (SampleCategory) -> Unit,
) {
    val config = LocalConfiguration.current
    val layoutSpacing by remember { mutableStateOf(0.03f * config.screenWidthDp.dp) }
    val isVisible = rememberSaveable { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Hide FAB
                if (available.y < -1) {
                    isVisible.value = false
                }
                // Show FAB
                if (available.y > 1) {
                    isVisible.value = true
                }
                return Offset.Zero
            }
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { HomeCategoryTopAppBar(onNavigateToAbout) },
        floatingActionButton = { SearchFloatingActionButton(isVisible, onNavigateToSearch) },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
        ) {
            LazyVerticalStaggeredGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(layoutSpacing),
                columns = StaggeredGridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(layoutSpacing),
                verticalItemSpacing = layoutSpacing
            ) {
                items(Category.SAMPLE_CATEGORIES.size) { index ->
                    val category = Category.SAMPLE_CATEGORIES[index]
                    CategoryCard(category) { onNavigateToCategory(category.title) }
                }
            }
            CheckAccessToken()
        }
    }
}

/**
 * Display a dialog if the default access token placeholder is still set.
 * This is required for samples to access Esri location services, including basemaps, routing,
 * and geocoding, requires authentication using either an API Key or an ArcGIS identity.
 *
 * @see <a href="https://github.com/Esri/arcgis-maps-sdk-kotlin-samples?tab=readme-ov-file#accessing-esri-location-services">Accessing Esri location services</a>
 *
 */
@Composable
fun CheckAccessToken() {
    var isDialogDisplayed by remember {
        mutableStateOf(BuildConfig.ACCESS_TOKEN == "DEFAULT_ACCESS_TOKEN")
    }
    if (isDialogDisplayed) {
        MessageDialog(
            title = "Access token",
            description = "Add an ACCESS_TOKEN to the project's local.properties",
            onDismissRequest = { isDialogDisplayed = false }
        )
    }
}

@Composable
private fun SearchFloatingActionButton(
    isVisible: MutableState<Boolean>,
    navigateToSearch: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible.value,
        enter = slideInVertically(initialOffsetY = { it * 2 }),
        exit = slideOutVertically(targetOffsetY = { it * 2 }),
    ) {
        FloatingActionButton(
            onClick = navigateToSearch,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.search)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeCategoryTopAppBar(navigateToAboutScreen: () -> Unit) {
    TopAppBar(
        title = {
            Text(text = stringResource(R.string.home_screen_title))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        actions = {
            IconButton(onClick = navigateToAboutScreen) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.about_section)
                )
            }
        }
    )
}

@Composable
@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
fun PreviewHomeCategoryScreen() {
    SampleAppTheme {
        HomeCategoryScreen(
            onNavigateToCategory = {},
            onNavigateToSearch = {},
            onNavigateToAbout =  {}
        )
    }
}
