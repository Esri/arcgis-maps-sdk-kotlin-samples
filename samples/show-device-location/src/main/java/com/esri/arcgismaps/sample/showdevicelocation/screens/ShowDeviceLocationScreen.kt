/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.showdevicelocation.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.arcgismaps.ArcGISEnvironment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.Color
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.rememberLocationDisplay
import com.esri.arcgismaps.sample.showdevicelocation.components.ShowDeviceLocationViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.launch
import com.esri.arcgismaps.sample.showdevicelocation.R

/**
 * Main screen layout for the sample app
 */
@Composable
fun ShowDeviceLocationScreen(sampleName: String) {
    val mapViewModel: ShowDeviceLocationViewModel = viewModel()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // some parts of the API require an Android Context to properly interact with Android system
    // features, such as LocationProvider and application resources
    ArcGISEnvironment.applicationContext = context.applicationContext

    // Create and remember a location display with a recenter auto pan mode.
    val locationDisplay = rememberLocationDisplay().apply {
        setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
    }

    if (checkPermissions(context)) {
        // Permissions are already granted.
        LaunchedEffect(Unit) {
            mapViewModel.onItemSelected(mapViewModel.selectedItem.value.text, locationDisplay)
        }
    } else {
        RequestPermissions(
            context = context,
            onPermissionsGranted = {
                coroutineScope.launch {
                    mapViewModel.onItemSelected(mapViewModel.selectedItem.value.text, locationDisplay)
                }
            }
        )
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize(),
                    arcGISMap = mapViewModel.arcGISMap,
                    locationDisplay = locationDisplay,
                )

                Card(
                    modifier = Modifier
                        .clickable { mapViewModel.expanded.value = true }
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp)),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(5.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = mapViewModel.selectedItem.value.text)
                        Spacer(modifier = Modifier.width(8.dp))
                        NinePatchImage(
                            imageId = mapViewModel.selectedItem.value.imageId,
                            modifier = Modifier.size(25.dp),
                        )
                    }
                }

                DropdownMenu(
                    expanded = mapViewModel.expanded.value,
                    onDismissRequest = { mapViewModel.expanded.value = false },
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                ) {
                    mapViewModel.dropDownMenuOptions.forEach { option ->
                        DropdownMenuItem(
                            text = @Composable{ Text(option.text) },
                            trailingIcon = @Composable{ NinePatchImage(
                                imageId = option.imageId,
                                modifier = Modifier.size(25.dp),
                            )},
                            onClick = {
                                mapViewModel.expanded.value = false
                                mapViewModel.selectedItem.value = option
                                mapViewModel.onItemSelected(option.text, locationDisplay)
                            },
                        )
                    }
                }
            }

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
    )
}


@Composable
fun RequestPermissions(context: Context, onPermissionsGranted: () -> Unit) {

    // Create an activity result launcher using permissions contract and handle the result.
    val activityResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if both fine & coarse location permissions are true.
        if (permissions.all { it.value }) {
            onPermissionsGranted()
        } else {
            showError(context, "Location permissions were denied")
        }
    }

    LaunchedEffect(Unit) {
        activityResultLauncher.launch(
            // Request both fine and coarse location permissions.
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }
}

@Composable
fun NinePatchImage(imageId: Int, modifier: Modifier,) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                setImageResource(imageId)
            }
        },
        modifier = modifier,
        update = {
            it.setImageResource(imageId)
        }
    )
}


fun checkPermissions(context: Context): Boolean {
    // Check permissions to see if both permissions are granted.
    // Coarse location permission.
    val permissionCheckCoarseLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    // Fine location permission.
    val permissionCheckFineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return permissionCheckCoarseLocation && permissionCheckFineLocation
}


fun showError(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
