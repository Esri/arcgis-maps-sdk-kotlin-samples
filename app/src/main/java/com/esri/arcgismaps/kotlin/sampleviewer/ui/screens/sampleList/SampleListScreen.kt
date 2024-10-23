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

package com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.sampleList

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.esri.arcgismaps.kotlin.sampleviewer.R
import com.esri.arcgismaps.kotlin.sampleviewer.model.DefaultSampleInfoRepository
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample
import com.esri.arcgismaps.kotlin.sampleviewer.model.SampleCategory
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.SampleRow
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.SampleViewerTopAppBar
import com.esri.arcgismaps.kotlin.sampleviewer.viewmodels.FavoritesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Shows the list of samples.
 */
@Composable
fun SampleListScreen(categoryNavEntry: String, navigateToInfo: (Int, Sample) -> Unit) {
    val viewModel: FavoritesViewModel = viewModel()
    val favoriteSamplesFlow = remember { viewModel.getFavorites() }
    val favoriteSamples by favoriteSamplesFlow.collectAsState(initial = emptyList())
    val category = SampleCategory.toEnum(categoryNavEntry)
    val samplesList = DefaultSampleInfoRepository.getSamplesInCategory(category)

    Scaffold(
        topBar = {
            SampleViewerTopAppBar(title = category.text)
        },
        modifier = Modifier
            .fillMaxSize(),
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (samplesList.isEmpty() && category != SampleCategory.FAVORITES) {
                EmptySampleListScreen(stringResource(R.string.upcoming_samples_text))
            } else if (category == SampleCategory.FAVORITES) {
                FavoriteItemsListScreen(favoriteSamples, navigateToInfo)
            } else {
                ListOfSamplesScreen(samplesList, navigateToInfo)
            }
        }
    }
}

@Composable
fun ListOfSamplesScreen(
    samples: List<Sample>,
    navigateToInfo: (Int, Sample) -> Unit,
) {
    val favoritesViewModel: FavoritesViewModel = viewModel()
    val favoriteSamples by favoritesViewModel.getFavorites().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        LazyColumn(modifier = Modifier.animateContentSize()) {
            itemsIndexed(samples) { index, sample ->
                val isFavorite = favoriteSamples.contains(sample)
                val readMePosition = 0
                val codeFilePosition = 1

                val dropdownSampleItems = listOf(
                    DropdownItemData(
                        title = "README",
                        icon = R.drawable.ic_readme,
                        onClick = { navigateToInfo(readMePosition, sample) }
                    ),
                    DropdownItemData(
                        title = "Code",
                        icon = R.drawable.ic_kotlin,
                        onClick = { navigateToInfo(codeFilePosition, sample) }
                    ),
                    DropdownItemData(
                        title = "Website",
                        icon = R.drawable.ic_link,
                        onClick = {
                            val url = "https://developers.arcgis.com/kotlin/sample-code/" +
                                    sample.metadata.title
                                        .replace(" ", "-")
                                        .lowercase(Locale.getDefault())
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    ),
                    DropdownItemData(
                        title = if (isFavorite) "Unfavorite" else "Favorite",
                        icon = if (isFavorite) R.drawable.ic_favorite_selected else R.drawable.ic_favorite_unselected,
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                if (isFavorite) favoritesViewModel.removeFavorite(sample)
                                else favoritesViewModel.addFavorite(sample)
                            }
                        }
                    )
                )

                SampleRow(
                    sample = sample,
                    dropdownSampleItems = dropdownSampleItems
                )

                // Add divider if not the last item
                if (index < samples.size - 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EmptySampleListScreen(emptyMessage: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FavoriteItemsListScreen(
    favoriteSamples: List<Sample>,
    navigateToInfo: (Int, Sample) -> Unit
) {
    if (favoriteSamples.isNotEmpty()) {
        ListOfSamplesScreen(
            samples = favoriteSamples,
            navigateToInfo = navigateToInfo
        )
    } else {
        EmptySampleListScreen(stringResource(R.string.no_favorites_text))
    }
}

data class DropdownItemData(
    val title: String,
    val icon: Int,
    val onClick: () -> Unit
)
