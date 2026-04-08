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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.arcgismaps.toolkit.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.FeatureFormEditingEvent
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureforms.ValidationErrorVisibility
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormDefaults
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.editfeaturesusingfeatureforms.R
import com.esri.arcgismaps.sample.editfeaturesusingfeatureforms.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(mapViewModel: MapViewModel) {

    val scope = rememberCoroutineScope()
    // the feature form the currently selected feature
    val featureFormState = mapViewModel.featureFormState

    // boolean trackers for save and discard edits dialogs
    var showSaveEditsDialog by remember { mutableStateOf(false) }
    var showDiscardEditsDialog by remember { mutableStateOf(false) }

    // The bottom sheet state used to control the visibility of the feature form
    val sheetState = rememberModalBottomSheetState(
        confirmValueChange = { sheetValue ->
            if (sheetValue != SheetValue.Hidden) return@rememberModalBottomSheetState true
            if (featureFormState?.hasEdits() == true) {
                // if there are unsaved edits, show the discard edits dialog
                showDiscardEditsDialog = true
                false
            } else {
                true
            }
        }
    )

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

        if (featureFormState != null) {
            // display feature form bottom sheet
            ModalBottomSheet(
                onDismissRequest = {
                    // clear the selected feature when the bottom sheet is dismissed
                    mapViewModel.clearSelection()
                },
                sheetState = sheetState
            ) {
                // display the selected feature form using the Toolkit component
                FeatureForm(
                    featureFormState = featureFormState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 20.dp)
                        .navigationBarsPadding(),
                    showCloseIcon = true,
                    validationErrorVisibility = ValidationErrorVisibility.Automatic,
                    onDismiss = {
                        // if there are edits, show the discard edits dialog, otherwise hide the
                        // bottom sheet
                        if (featureFormState.hasEdits()) {
                            showDiscardEditsDialog = true
                        } else {
                            scope.launch {
                                sheetState.hide()
                                mapViewModel.clearSelection()
                            }
                        }
                    },
                    onEditingEvent = { event ->
                        when (event) {
                            is FeatureFormEditingEvent.SavedEdits -> {
                                // when the save edits event is received, attempt to apply edits
                                showSaveEditsDialog = true
                                mapViewModel.applyEdits {
                                    showSaveEditsDialog = false
                                }
                            }

                            else -> {}
                        }
                    },
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

    if (showSaveEditsDialog) {
        // no validation errors found, show dialog when committing edits
        SaveFormDialog()
    }

    if (showDiscardEditsDialog) {
        DiscardEditsDialog(
            onConfirm = {
                scope.launch {
                    featureFormState?.discardEdits()
                    sheetState.hide()
                    showDiscardEditsDialog = false
                }
            },
            onCancel = {
                showDiscardEditsDialog = false
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

/**
 * Extension function to check if there are unsaved edits in the feature form.
 */
private fun FeatureFormState.hasEdits(): Boolean {
    return this.activeFeatureForm.hasEdits.value
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
fun DiscardEditsDialogPreview() {
    SampleAppTheme { DiscardEditsDialog(onConfirm = {}) {} }
}
