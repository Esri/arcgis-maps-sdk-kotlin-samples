package com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.SampleViewerTopAppBar
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.sampleList.ListOfSamplesScreen
import com.esri.arcgismaps.kotlin.sampleviewer.viewmodels.FavoritesViewModel
import com.esri.arcgismaps.kotlin.sampleviewer.viewmodels.SampleSearchViewModel

/**
 * Shows search results based on valid query searches.
 */
@Composable
fun SearchResults(searchQuery: String, navController: NavController) {

    val context = LocalContext.current
    val favoriteViewModel: FavoritesViewModel = viewModel()
    val favoriteSamplesFlow = remember { favoriteViewModel.getFavorites() }
    val favoriteSamples by favoriteSamplesFlow.collectAsState(initial = emptyList())
    val searchViewModel: SampleSearchViewModel = viewModel<SampleSearchViewModel>()
    searchViewModel.rankedSearch(searchQuery)
    val rankedSearchResults by searchViewModel.rankedSearchResults.collectAsState()

    Scaffold(
        topBar = {
            SampleViewerTopAppBar(
                navController = navController,
                title = searchQuery,
                context = context
            )
        }) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            ListOfSamplesScreen(
                samples = rankedSearchResults,
                viewModel = favoriteViewModel,
                favoriteSamples = favoriteSamples,
                navController = navController
            )
        }
    }
}
