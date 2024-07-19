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

package com.esri.arcgismaps.sample.featureforms.components

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.exceptions.FeatureFormValidationException
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.featureforms.ValidationErrorVisibility
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapViewModel() : ViewModel() {

    // https://www.arcgis.com/home/item.html?id=f72207ac170a40d8992b7a3507b44fad"
    val portalItem = PortalItem(
        Portal("https://www.arcgis.com"),
        "f72207ac170a40d8992b7a3507b44fad"
    )

    // Create a map with a feature layer that contains building data.
    val map = ArcGISMap(portalItem)

    val proxy = MapViewProxy()

    private val _uiState: MutableState<UIState> = mutableStateOf(UIState.NotEditing)
    val uiState: State<UIState>
        get() = _uiState

    init {

    }


    fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
        viewModelScope.launch {
            proxy.identifyLayers(
                screenCoordinate = singleTapEvent.screenCoordinate,
                tolerance = 22.dp,
                returnPopupsOnly = false
            ).onSuccess { results ->
                try {
                    results.forEach { result ->
                        result.geoElements.firstOrNull {
                            it is ArcGISFeature && (it.featureTable?.layer as? FeatureLayer)?.featureFormDefinition != null
                        }?.let {
                            if (_uiState.value is UIState.Editing) {
                                val currentState = _uiState.value as UIState.Editing
                                val newFeature = it as ArcGISFeature
                                _uiState.value = UIState.Switching(
                                    oldState = currentState,
                                    newFeature = newFeature
                                )
                            } else if (_uiState.value is UIState.NotEditing) {
                                val feature = it as ArcGISFeature
                                val layer = feature.featureTable!!.layer as FeatureLayer
                                val featureForm =
                                    FeatureForm(feature, layer.featureFormDefinition!!)
                                // select the feature
                                layer.selectFeature(feature)
                                // set the UI to an editing state with the FeatureForm
                                _uiState.value = UIState.Editing(featureForm)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TAG", "failed to create a FeatureForm for the feature")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Apply attribute edits to the Geodatabase backing
     * the ServiceFeatureTable and refresh the local feature.
     *
     * Persisting changes to attributes is not part of the FeatureForm API.
     *
     * @return a Result indicating success, or any error encountered.
     */
    suspend fun commitEdits(): Result<Unit> {
        val state = (_uiState.value as? UIState.Editing)
            ?: return Result.failure(IllegalStateException("Not in editing state"))
        // build the list of errors
        val errors = mutableListOf<ErrorInfo>()
        val featureForm = state.featureForm
        featureForm.validationErrors.value.forEach { entry ->
            entry.value.forEach { error ->
                featureForm.elements.getFormElement(entry.key)?.let { formElement ->
                    if (formElement.isEditable.value) {
                        errors.add(
                            ErrorInfo(
                                formElement.label,
                                error as FeatureFormValidationException
                            )
                        )
                    }
                }
            }
        }
        // set the state to committing with the errors if any
        _uiState.value = UIState.Committing(
            featureForm = state.featureForm,
            errors = errors
        )
        // if there are no errors then update the feature
        return if (errors.isEmpty()) {
            val feature = state.featureForm.feature
            val serviceFeatureTable =
                feature.featureTable as? ServiceFeatureTable ?: return Result.failure(
                    IllegalStateException("cannot save feature edit without a ServiceFeatureTable")
                )
            val result = serviceFeatureTable.updateFeature(feature).map {
                serviceFeatureTable.serviceGeodatabase?.applyEdits()
                    ?: throw IllegalStateException("cannot apply feature edit without a ServiceGeodatabase")
                feature.refresh()
                // unselect the feature after the edits have been saved
                (feature.featureTable?.layer as FeatureLayer).clearSelection()
            }
            // set the state to not editing since the feature was updated successfully
            _uiState.value = UIState.NotEditing
            result
        } else {
            // even though there are errors send a success result since the operation was successful
            // and the control is back with the UI
            Result.success(Unit)
        }
    }

    /**
     * Cancels the commit if the current state is [UIState.Committing] and sets the ui state to
     * [UIState.Editing].
     */
    fun cancelCommit(): Result<Unit> {
        val previousState = (_uiState.value as? UIState.Committing) ?: return Result.failure(
            IllegalStateException("Not in committing state")
        )
        // set the state back to an editing state while showing all errors using
        // ValidationErrorVisibility.Always
        _uiState.value = UIState.Editing(
            previousState.featureForm,
            validationErrorVisibility = ValidationErrorVisibility.Visible
        )
        return Result.success(Unit)
    }

    fun selectNewFeature() =
        (_uiState.value as? UIState.Switching)?.let { prevState ->
            prevState.oldState.featureForm.discardEdits()
            val layer = prevState.oldState.featureForm.feature.featureTable?.layer as FeatureLayer
            layer.clearSelection()
            layer.selectFeature(prevState.newFeature)
            _uiState.value =
                UIState.Editing(
                    featureForm = FeatureForm(
                        prevState.newFeature,
                        layer.featureFormDefinition!!
                    )
                )
        }

    fun continueEditing() =
        (_uiState.value as? UIState.Switching)?.let { prevState ->
            _uiState.value = prevState.oldState
        }

    fun rollbackEdits(): Result<Unit> {
        (_uiState.value as? UIState.Editing)?.let {
            it.featureForm.discardEdits()
            // unselect the feature
            (it.featureForm.feature.featureTable?.layer as FeatureLayer).clearSelection()
            _uiState.value = UIState.NotEditing
            return Result.success(Unit)
        } ?: return Result.failure(IllegalStateException("Not in editing state"))
    }

}

/**
 * A UI state class that indicates the current editing state for a feature form.
 */
sealed class UIState {
    /**
     * Currently not editing.
     */
    object NotEditing : UIState()

    /**
     * Currently selecting a new Feature
     */
    data class Switching(
        val oldState: Editing,
        val newFeature: ArcGISFeature
    ) : UIState()

    /**
     * In editing state with the [featureForm] with the validation error visibility given by
     * [validationErrorVisibility].
     */
    data class Editing(
        val featureForm: FeatureForm,
        val validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic
    ) : UIState()

    /**
     * Commit in progress state for the [featureForm] with validation errors [errors].
     */
    data class Committing(
        val featureForm: FeatureForm,
        val errors: List<ErrorInfo>
    ) : UIState()
}

/**
 * Class that provides a validation error [error] for the field with name [fieldName]. To fetch
 * the actual message string use [FeatureFormValidationException.getString] in the composition.
 */
data class ErrorInfo(val fieldName: String, val error: FeatureFormValidationException)

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

