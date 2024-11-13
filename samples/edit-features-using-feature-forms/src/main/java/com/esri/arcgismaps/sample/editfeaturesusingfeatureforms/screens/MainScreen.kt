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

package com.esri.arcgismaps.sample.editfeaturesusingfeatureforms.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.arcgismaps.toolkit.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.ValidationErrorVisibility
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormDefaults
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.editfeaturesusingfeatureforms.R
import com.esri.arcgismaps.sample.editfeaturesusingfeatureforms.components.ErrorInfo
import com.esri.arcgismaps.sample.editfeaturesusingfeatureforms.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(mapViewModel: MapViewModel) {

    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // the feature form the currently selected feature
    val featureForm by mapViewModel.featureForm.collectAsState()
    // the validation errors found when the edits are applied
    val formValidationErrors by mapViewModel.errors.collectAsState()

    // boolean trackers for save and discard edits dialogs
    var showSaveEditsDialog by remember { mutableStateOf(false) }
    var showDiscardEditsDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { SampleTopAppBar(title = stringResource(R.string.edit_features_using_feature_forms_app_name)) }
    ) { padding ->
        // display the composable map using the mapViewModel
        MapView(
            arcGISMap = mapViewModel.map,
            mapViewProxy = mapViewModel.mapViewProxy,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            onSingleTapConfirmed = { mapViewModel.onSingleTapConfirmed(it) }
        )

        // update bottom sheet visibility when a feature is selected
        LaunchedEffect(featureForm) {
            showBottomSheet = featureForm != null
        }

        if (showBottomSheet && featureForm != null) {
            // display feature form bottom sheet
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    showDiscardEditsDialog = true
                },
                sheetState = sheetState
            ) {
                // top bar to manage save or discard edits
                TopFormBar(
                    onClose = { showDiscardEditsDialog = true },
                    onSave = {
                        showSaveEditsDialog = true
                        mapViewModel.applyEdits {
                            scope.launch {
                                sheetState.hide()
                                showBottomSheet = false
                                showSaveEditsDialog = false
                            }
                        }
                    })
                // display the selected feature form using the Toolkit component
                FeatureForm(
                    featureForm = featureForm!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 20.dp)
                        .navigationBarsPadding(),
                    validationErrorVisibility = ValidationErrorVisibility.Automatic,
                    colorScheme = FeatureFormDefaults.colorScheme(
                        groupElementColors = FeatureFormDefaults.groupElementColors(
                            outlineColor = MaterialTheme.colorScheme.secondary,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            supportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ),
                    typography = FeatureFormDefaults.typography(
                        readOnlyFieldTypography = FeatureFormDefaults.readOnlyFieldTypography(
                            labelStyle = MaterialTheme.typography.headlineSmall,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            supportingTextStyle = MaterialTheme.typography.labelLarge
                        )
                    )
                )
            }
        }
    }

    if (showSaveEditsDialog && formValidationErrors.isNotEmpty() && showBottomSheet) {
        // validation errors found, cancel the commit and show validation errors
        ValidationErrorsDialog(errors = formValidationErrors) {
            showSaveEditsDialog = false
            mapViewModel.cancelCommit()
        }
    } else if (showSaveEditsDialog && showBottomSheet) {
        // no validation errors found, show dialog when committing edits
        SaveFormDialog()
    }

    if (showDiscardEditsDialog) {
        DiscardEditsDialog(
            onConfirm = {
                mapViewModel.rollbackEdits()
                scope.launch {
                    sheetState.hide()
                    showDiscardEditsDialog = false
                    showBottomSheet = false
                }
            },
            onCancel = {
                showDiscardEditsDialog = false
                showBottomSheet = true
            }
        )
    }

    // Display a MessageDialog with any error information
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
            OutlinedButton(onClick = onCancel) {
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

@Composable
private fun SaveFormDialog() {
    // show a progress dialog when no errors are present
    Dialog(onDismissRequest = { /* cannot be dismissed */ }) {
        Card(modifier = Modifier.wrapContentSize()) {
            Column(
                modifier = Modifier.padding(25.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Saving..")
            }
        }
    }
}

@Composable
private fun ValidationErrorsDialog(errors: List<ErrorInfo>, onDismissRequest: () -> Unit) {
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
                    modifier = Modifier.fillMaxWidth(),
                    text = "Form Validation Errors",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Text(
                        text = stringResource(R.string.attributes_failed, errors.count()),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(errors.count()) { index ->
                            Text(
                                text = "${errors[index].fieldName}: ${errors[index].error::class.simpleName.toString()}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SavePreview() {
    SampleAppTheme { SaveFormDialog() }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ValidationErrorsPreview() {
    SampleAppTheme { ValidationErrorsDialog(listOf()) { } }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DiscardEditsDialogPreview() {
    SampleAppTheme { DiscardEditsDialog(onConfirm = {}) {} }
}

@Preview(showBackground = true)
@Composable
fun TopFormBarPreview() {
    SampleAppTheme { TopFormBar() }
}
