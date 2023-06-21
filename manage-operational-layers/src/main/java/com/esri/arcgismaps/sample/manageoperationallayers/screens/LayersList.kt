package com.esri.arcgismaps.sample.manageoperationallayers.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.layers.Layer
import com.esri.arcgismaps.sample.manageoperationallayers.R

/**
 * Layout to display a list of operational layers on the map using [layerNames].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LayersList(
    layerNames: List<String>,
    inactiveLayers: List<Layer>,
    onMoveLayerUp: (String) -> Unit = {},
    onMoveLayerDown: (String) -> Unit = {},
    onRemoveLayer: (String) -> Unit = {},
    onAddLayer: (String) -> Unit = {},
) {
    Column {
        Text(
            modifier = Modifier.padding(12.dp),
            text = "Active layers"
        )
        LazyColumn(modifier = Modifier.padding(12.dp)) {
            items(layerNames.size, key = { layerNames[it] }) { index ->
                LayerRow(
                    modifier = Modifier.fillMaxWidth().animateItemPlacement(),
                    layerName = layerNames[index],
                    onMoveLayerUp = onMoveLayerUp,
                    onMoveLayerDown = onMoveLayerDown,
                    onRemoveLayer = onRemoveLayer
                )
            }
        }
        Text(
            modifier = Modifier.padding(12.dp),
            text = "Inactive layers"
        )
        LazyColumn(modifier = Modifier.padding(12.dp)) {
            items(inactiveLayers.size, key = { inactiveLayers[it].name }) { index ->
                InactiveLayerRow(
                    modifier = Modifier.fillMaxWidth().animateItemPlacement(),
                    layerName = inactiveLayers[index].name,
                    onAddLayer = onAddLayer
                )
            }
        }
    }
}

@Composable
fun LayerRow(
    modifier: Modifier = Modifier,
    layerName: String,
    onMoveLayerUp: (String) -> Unit = {},
    onMoveLayerDown: (String) -> Unit = {},
    onRemoveLayer: (String) -> Unit = {},
) {
    Card(modifier = modifier.padding(4.dp)) {
        Row(modifier = modifier) {
            Text(
                modifier = Modifier.padding(12.dp),
                text = layerName
            )
            Spacer(Modifier.weight(1f))
            Row {
                IconButton(onClick = { onMoveLayerUp(layerName) }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Up arrow"
                    )
                }
                IconButton(onClick = { onMoveLayerDown(layerName) }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Up arrow"
                    )
                }
                IconButton(onClick = { onRemoveLayer(layerName) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_show),
                        contentDescription = "Show icon"
                    )
                }
            }
        }
    }
}

@Composable
fun InactiveLayerRow(
    modifier: Modifier = Modifier,
    layerName: String,
    onAddLayer: (String) -> Unit = {},
) {
    Card(modifier = modifier.padding(4.dp)) {
        Row(modifier = modifier) {
            Text(
                modifier = Modifier.padding(12.dp),
                text = layerName
            )
            Spacer(Modifier.weight(1f))
            Row {
                IconButton(onClick = { onAddLayer(layerName) }) {
                    Icon(
                        painter = painterResource(R.drawable.hide),
                        contentDescription = "Show icon"
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewLayersList() {
    LayersList(
        layerNames = listOf("Layer 1", "Layer 2", "Layer 3"),
        inactiveLayers = listOf()
    )
}
