package com.esri.arcgismaps.sample.showcoordinatesinmultipleformats.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
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
import androidx.core.view.WindowCompat
import com.esri.arcgismaps.sample.showcoordinatesinmultipleformats.components.MapViewModel

@Composable
fun CoordinatesLayout(
    modifier: Modifier = Modifier,
    onQuerySubmit: (MapViewModel.NotationType, String) -> Unit,
    decimalDegrees: MutableState<String>,
    degreesMinutesSeconds: MutableState<String>,
    utm: MutableState<String>,
    usng: MutableState<String>
) {

    // remember the OutlinedTextField's focus requester to change focus on search
    val focusRequester = remember { FocusRequester() }
    // focus manager is used to clear focus from OutlinedTextField on search
    val focusManager = LocalFocusManager.current

    Column(modifier.focusRequester(focusRequester)) {
        // set the keyboard options for all field to search
        val keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        )
        // decimal degrees text field
        OutlinedTextField(
            modifier = modifier,
            value = decimalDegrees.value,
            maxLines = 1,
            onValueChange = { decimalDegrees.value = it },
            label = { Text("Decimal Degrees") },
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(
                onSearch = {
                    onQuerySubmit(MapViewModel.NotationType.DD, decimalDegrees.value)
                    focusManager.clearFocus()
                },
            ),
        )
        // decimal degrees, minutes, seconds text field
        OutlinedTextField(
            modifier = modifier,
            value = degreesMinutesSeconds.value,
            maxLines = 1,
            onValueChange = { degreesMinutesSeconds.value = it },
            label = { Text("Degrees, Minutes, Seconds") },
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(
                onSearch = {
                    onQuerySubmit(MapViewModel.NotationType.DMS, degreesMinutesSeconds.value)
                    focusManager.clearFocus()
                },
            ),
        )
        // Universal Transverse Mercator (UTM) text field
        OutlinedTextField(
            modifier = modifier,
            value = utm.value,
            maxLines = 1,
            onValueChange = { utm.value = it },
            label = { Text("UTM") },
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(
                onSearch = {
                    onQuerySubmit(MapViewModel.NotationType.UTM, utm.value)
                    focusManager.clearFocus()
                },
            ),
        )
        // United States National Grid (USNG) text field
        OutlinedTextField(
            modifier = modifier,
            value = usng.value,
            maxLines = 1,
            onValueChange = { usng.value = it },
            label = { Text("USNG") },
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(
                onSearch = {
                    onQuerySubmit(MapViewModel.NotationType.USNG, usng.value)
                    focusManager.clearFocus()
                },
            ),
        )

        // if keyboard is closed, remove focus from text fields
        if (!keyboardAsState().value) {
            focusManager.clearFocus()
        }
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
    CoordinatesLayout(
        onQuerySubmit = { _, _ -> },
        decimalDegrees = remember { mutableStateOf("") },
        degreesMinutesSeconds = remember { mutableStateOf("") },
        utm = remember { mutableStateOf("") },
        usng = remember { mutableStateOf("") },
    )
}
