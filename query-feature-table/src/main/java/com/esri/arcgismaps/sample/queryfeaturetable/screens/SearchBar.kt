/* Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.queryfeaturetable.screens

import android.content.res.Configuration
import android.view.KeyEvent
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
import androidx.compose.ui.input.key.onKeyEvent
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
            modifier = modifier.focusRequester(focusRequester).onKeyEvent {
                // submit query when enter is tapped
                if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                    onQuerySubmit(text)
                    focusManager.clearFocus()
                }
                false
            },
            value = text,
            maxLines = 1,
            singleLine = true,
            onValueChange = { text = it.lines()[0] },
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