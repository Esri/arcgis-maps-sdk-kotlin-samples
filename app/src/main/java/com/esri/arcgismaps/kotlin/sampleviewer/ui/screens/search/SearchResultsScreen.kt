package com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.search

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.SampleViewerTopAppBar
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.home.HomeCategoryScreen
import com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.sampleList.ListOfSamplesScreen
import com.esri.arcgismaps.kotlin.sampleviewer.viewmodels.SampleSearchViewModel
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

/**
 * Shows search results based on valid query searches.
 */
@Composable
fun SearchResults(searchQuery: String, navigateToInfo: (Int, Sample) -> Unit) {
    val searchViewModel: SampleSearchViewModel = viewModel()
    searchViewModel.rankedSearch(searchQuery)
    Scaffold(
        topBar = {
            SampleViewerTopAppBar(title = searchQuery)
        }) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            // List of samples results ranked with using the searchQuery
            val rankedSearchResults by searchViewModel.rankedSearchResults.collectAsState()
            ListOfSamplesScreen(
                samples = rankedSearchResults,
                navigateToInfo = navigateToInfo
            )
        }
    }
}
