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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.esri.arcgismaps.kotlin.sampleviewer.R
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
        startDestination = R.string.homeCategory_section.toString(),
    ) {

        composable(R.string.homeCategory_section.toString()) {
            HomeCategoryScreen(navController)
        }

        composable(R.string.about_section.toString()) {
            AboutScreen(navController)
        }

        composable(
            route = "${R.string.sampleList_section}/category={category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryNavEntry = backStackEntry.arguments?.getString("category") ?: ""
            SampleListScreen(categoryNavEntry, navController)
        }

        composable(
            route = "${R.string.codePager_section}/optionPosition={optionPosition}/sampleName={sampleName}",
            arguments = listOf(
                navArgument("optionPosition") { type = NavType.IntType },
                navArgument("sampleName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val optionPositionNavEntry = backStackEntry.arguments?.getInt("optionPosition") ?: 0
            val sampleNameNavEntry = backStackEntry.arguments?.getString("sampleName") ?: ""
            SampleInfoScreen(
                onBackPressed = { navController.popBackStack() },
                optionPosition = optionPositionNavEntry,
                sampleName = sampleNameNavEntry
            )
        }

        composable(R.string.search_section.toString()) {
            EnterAnimation {
                SearchScreen(
                    navController = navController)
            }
        }

        composable(
            route = "${R.string.searchResults_section}/query={query}",
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) { backStackEntry ->
                val queryNavEntry = backStackEntry.arguments?.getString("query") ?: ""
            SearchResults(
                searchQuery = queryNavEntry,
                navController = navController)
        }
    }
}

@Composable
fun EnterAnimation(content: @Composable () -> Unit) {
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = true
    AnimatedVisibility(
        visibleState = transitionState,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight } // slide in from the bottom
        ) + fadeIn(initialAlpha = 0.3f), // fade-in effect
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight } // slide out to the bottom
        ) + fadeOut(), // fade-out effect
        modifier = Modifier
    ) {
        content()
    }
}
