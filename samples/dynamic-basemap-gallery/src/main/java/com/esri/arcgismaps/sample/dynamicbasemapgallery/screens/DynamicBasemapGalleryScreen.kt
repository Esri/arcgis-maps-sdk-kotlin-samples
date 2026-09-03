/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.dynamicbasemapgallery.screens

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.BasemapStyleInfo
import com.arcgismaps.toolkit.basemapgallery.BasemapGalleryItem
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.dynamicbasemapgallery.R
import com.esri.arcgismaps.sample.dynamicbasemapgallery.components.DynamicBasemapGalleryViewModel
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun DynamicBasemapGalleryScreen(
    mapViewModel: DynamicBasemapGalleryViewModel = viewModel()
) {
    val mapViewModel: DynamicBasemapGalleryViewModel = viewModel()

    // Controls whether the basemap gallery popup is shown
    var showBasemapGallery by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SampleTopAppBar(title = stringResource(R.string.dynamic_basemap_gallery_app_name))
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MapView(
                modifier = Modifier.fillMaxSize(),
                arcGISMap = mapViewModel.arcGISMap
            )

            FloatingActionButton(
                onClick = { showBasemapGallery = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Map,
                    contentDescription = "Show basemap gallery"
                )
            }
        }

        // Display menu
        if (showBasemapGallery) {
            // Default the pending selection to the actual gallery item for the basemap style
            // currently applied to the map, so it can be highlighted when the gallery opens
            var pendingItem by remember {
                mutableStateOf(
                    mapViewModel.basemapGalleryItems.firstOrNull { item ->
                        (item.tag as? BasemapStyleInfo)?.style == mapViewModel.selectedBasemapStyleInfo?.style
                    }
                )
            }
            var pendingLanguage by remember { mutableStateOf(mapViewModel.selectedLanguage) }
            var pendingWorldview by remember { mutableStateOf(mapViewModel.selectedWorldview) }

            Dialog(onDismissRequest = { showBasemapGallery = false }) {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .height(520.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Basemap Gallery",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Choose a basemap style to change the map's basemap.",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(96.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mapViewModel.basemapGalleryItems) { item ->
                            BasemapGalleryItemCard(
                                item = item,
                                selected = item === pendingItem,
                                onClick = {
                                    pendingItem = item
                                    pendingLanguage = null
                                    pendingWorldview = null
                                }
                            )
                        }
                    }

                    // Lists the languages supported by the pending basemap style selection
                    val languageOptions =
                        (pendingItem?.tag as? BasemapStyleInfo)?.languages.orEmpty()
                    DropDownMenuBox(
                        modifier = Modifier.fillMaxWidth(),
                        textFieldValue = pendingLanguage?.displayName ?: "Default",
                        textFieldLabel = "Language",
                        dropDownItemList = languageOptions.map { it.displayName },
                        onIndexSelected = { index ->
                            pendingLanguage = languageOptions[index]
                        }
                    )

                    // Lists the worldviews supported by the pending basemap style selection
                    val worldviewOptions =
                        (pendingItem?.tag as? BasemapStyleInfo)?.worldviews.orEmpty()
                    DropDownMenuBox(
                        modifier = Modifier.fillMaxWidth(),
                        textFieldValue = pendingWorldview?.displayName ?: "Default",
                        textFieldLabel = "Worldview",
                        dropDownItemList = worldviewOptions.map { it.displayName },
                        onIndexSelected = { index ->
                            pendingWorldview = worldviewOptions[index]
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        OutlinedButton(onClick = { showBasemapGallery = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                mapViewModel.onDoneClicked(
                                    item = pendingItem,
                                    languageInfo = pendingLanguage,
                                    worldview = pendingWorldview
                                )
                                showBasemapGallery = false
                            }
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }

        // Display a dialog if the sample encounters an error
        mapViewModel.messageDialogVM.apply {
            if (dialogStatus) {
                MessageDialog(
                    title = messageTitle,
                    description = messageDescription,
                    onDismissRequest = ::dismissDialog
                )
            }
        }
    }
}


// Base Map Gallery card for each Style
@Composable
private fun BasemapGalleryItemCard(
    item: BasemapGalleryItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val basemapStyleInfo = item.tag as? BasemapStyleInfo
    val thumbnail by produceState<BitmapDrawable?>(initialValue = null, key1 = basemapStyleInfo) {
        value = basemapStyleInfo?.thumbnail?.let { thumbnailImage ->
            thumbnailImage.load()
            thumbnailImage.image
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            thumbnail?.let { drawable ->
                Image(
                    bitmap = drawable.bitmap.asImageBitmap(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )
            }
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
