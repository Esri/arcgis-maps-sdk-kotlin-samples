package com.esri.arcgismaps.sample.showcoordinatesinmultipleformats.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun CoordinatesLayout(
    modifier: Modifier = Modifier,
    onQuerySubmit: (String) -> Unit,
    decimalDegrees: MutableState<String>,
    degreesMinutesSeconds: MutableState<String>,
    utm: MutableState<String>,
    usng: MutableState<String>
) {

    Column(modifier) {
        OutlinedTextField(
            modifier = modifier,
            value = decimalDegrees.value,
            maxLines = 1,
            onValueChange = {  decimalDegrees.value = it },
            label = { Text("Decimal Degrees") },
            keyboardActions = KeyboardActions(
                onSearch = {
                    onQuerySubmit(decimalDegrees.value)
                },
            ),
        )

        OutlinedTextField(
            modifier = modifier,
            value = degreesMinutesSeconds.value,
            maxLines = 1,
            onValueChange = { degreesMinutesSeconds.value = it },
            label = { Text("Degrees, Minutes, Seconds") },
            keyboardActions = KeyboardActions(
                onSearch = {
                    onQuerySubmit(degreesMinutesSeconds.value)
                },
            ),
        )

        OutlinedTextField(
            modifier = modifier,
            value = utm.value,
            maxLines = 1,
            onValueChange = { utm.value = it },
            label = { Text("UTM") },
            keyboardActions = KeyboardActions(
                onSearch = {
                    onQuerySubmit(utm.value)
                },
            ),
        )

        OutlinedTextField(
            modifier = modifier,
            value = usng.value,
            maxLines = 1,
            onValueChange = { usng.value = it },
            label = { Text("USNG") },
            keyboardActions = KeyboardActions(
                onSearch = {
                    onQuerySubmit(usng.value)
                },
            ),
        )
    }

}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewCoordinatesLayout(){
    CoordinatesLayout(
        onQuerySubmit = {},
        decimalDegrees = remember { mutableStateOf("") },
        degreesMinutesSeconds = remember { mutableStateOf("") },
        utm = remember { mutableStateOf("") },
        usng = remember { mutableStateOf("") },
    )
}