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

package com.esri.arcgismaps.sample.featureforms.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arcgismaps.exceptions.FeatureFormValidationException
import com.arcgismaps.toolkit.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.ValidationErrorVisibility
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.featureforms.R
import com.esri.arcgismaps.sample.featureforms.compoenents.ErrorInfo
import com.esri.arcgismaps.sample.featureforms.compoenents.MapViewModel
import com.esri.arcgismaps.sample.featureforms.compoenents.UIState
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(mapViewModel: MapViewModel) {

    val scope = rememberCoroutineScope()
    val uiState by mapViewModel.uiState
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    val (featureForm, errorVisibility) = remember(uiState) {
        when (uiState) {
            is UIState.Editing -> {
                val state = (uiState as UIState.Editing)
                Pair(state.featureForm, state.validationErrorVisibility)
            }

            is UIState.Committing -> {
                Pair(
                    (uiState as UIState.Committing).featureForm,
                    ValidationErrorVisibility.Automatic
                )
            }

            is UIState.Switching -> {
                val state = uiState as UIState.Switching
                Pair(
                    state.oldState.featureForm, state.oldState.validationErrorVisibility
                )
            }

            else -> {
                Pair(null, ValidationErrorVisibility.Automatic)
            }
        }
    }
    var showDiscardEditsDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { SampleTopAppBar(title = "Feature Forms Sample") }
    ) { padding ->
        // show the composable map using the mapViewModel
        MapView(
            arcGISMap = mapViewModel.map,
            mapViewProxy = mapViewModel.proxy,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            onSingleTapConfirmed = { mapViewModel.onSingleTapConfirmed(it) }
        )

        LaunchedEffect(featureForm) {
            showBottomSheet = featureForm != null
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    showDiscardEditsDialog = true
                },
                sheetState = sheetState
            ) {
                // Sheet content
                val isSwitching = uiState is UIState.Switching
                // remember the form and update it when a new form is opened
                val rememberedForm = remember(this, isSwitching) {
                    featureForm!!
                }

                // show the top bar which changes available actions based on if the FeatureForm is
                // being shown and is in edit mode
                TopFormBar(
                    onClose = {
                        showDiscardEditsDialog = true
                    },
                    onSave = {
                        scope.launch {
                            mapViewModel.commitEdits().onFailure {
                                Log.w("Forms", "Applying edits failed : ${it.message}")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Applying edits failed : ${it.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    })

                // set bottom sheet content to the FeatureForm
                FeatureForm(
                    featureForm = rememberedForm,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 20.dp),
                    validationErrorVisibility = errorVisibility
                )

            }
        }
    }
    when (uiState) {
        is UIState.Committing -> {
            SubmitForm(errors = (uiState as UIState.Committing).errors) {
                mapViewModel.cancelCommit()
            }
        }

        is UIState.Switching -> {
            DiscardEditsDialog(
                onConfirm = { mapViewModel.selectNewFeature() },
                onCancel = { mapViewModel.continueEditing() }
            )
        }

        else -> {}
    }

    if (showDiscardEditsDialog) {
        DiscardEditsDialog(
            onConfirm = {
                mapViewModel.rollbackEdits()
                showDiscardEditsDialog = false
            },
            onCancel = {
                showDiscardEditsDialog = false
                scope.launch {
                    showBottomSheet = true
                    sheetState.show()
                    mapViewModel.continueEditing()
                }
            }
        )
    }

}

@Composable
fun DiscardEditsDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = stringResource(R.string.discard))
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        title = {
            Text(text = stringResource(R.string.discard_edits))
        },
        text = {
            Text(text = stringResource(R.string.all_changes_will_be_lost))
        }
    )
}

@Composable
fun TopFormBar(
    onClose: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Absolute.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Feature Editor"
                )
            }
            Text(
                text = "Edit feature",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onSave) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save Feature",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        HorizontalDivider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubmitForm(errors: List<ErrorInfo>, onDismissRequest: () -> Unit) {
    if (errors.isEmpty()) {
        // show a progress dialog if no errors are present
        BasicAlertDialog(onDismissRequest = { /* cannot be dismissed */ }) {
            Card(modifier = Modifier.wrapContentSize()) {
                Column(
                    modifier = Modifier.padding(15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(50.dp), strokeWidth = 5.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Saving..")
                }
            }
        }
    } else {
        // show all the validation errors in a dialog
        AlertDialog(
            onDismissRequest = onDismissRequest,
            modifier = Modifier.heightIn(max = 600.dp),
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.view))
                    }
                }
            },
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.the_form_has_errors),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.errors_must_be_fixed_to_submit_this_form),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(15.dp)) {
                        Text(
                            text = stringResource(R.string.attributes_failed, errors.count()),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyColumn(
                            modifier = Modifier,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(errors.count()) { index ->
                                val errorString =
                                    "${errors[index].fieldName} : ${errors[index].error.getString()}"
                                Text(text = errorString, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun FeatureFormValidationException.getString(): String {
    return when (this) {
        is FeatureFormValidationException.IncorrectValueTypeException -> {
            stringResource(id = R.string.value_must_be_of_correct_type)
        }

        is FeatureFormValidationException.LessThanMinimumDateTimeException -> {
            stringResource(id = R.string.date_less_than_minimum)
        }

        is FeatureFormValidationException.MaxCharConstraintException -> {
            stringResource(id = R.string.maximum_character_length_exceeded)
        }

        is FeatureFormValidationException.MaxDateTimeConstraintException -> {
            stringResource(id = R.string.date_exceeds_maximum)
        }

        is FeatureFormValidationException.MaxNumericConstraintException -> {
            stringResource(id = R.string.exceeds_maximum_value)
        }

        is FeatureFormValidationException.MinCharConstraintException -> {
            stringResource(id = R.string.minimum_character_length_not_met)
        }

        is FeatureFormValidationException.MinNumericConstraintException -> {
            stringResource(id = R.string.less_than_minimum_value)
        }

        is FeatureFormValidationException.NullNotAllowedException -> {
            stringResource(id = R.string.value_must_not_be_empty)
        }

        is FeatureFormValidationException.OutOfDomainException -> {
            stringResource(id = R.string.value_must_be_within_domain)
        }

        is FeatureFormValidationException.RequiredException -> {
            stringResource(id = R.string.required)
        }

        is FeatureFormValidationException.UnknownFeatureFormException -> {
            stringResource(id = R.string.unknown_error)
        }
    }
}
