package com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.esri.arcgismaps.kotlin.sampleviewer.R
import com.esri.arcgismaps.kotlin.sampleviewer.model.DefaultSampleInfoRepository
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample
import com.esri.arcgismaps.kotlin.sampleviewer.model.startSample
import com.esri.arcgismaps.kotlin.sampleviewer.viewmodels.SampleSearchViewModel

/**
 * Allows functionality to search samples to get two types of categories: Samples and Relevant APIs attached to Sample Readmes
 */
@Composable
fun SearchScreen(navController: NavController) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val viewModel: SampleSearchViewModel = viewModel()
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    // Get the ordered search suggestions of samples or relevantAPIs
    val searchSuggestions: List<Pair<String, Boolean>> by viewModel.searchSuggestions.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SampleViewerSearchBar(
                searchQuery = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.suggestionSearch(searchQuery)
                },
                onClear = {
                    searchQuery = ""
                    focusManager.clearFocus()
                },
                onExit = {
                    keyboardController?.hide()
                    navController.popBackStack()
                },
                onSearch = {
                    if (searchQuery.trim().isNotEmpty()) {
                        keyboardController?.hide()
                        // Open results screen with list view.
                        navController.navigate("${R.string.searchResults_section}/query=${searchQuery}")
                    }

                }
            )
            SearchSuggestionsList(
                searchQuery = searchQuery,
                searchSuggestions = searchSuggestions,
                onSampleSelected = {
                    it.startSample(context)
                },
                onRelevantAPISelected = { apiName ->
                    // Show's search results
                    navController.navigate("${R.string.searchResults_section}/query=${apiName}")
                }
            )
        }


    }
}

@Composable
fun SearchSuggestionsList(
    searchQuery: String,
    searchSuggestions: List<Pair<String, Boolean>>,
    onSampleSelected: (Sample) -> Unit,
    onRelevantAPISelected: (String) -> Unit

) {
    val samplesFromRepository by rememberSaveable { mutableStateOf(DefaultSampleInfoRepository.getAllSamples()) }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        AnimatedVisibility(
            visible = searchQuery.isEmpty(),
            enter = fadeIn(), exit = fadeOut(),
        ) {
            EmptySearchQueryMessage()
        }

        AnimatedVisibility(
            visible = searchQuery.isNotEmpty(),
            enter = fadeIn(), exit = fadeOut()
        ) {
            // Show dynamic search results as search query changes
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .animateContentSize()
            ) {
                items(
                    items = searchSuggestions,
                    key = { suggestion -> suggestion }
                ) { suggestion ->
                    if (suggestion.second) { // Sample Suggestion
                        val sample = samplesFromRepository.find { it.name == suggestion.first }
                            ?: return@items
                        Row(
                            modifier = Modifier
                                .animateItem()
                                .clickable { onSampleSelected(sample) }
                        ) {
                            SampleListItem(sample.name)
                        }
                    } else { // RelevantAPI Suggestion
                        val relevantApi = suggestion.first
                        Row(
                            modifier = Modifier
                                .animateItem()
                                .clickable { onRelevantAPISelected(relevantApi) }
                        ) {
                            RelevantAPIListItem(relevantApi)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SampleViewerSearchBar(
    searchQuery: String,
    onValueChange: (String) -> Unit,
    onExit: () -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit
) {
    val windowInfo = LocalWindowInfo.current
    val focusRequester = remember { FocusRequester() }
    // Opens keyboard and focuses on search when the screen comes in focus
    LaunchedEffect(windowInfo) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { isWindowFocused ->
            if (isWindowFocused) {
                focusRequester.requestFocus()
            }
        }
    }

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 12.dp)
            .focusRequester(focusRequester),
        value = searchQuery,
        onValueChange = onValueChange,
        maxLines = 1,
        singleLine = true,
        shape = RoundedCornerShape(50.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
        ),
        placeholder = {
            Text(
                text = "Search...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            IconButton(onClick = { onExit() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        trailingIcon = {
            IconButton(onClick = { onClear() }) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search,
            keyboardType = KeyboardType.Text
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch() }
        )
    )
}

@Composable
private fun RelevantAPIListItem(relevantApi: String) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        },
        headlineContent = {
            Text(
                text = relevantApi,
                fontSize = MaterialTheme.typography.titleSmall.fontSize,
            )
        },
    )
}

@Composable
private fun SampleListItem(sampleName: String) {
    ListItem(
        leadingContent = {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        },
        headlineContent = {
            Text(
                text = sampleName,
                fontSize = MaterialTheme.typography.titleSmall.fontSize,
            )
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
}

@Composable
private fun EmptySearchQueryMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = BiasAlignment(0f, -0.5f)
    ) {
        Text(
            text = "Search for sample name, feature or an API",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
