package com.esri.arcgismaps.sample.sampleslib.components

import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SampleSnackbarHostState(snackbarHostState: SnackbarHostState) {
    return SnackbarHost(hostState = snackbarHostState) { data ->
        Snackbar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dismissActionContentColor = MaterialTheme.colorScheme.primary,
            snackbarData = data
        )
    }
}

fun showMessage(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    message: String,
    isError: Boolean = true
) {
    scope.launch {
        snackbarHostState.showSnackbar(
            message = message,
            withDismissAction = true
        )
    }
    if(isError)
        Log.e("Sample Error", message)
    else
        Log.i("Sample Info", message)
}
