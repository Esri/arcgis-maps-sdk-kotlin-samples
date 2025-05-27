package com.esri.arcgismaps.sample.addwfslayer.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.addwfslayer.components.AddWfsLayerViewModel

/**
 * Main screen layout for the AddWfsLayer sample app.
 */
@Composable
fun AddWfsLayerScreen(sampleName: String) {
    val mapViewModel: AddWfsLayerViewModel = viewModel()
    val isPopulating by mapViewModel.isPopulating.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MapView(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        arcGISMap = mapViewModel.arcGISMap,
                        mapViewProxy = mapViewModel.mapViewProxy,
                        onVisibleAreaChanged = { polygon: Polygon ->
                            mapViewModel.onVisibleAreaChanged(polygon)
                        },
                        onNavigationChanged = { isNavigating ->
                            mapViewModel.onNavigatingChanged(isNavigating)
                        }
                    )
                }
                if (isPopulating) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center),
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Populating features...")
                            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
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
        }
    )
}
