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

package com.esri.arcgismaps.kotlin.sampleviewer.navigation

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.esri.arcgismaps.kotlin.sampleviewer.model.DefaultSampleInfoRepository
import com.esri.arcgismaps.kotlin.sampleviewer.model.SampleCategory
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.about.AboutScreen
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.codePager.SampleInfoScreen
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.home.HomeCategoryScreen
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.sampleList.SampleListScreen
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.search.SearchResults
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.search.SearchScreen

/**
 *  A composable function to host the navigation system.
 */
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.HOME_SCREEN,
    ) {

        composable(Routes.HOME_SCREEN) {
            HomeCategoryScreen(
                navigateToSearch = { navController.navigate(Routes.SEARCH_SCREEN) },
                navigateToAbout = { navController.navigate(Routes.ABOUT_SCREEN) },
                navigateToCategory = { navController.navigate(Routes.createCategoryRoute(it)) }
            )
        }

        composable(Routes.ABOUT_SCREEN) {
            AboutScreen(popBackStack = { navController.popBackStack() })
        }

        composable(
            route = Routes.CATEGORY_SAMPLE_LIST_ROUTE,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryNavEntry = backStackEntry.arguments?.getString("category")
            if (!categoryNavEntry.isNullOrEmpty())
                SampleListScreen(
                    categoryNavEntry = categoryNavEntry,
                    navigateToInfo = { optionPosition, sample ->
                        navController.navigate(
                            Routes.createSampleInfoRoute(optionPosition, sample.name)
                        )
                    },
                    popBackStack = { navController.popBackStack() }
                )
            else {
                InvalidArgsException("categoryNavEntry is null/empty")
                navController.navigateToHome()
            }
        }

        composable(
            route = Routes.SAMPLE_INFO_ROUTE,
            arguments = listOf(
                navArgument("optionPosition") { type = NavType.IntType },
                navArgument("sampleName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val optionPositionNavEntry = backStackEntry.arguments?.getInt("optionPosition")
            val sampleNameNavEntry = backStackEntry.arguments?.getString("sampleName")

            if (optionPositionNavEntry != null && !sampleNameNavEntry.isNullOrEmpty()) {
                val sampleNavEntry = DefaultSampleInfoRepository.getSampleByName(sampleNameNavEntry)
                SampleInfoScreen(
                    sample = sampleNavEntry,
                    optionPosition = optionPositionNavEntry,
                    popBackStack = { navController.popBackStack() }
                )
            } else if (optionPositionNavEntry == null) {
                InvalidArgsException("optionPositionNavEntry is null")
                navController.navigateToHome()
            } else {
                InvalidArgsException("sampleNameNavEntry is null/empty")
                navController.navigateToHome()
            }
        }

        composable(Routes.SEARCH_SCREEN) {
            SearchScreen(
                navigateToSearchResults = {
                    navController.navigate(Routes.createSearchResultsRoute(it))
                },
                popBackStack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.SEARCH_RESULTS_ROUTE,
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) { backStackEntry ->
            val queryNavEntry = backStackEntry.arguments?.getString("query")
            if (!queryNavEntry.isNullOrEmpty()) {
                SearchResults(
                    searchQuery = queryNavEntry,
                    navigateToInfo = { optionPosition, sample ->
                        navController.navigate(
                            Routes.createSampleInfoRoute(
                                optionPosition,
                                sample.name
                            )
                        )
                    },
                    popBackStack = { navController.popBackStack() }
                )
            } else {
                navController.navigateToHome()
                InvalidArgsException("queryNavEntry is null/empty")
            }
        }
    }
}

@Composable
fun InvalidArgsException(message: String) {
    val exceptionTag = "InvalidArgsException"
    Toast.makeText(LocalContext.current, "$exceptionTag: $message", Toast.LENGTH_SHORT).show()
    Log.e(exceptionTag, message)
}

/**
 * Navigates to the home screen, clearing the back stack and restoring the state.
 */
private fun NavHostController.navigateToHome() {
    navigate(Routes.HOME_SCREEN) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}

/**
 * Navigation Routes for the application.
 */
object Routes {
    private const val SAMPLE_LIST = "Sample List"
    private const val SAMPLE_INFO = "Sample Info"
    private const val SEARCH_RESULTS = "Search Results"
    const val HOME_SCREEN = "Sample Categories"
    const val ABOUT_SCREEN = "About"
    const val SEARCH_SCREEN = "Search"
    const val CATEGORY_SAMPLE_LIST_ROUTE = "$SAMPLE_LIST/{category}"
    const val SAMPLE_INFO_ROUTE = "$SAMPLE_INFO/{optionPosition}/{sampleName}"
    const val SEARCH_RESULTS_ROUTE = "$SEARCH_RESULTS/{query}"

    fun createCategoryRoute(category: SampleCategory): String {
        return "$SAMPLE_LIST/${category}"
    }

    fun createSearchResultsRoute(query: String): String {
        return "$SEARCH_RESULTS/$query"
    }

    fun createSampleInfoRoute(optionPosition: Int, sampleName: String): String {
        return "$SAMPLE_INFO/$optionPosition/$sampleName"
    }
}
