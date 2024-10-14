package com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.home

import android.app.Application
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.navigation.NavController
import com.esri.arcgismaps.kotlin.sampleviewer.R
import com.esri.arcgismaps.kotlin.sampleviewer.model.Category
import com.esri.arcgismaps.kotlin.sampleviewer.model.SampleCategory
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.CardItem

/**
 * Showcase list of categories for all samples
 */
@Composable
fun HomeCategoryScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val spacing = 0.03f * screenWidth
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
        topBar = { HomeCategoryTopAppBar(application, navController) },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            SearchFloatingActionButton(
                isVisible,
                application,
                navController
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
        ) {
            val categoryList = getGridItems()

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalItemSpacing = spacing,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing)
            ) {
                items(categoryList.size) { index ->
                    val category = categoryList[index]
                    CardItem(category) {
                        navController.navigate("${R.string.sampleList_section}/category=${category.title}")
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFloatingActionButton(
    isVisible: MutableState<Boolean>,
    application: Application,
    navController: NavController
) {
    AnimatedVisibility(
        visible = isVisible.value,
        enter = slideInVertically(initialOffsetY = { it * 2 }),
        exit = slideOutVertically(targetOffsetY = { it * 2 }),
    ) {
        FloatingActionButton(
            onClick = { navController.navigate(R.string.search_section.toString()) },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = application.getString(R.string.search_FAB_text)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeCategoryTopAppBar(
    application: Application,
    navController: NavController
) {
    TopAppBar(
        title = {
            Text(text = application.getString(R.string.homeCategory_section))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        actions = {
            IconButton(onClick = {
                navController.navigate(R.string.about_section.toString()) {
                    // TODO: Maybe this should be handled in the NavGraph?
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = application.getString(R.string.about_section)
                )
            }
        }
    )
}

fun getGridItems(): List<Category> {
    return listOf(
        Category(
            SampleCategory.ANALYSIS,
            R.drawable.ic_analysis,
            R.drawable.analysis_background,
        ),
        Category(
            SampleCategory.AUGMENTED_REALITY,
            R.drawable.ic_augmented_reality,
            R.drawable.augmented_reality_background,
        ),
        Category(
            SampleCategory.CLOUD_AND_PORTAL,
            R.drawable.ic_cloud,
            R.drawable.cloud_background,
        ),
        Category(
            SampleCategory.EDIT_AND_MANAGE_DATA,
            R.drawable.ic_manage_data,
            R.drawable.manage_data_background,
        ),
        Category(
            SampleCategory.LAYERS,
            R.drawable.ic_layers,
            R.drawable.layers_background,
        ),
        Category(
            SampleCategory.MAPS,
            R.drawable.ic_map,
            R.drawable.maps_and_scenes_background,
        ),
        Category(
            SampleCategory.ROUTING_AND_LOGISTICS,
            R.drawable.ic_routing_and_logistics,
            R.drawable.routing_and_logistics_background,
        ),
        Category(
            SampleCategory.SCENES,
            R.drawable.ic_scenes,
            R.drawable.scenes_background,
        ),
        Category(
            SampleCategory.SEARCH_AND_QUERY,
            R.drawable.ic_search_and_query,
            R.drawable.search_and_query_background,
        ),
        Category(
            SampleCategory.UTILITY_NETWORKS,
            R.drawable.ic_utility,
            R.drawable.utility_background,
        ),
        Category(
            SampleCategory.VISUALIZATION,
            R.drawable.ic_visualization,
            R.drawable.visualization_background,
        ),
        Category(
            SampleCategory.FAVORITES,
            R.drawable.ic_favorite_selected,
            R.drawable.maps_and_scenes_background,
        ),
    )
}