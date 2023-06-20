package com.esri.arcgismaps.sample.manageoperationallayers.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LayersList(
    layerNames: List<String>,
    onMoveLayerUp: (String) -> Unit = {},
    onMoveLayerDown: (String) -> Unit = {},
    onToggleLayerVisibility: (String) -> Unit = {},
) {
    LazyColumn(modifier = Modifier.padding(12.dp)) {
        items(layerNames.size, key = { layerNames[it] }) { index ->
            LayerRow(
                modifier = Modifier.fillMaxWidth(),
                layerName = layerNames[index],
                onMoveLayerUp = onMoveLayerUp,
                onMoveLayerDown = onMoveLayerDown,
                onToggleLayerVisibility = onToggleLayerVisibility
            )
        }
    }
}

@Composable
fun LayerRow(
    modifier: Modifier = Modifier,
    layerName: String,
    onMoveLayerUp: (String) -> Unit = {},
    onMoveLayerDown: (String) -> Unit = {},
    onToggleLayerVisibility: (String) -> Unit = {},
) {
    Card(modifier = modifier.padding(4.dp)) {
        Row(modifier = modifier) {
            Text(modifier = Modifier.padding(12.dp), text = layerName)
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
                IconButton(onClick = { onToggleLayerVisibility(layerName) }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Toggle visibility"
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
    LayersList(listOf("Layer 1", "Layer 2", "Layer 3"))
}