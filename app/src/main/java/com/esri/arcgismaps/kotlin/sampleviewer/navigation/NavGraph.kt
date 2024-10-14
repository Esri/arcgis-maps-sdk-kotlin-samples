package com.esri.arcgismaps.kotlin.sampleviewer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.esri.arcgismaps.kotlin.sampleviewer.R
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.about.AboutScreen
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.codePager.CodePagerScreen
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.home.HomeCategoryScreen
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.sampleList.SampleListScreen
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.search.SearchResults
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.search.SearchScreen

/**
 *  A composable function to host the navigation system
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
            val categoryNavEntry = backStackEntry.arguments?.getString("category")
            if (!categoryNavEntry.isNullOrEmpty())
                SampleListScreen(categoryNavEntry, navController)
        }

        composable(
            route = "${R.string.codePager_section}/optionPosition={optionPosition}/sampleName={sampleName}",
            arguments = listOf(
                navArgument("optionPosition") { type = NavType.IntType },
                navArgument("sampleName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val optionPositionNavEntry = backStackEntry.arguments?.getInt("optionPosition")
            val sampleNameNavEntry = backStackEntry.arguments?.getString("sampleName")

            if (optionPositionNavEntry != null && !sampleNameNavEntry.isNullOrEmpty())
                CodePagerScreen(
                    onBackPressed = { navController.popBackStack() },
                    optionPosition = optionPositionNavEntry,
                    sampleName = sampleNameNavEntry
                )
        }

        composable(R.string.search_section.toString()) {
            SearchScreen(navController = navController)
        }

        composable(
            route = "${R.string.searchResults_section}/query={query}",
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) { backStackEntry ->
            val queryNavEntry = backStackEntry.arguments?.getString("query")
            if (!queryNavEntry.isNullOrEmpty())
                SearchResults(
                    searchQuery = queryNavEntry,
                    navController = navController
                )
        }
    }
}
