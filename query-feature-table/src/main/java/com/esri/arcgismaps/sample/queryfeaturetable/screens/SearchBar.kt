package com.esri.arcgismaps.sample.queryfeaturetable.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    onQuerySubmit: (String) -> Unit
) {
    // query text typed in OutlinedTextField
    var text by rememberSaveable { mutableStateOf("") }
    // remember the OutlinedTextField's focus requester to change focus on search
    val focusRequester = remember { FocusRequester() }
    // focus manager is used to clear focus from OutlinedTextField on search
    val focusManager = LocalFocusManager.current

    Row(modifier) {
        OutlinedTextField(
            modifier = modifier.focusRequester(focusRequester),
            value = text,
            maxLines = 1,
            onValueChange = { text = it },
            label = { Text("Search a US state") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onQuerySubmit(text)
                    focusManager.clearFocus()
                },
            ),
            trailingIcon = {
                IconButton(onClick = {
                    text = ""
                    focusManager.clearFocus()
                }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "ClearIcon"
                    )
                }
            },
        )
    }

}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewSearchBar() {
    SearchBar(onQuerySubmit = {})
}