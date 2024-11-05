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

package com.esri.arcgismaps.sample.editfeaturesusingfeatureforms.components

import android.app.Application
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.exceptions.FeatureFormValidationException
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FeatureFormDefinition
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.editfeaturesusingfeatureforms.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A view model for the MainScreen UI
 */
class MapViewModel(application: Application) : AndroidViewModel(application) {

    val mapViewProxy = MapViewProxy()

    // feature forms enabled web-map showcasing places of interest with form fields
    private var portalItem = PortalItem(application.getString(R.string.feature_form_web_map))

    val map = ArcGISMap(portalItem)

    // keep track of the selected feature form
    private val _featureForm = MutableStateFlow<FeatureForm?>(null)
    val featureForm: StateFlow<FeatureForm?> = _featureForm.asStateFlow()

    // keep track of the list of validation errors
    private val _errors = MutableStateFlow<List<ErrorInfo>>(listOf())
    val errors: StateFlow<List<ErrorInfo>> = _errors.asStateFlow()

    // create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            // load a map that has a FeatureFormDefinition on any of its layers
            map.load()
        }
    }

    /**
     * Apply attribute edits to the Geodatabase backing the ServiceFeatureTable
     * and refresh the local feature. Persisting changes to attributes is
     * not part of the FeatureForm API.
     *
     * @param onEditsCompleted Invoked when edits are applied successfully
     */
    fun applyEdits(onEditsCompleted: () -> Unit) {
        val featureForm = _featureForm.value
            ?: return messageDialogVM.showMessageDialog("Feature form state is not configured")

        // update the state flow with the list of validation errors found
        _errors.value = validateFormInputEdits(featureForm)
        // if there are no errors then update the feature
        if (_errors.value.isEmpty()) {
            val serviceFeatureTable = featureForm.feature.featureTable as? ServiceFeatureTable
                ?: return messageDialogVM.showMessageDialog("Cannot save feature edit without a ServiceFeatureTable")

            viewModelScope.launch {
                // commits changes of the edited feature to the database
                featureForm.finishEditing().onSuccess {
                    serviceFeatureTable.serviceGeodatabase?.let { database ->
                        if (database.serviceInfo?.canUseServiceGeodatabaseApplyEdits == true) {
                            // applies all local edits in the tables to the service
                            database.applyEdits().onFailure {
                                return@onFailure messageDialogVM.showMessageDialog(
                                    title = it.message.toString(),
                                    description = it.cause.toString()
                                )
                            }
                        } else {
                            // uploads any changes to the local table to the feature service
                            serviceFeatureTable.applyEdits().onFailure {
                                return@onFailure messageDialogVM.showMessageDialog(
                                    title = it.message.toString(),
                                    description = it.cause.toString()
                                )
                            }
                        }
                    }
                    // resets the attributes and geometry to the values in the data source
                    featureForm.feature.refresh()
                    // unselect the feature after the edits have been saved
                    (featureForm.feature.featureTable?.layer as FeatureLayer).clearSelection()
                    // dismiss dialog when edits are completed
                    onEditsCompleted()
                }.onFailure {
                    return@onFailure messageDialogVM.showMessageDialog(
                        title = it.message.toString(),
                        description = it.cause.toString()
                    )
                }
            }
        }
    }

    /**
     * Performs validation checks on the given [featureForm] with local edits.
     * Return a list of [ErrorInfo] if errors are found, if not, empty list is returned.
     */
    private fun validateFormInputEdits(featureForm: FeatureForm): List<ErrorInfo> {
        val errors = mutableListOf<ErrorInfo>()
        // If an element is editable or derives its value from an arcade expression,
        // its errors must be corrected before submitting the form
        featureForm.validationErrors.value.forEach { entry ->
            entry.value.forEach { error ->
                featureForm.elements.getFormElement(entry.key)?.let { formElement ->
                    if (formElement.isEditable.value || formElement.hasValueExpression) {
                        errors.add(
                            ErrorInfo(
                                fieldName = formElement.label,
                                error = error as FeatureFormValidationException
                            )
                        )
                    }
                }
            }
        }
        return errors
    }

    /**
     * Cancels the commit by resetting the validation errors.
     */
    fun cancelCommit() {
        // reset the validation errors
        _errors.value = listOf()
    }

    /**
     * Discard edits and unselects feature from the layer
     */
    fun rollbackEdits() {
        // discard local edits to the feature form
        _featureForm.value?.discardEdits()
        // unselect the feature
        (_featureForm.value?.feature?.featureTable?.layer as FeatureLayer).clearSelection()
        // reset the validation errors
        _errors.value = listOf()
    }

    /**
     * Perform an identify the tapped [ArcGISFeature] and retrieve the
     * layer's [FeatureFormDefinition] to create the respective [FeatureForm]
     */
    fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
        viewModelScope.launch {
            mapViewProxy.identifyLayers(
                screenCoordinate = singleTapEvent.screenCoordinate,
                tolerance = 22.dp,
                returnPopupsOnly = false
            ).onSuccess { results ->
                try {
                    results.forEach { result ->
                        result.geoElements.firstOrNull {
                            it is ArcGISFeature && (it.featureTable?.layer as? FeatureLayer)?.featureFormDefinition != null
                        }?.let {
                            val feature = it as ArcGISFeature
                            val layer = feature.featureTable!!.layer as FeatureLayer
                            val featureForm = FeatureForm(feature, layer.featureFormDefinition!!)
                            // select the feature
                            layer.selectFeature(feature)
                            // set the UI to an editing state with the FeatureForm
                            _featureForm.value = featureForm
                        }
                    }
                } catch (e: Exception) {
                    messageDialogVM.showMessageDialog(
                        title = "Failed to create feature form for the feature",
                        description = e.message.toString()
                    )
                }
            }
        }
    }
}

/**
 * Returns the [FieldFormElement] with the given [fieldName] in the [FeatureForm]. If none exists
 * null is returned.
 */
fun List<FormElement>.getFormElement(fieldName: String): FieldFormElement? {
    val fieldElements = filterIsInstance<FieldFormElement>()
    val element = if (fieldElements.isNotEmpty()) {
        fieldElements.firstNotNullOfOrNull {
            if (it.fieldName == fieldName) it else null
        }
    } else {
        null
    }

    return element ?: run {
        val groupElements = filterIsInstance<GroupFormElement>()
        if (groupElements.isNotEmpty()) {
            groupElements.firstNotNullOfOrNull {
                it.elements.getFormElement(fieldName)
            }
        } else {
            null
        }
    }
}

/**
 * Class that provides a validation error [error] for the field with name [fieldName].
 */
data class ErrorInfo(val fieldName: String, val error: FeatureFormValidationException)
