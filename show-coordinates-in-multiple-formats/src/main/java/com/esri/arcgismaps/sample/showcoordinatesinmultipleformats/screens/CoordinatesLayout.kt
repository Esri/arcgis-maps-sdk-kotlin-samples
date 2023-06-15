package com.esri.arcgismaps.sample.showcoordinatesinmultipleformats.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.esri.arcgismaps.sample.showcoordinatesinmultipleformats.components.MapViewModel

@Composable
fun CoordinatesLayout(mapViewModel: MapViewModel) {
    CoordinateField(
        notationType = MapViewModel.NotationType.DD,
        text = mapViewModel.decimalDegrees,
        labelText = "Decimal Degrees",
        onTextChanged = mapViewModel::setDecimalDegreesCoordinate,
        onQuerySubmit = mapViewModel::fromCoordinateNotationToPoint,
    )

    CoordinateField(
        notationType = MapViewModel.NotationType.DMS,
        text = mapViewModel.degreesMinutesSeconds,
        labelText = "Degrees, Minutes, Seconds",
        onTextChanged = mapViewModel::degreesMinutesSecondsCoordinate,
        onQuerySubmit = mapViewModel::fromCoordinateNotationToPoint,
    )

    CoordinateField(
        notationType = MapViewModel.NotationType.UTM,
        text = mapViewModel.utm,
        labelText = "UTM",
        onTextChanged = mapViewModel::setUTMCoordinate,
        onQuerySubmit = mapViewModel::fromCoordinateNotationToPoint,
    )

    CoordinateField(
        notationType = MapViewModel.NotationType.USNG,
        text = mapViewModel.usng,
        labelText = "USNG",
        onTextChanged = mapViewModel::setUSNGDegreesCoordinate,
        onQuerySubmit = mapViewModel::fromCoordinateNotationToPoint,
    )
}

@Composable
fun CoordinateField(
    modifier: Modifier = Modifier,
    notationType: MapViewModel.NotationType,
    text: String = "",
    onTextChanged: (String) -> Unit,
    onQuerySubmit: (MapViewModel.NotationType, String) -> Unit,
    labelText: String
) {
    // remember the OutlinedTextField's focus requester to change focus on search
    val focusRequester = remember { FocusRequester() }
    // focus manager is used to clear focus from OutlinedTextField on search
    val focusManager = LocalFocusManager.current
    // set the keyboard options for all field to search
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Search
    )

    OutlinedTextField(
        modifier = modifier.fillMaxWidth().focusRequester(focusRequester),
        value = text,
        maxLines = 1,
        onValueChange = onTextChanged,
        label = { Text(labelText) },
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(
            onSearch = {
                onQuerySubmit(notationType, text)
                focusManager.clearFocus()
            },
        ),
    )

    // if keyboard is closed, remove focus from text fields
    if (!keyboardAsState().value) {
        focusManager.clearFocus()
    }
}

/**
 * Composable function that returns a false state when keyboard is closed.
 */
@Composable
fun keyboardAsState(): State<Boolean> {
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    return rememberUpdatedState(isImeVisible)
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewCoordinatesLayout() {
    CoordinatesLayout(mapViewModel = viewModel())
}
