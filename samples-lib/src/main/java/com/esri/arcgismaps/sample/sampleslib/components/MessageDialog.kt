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

package com.esri.arcgismaps.sample.sampleslib.components

import android.content.res.Configuration
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import com.arcgismaps.exceptions.ArcGISException
import com.arcgismaps.exceptions.CriticalRenderingException
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

/**
 * Composable component to display a dialog with a message
 */
@Composable
fun MessageDialog(
    title: String,
    description: String = "",
    icon: ImageVector = Icons.Filled.Info,
    onDismissRequest: () -> Unit
) {
    Log.e("SampleAlertMessage", "$title: $description")
    // display a dialog with a description text
    if (description.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { onDismissRequest() },
            icon = { Icon(imageVector = icon, contentDescription = null) },
            title = { Text(title) },
            text = { Text(description) },
            confirmButton = {
                TextButton(onClick = { onDismissRequest() }) {
                    Text("Dismiss")
                }
            },
        )
    } else {
        // display a dialog without a description text
        AlertDialog(
            onDismissRequest = { onDismissRequest() },
            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
            title = { Text(title) },
            confirmButton = {
                TextButton(onClick = { onDismissRequest() }) {
                    Text("Dismiss")
                }
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewMessageDialog() {
    SampleAppTheme {
        MessageDialog(
            title = "Message title",
            description = "Dialog loading message here",
            onDismissRequest = { }
        )
    }
}

class MessageDialogViewModel : ViewModel() {
    // display dialog when status is true
    var dialogStatus by mutableStateOf(false)
        private set

    var messageTitle by mutableStateOf("")
        private set

    var messageDescription by mutableStateOf("")
        private set

    /**
     * Displays a message dialog
     */
    fun showMessageDialog(title: String, description: String = "") {
        messageTitle = title
        messageDescription = description
        dialogStatus = true
    }

    fun showMessageDialog(throwable: Throwable?) {
        throwable ?: return
        SampleDialogExceptionRelay.publish(throwable)
        when (throwable) {
            is CriticalRenderingException -> showMessageDialog(throwable)
            is ArcGISException -> showMessageDialog(throwable)
            else -> {
                showMessageDialog(
                    title = throwable.message.toString(),
                    description = throwable.cause.toString()
                )
            }
        }
    }

    /**
     * See [platform error codes](https://developers.arcgis.com/kotlin/reference/platform-error-codes/).
     */
    fun showMessageDialog(exception: ArcGISException) {
        showMessageDialog(
            title = "ArcGIS Maps SDK error",
            description = exception.message
        )
    }

    fun showMessageDialog(exception: CriticalRenderingException) {
        showMessageDialog(
            title = "GeoView critical rendering error",
            description = exception.message
        )
    }

    /**
     * Dismiss the message dialog
     */
    fun dismissDialog() {
        dialogStatus = false
    }
}

/**
 * Process-wide queue for sample dialog exceptions so tests can fail with the original exception.
 */
object SampleDialogExceptionRelay {
    private val pending = ArrayDeque<Throwable>()

    @Synchronized
    fun publish(throwable: Throwable) {
        pending.addLast(throwable)
    }

    @Synchronized
    fun consume(): Throwable? = if (pending.isEmpty()) null else pending.removeFirst()

    @Synchronized
    fun clear() {
        pending.clear()
    }
}
