package com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.SampleViewerTopAppBar
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.sampleList.ListOfSamplesScreen
import com.esri.arcgismaps.kotlin.sampleviewer.viewmodels.SampleSearchViewModel

/**
 * Shows search results based on valid query searches.
 */
@Composable
fun SearchResults(searchQuery: String, navController: NavController) {
    val searchViewModel: SampleSearchViewModel = viewModel()
    searchViewModel.rankedSearch(searchQuery)
    Scaffold(
        topBar = {
            SampleViewerTopAppBar(
                title = searchQuery,
                onBackPressed = { navController.popBackStack() }
            )
        }) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            // List of samples results ranked with using the searchQuery
            val rankedSearchResults by searchViewModel.rankedSearchResults.collectAsState()
            ListOfSamplesScreen(
                samples = rankedSearchResults,
                navController = navController
            )
        }
    }
}
