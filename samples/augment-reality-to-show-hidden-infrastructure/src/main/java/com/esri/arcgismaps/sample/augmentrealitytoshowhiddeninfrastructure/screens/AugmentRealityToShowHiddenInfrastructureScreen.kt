package com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.components.AugmentRealityToShowHiddenInfrastructureViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import androidx.compose.ui.graphics.Color as ComposeColor

@Composable
fun AugmentRealityToShowHiddenInfrastructureScreen(sampleName: String) {
    val mapViewModel: AugmentRealityToShowHiddenInfrastructureViewModel = viewModel()
    val statusMessage by mapViewModel.statusMessage.collectAsState()
    val canDelete by mapViewModel.canDelete.collectAsState()
    val canApplyEdits by mapViewModel.canApplyEdits.collectAsState()
    val geometryEditorCanUndo by mapViewModel.geometryEditorCanUndo.collectAsState()
    val context = LocalContext.current
    var isViewmodelInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(isViewmodelInitialized) {
        if (!isViewmodelInitialized) {
            mapViewModel.startDrawingPipe()
            isViewmodelInitialized = true
        }
    }
    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                MapView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISMap = mapViewModel.arcGISMap,
                    graphicsOverlays = listOf(mapViewModel.pipesGraphicsOverlay),
                    locationDisplay = mapViewModel.locationDisplay,
                    geometryEditor = mapViewModel.geometryEditor,
                    mapViewProxy = mapViewModel.mapViewProxy
                )
                // Status message overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ComposeColor.Black.copy(alpha = 0.7f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = statusMessage,
                        color = ComposeColor.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // Bottom toolbar
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { mapViewModel.undoOrDelete() },
                        enabled = geometryEditorCanUndo || canDelete
                    ) {
                        Icon(
                            imageVector = if (geometryEditorCanUndo) Icons.Filled.Clear else Icons.Filled.Delete,
                            contentDescription = if (geometryEditorCanUndo) "Undo" else "Delete",
                            tint = if (geometryEditorCanUndo || canDelete) ComposeColor.White else ComposeColor.Gray
                        )
                    }
                    IconButton(
                        onClick = { /* TODO: Launch AR view here */ },
                        enabled = canDelete && !geometryEditorCanUndo
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "View in AR",
                            tint = if (canDelete && !geometryEditorCanUndo) ComposeColor.White else ComposeColor.Gray
                        )
                    }
                    IconButton(
                        onClick = { mapViewModel.applyPipe() },
                        enabled = canApplyEdits
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Done",
                            tint = if (canApplyEdits) ComposeColor.White else ComposeColor.Gray
                        )
                    }
                }
                // Elevation input dialog
                if (mapViewModel.isElevationDialogVisible) {
                    ElevationInputDialog(
                        onConfirm = { elevation -> mapViewModel.confirmElevationInput(elevation) },
                        onCancel = { mapViewModel.cancelElevationInput() }
                    )
                }
                // Error dialog
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
        }
    )
}

@Composable
fun ElevationInputDialog(
    onConfirm: (Double) -> Unit,
    onCancel: () -> Unit
) {
    var textState by remember { mutableStateOf(TextFieldValue("0.0")) }
    var isError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Enter an Elevation") },
        text = {
            Column {
                OutlinedTextField(
                    value = textState,
                    onValueChange = {
                        textState = it
                        isError = false
                    },
                    label = { Text("Elevation offset (-10 to 10 meters)") },
                    keyboardOptions = KeyboardOptions(),
                    isError = isError
                )
                if (isError) {
                    Text(
                        text = "Enter a value between -10 and 10.",
                        color = ComposeColor.Red,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = textState.text.toDoubleOrNull()
                if (value != null && value in -10.0..10.0) {
                    onConfirm(value)
                } else {
                    isError = true
                }
            }) {
                Text("Done")
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewElevationInputDialog() {
    Surface { ElevationInputDialog(onConfirm = {}, onCancel = {}) }
}
