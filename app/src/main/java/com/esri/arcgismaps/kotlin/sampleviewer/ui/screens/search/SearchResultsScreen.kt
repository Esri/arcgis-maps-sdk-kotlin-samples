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
fun SearchResults(
    searchQuery: String,
    navigateToInfo: (Int, Sample) -> Unit,
    popBackStack: () -> Unit
) {
    val searchViewModel: SampleSearchViewModel = viewModel()
    searchViewModel.rankedSearch(searchQuery)
    Scaffold(
        topBar = { SampleViewerTopAppBar(title = searchQuery, popBackStack) }) { innerPadding ->
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
