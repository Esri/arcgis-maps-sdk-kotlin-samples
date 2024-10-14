package com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.home

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.esri.arcgismaps.kotlin.sampleviewer.R
import com.esri.arcgismaps.kotlin.sampleviewer.model.Category
import com.esri.arcgismaps.kotlin.sampleviewer.model.SampleCategory
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.CategoryCard
import kotlinx.coroutines.delay

/**
 * Shows list of categories for all samples.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCategoryScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val spacing = 0.03f * screenWidth
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
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
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    navBackStackEntry.value?.destination?.route
    Scaffold(
        topBar = {
            HomeCategoryTopAppBar(application, scrollBehavior, navController)
        },
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
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
            var visibleItems by remember { mutableIntStateOf(0) }
            val items = getGridItems()
            // Simulate the animation delay
            LaunchedEffect(Unit) {
                items.forEachIndexed { index, _ ->
                    delay(100) // Delay between each item
                    visibleItems = index + 1
                }
            }
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalItemSpacing = spacing,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing)
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    AnimatedVisibility(
                        visible = index < visibleItems,
                        enter = slideInHorizontally(
                            initialOffsetX = { it }, // Slide in from right
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = FastOutSlowInEasing
                            )
                        ) + fadeIn(
                            initialAlpha = 0.5f,
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = FastOutSlowInEasing
                            )
                        ),
                        exit = fadeOut() + slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(durationMillis = 400)
                        )
                    ) {
                        CategoryCard(item) {
                            navController.navigate("${R.string.sampleList_section}/category=${item.title}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeCategoryTopAppBar(
    application: Application,
    scrollBehavior: TopAppBarScrollBehavior,
    navController: NavController
) {
    TopAppBar(
        title = {
            Text(
                text = application.getString(R.string.homeCategory_section),
                color = MaterialTheme.colorScheme.onPrimary
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
        scrollBehavior = scrollBehavior,
        actions = {
            IconButton(onClick = {
                navController.navigate(R.string.about_section.toString()) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = application.getString(R.string.about_section),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    )
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
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = application.getString(R.string.search_FAB_text)
            )
        }
    }
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
            SampleCategory.LAYERS,
            R.drawable.ic_layers,
            R.drawable.layers_background,
        ),
        Category(
            SampleCategory.EDIT_AND_MANAGE_DATA,
            R.drawable.ic_manage_data,
            R.drawable.manage_data_background,
        ),
        Category(
            SampleCategory.MAPS,
            R.drawable.ic_map,
            R.drawable.maps_and_scenes_background,
        ),
        Category(
            SampleCategory.SCENES,
            R.drawable.ic_scenes,
            R.drawable.scenes_background,
        ),
        Category(
            SampleCategory.ROUTING_AND_LOGISTICS,
            R.drawable.ic_routing_and_logistics,
            R.drawable.routing_and_logistics_background,
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
