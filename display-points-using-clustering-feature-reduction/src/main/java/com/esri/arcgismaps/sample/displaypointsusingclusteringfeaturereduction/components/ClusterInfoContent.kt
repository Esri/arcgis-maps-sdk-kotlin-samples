package com.esri.arcgismaps.sample.displaypointsusingclusteringfeaturereduction.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.sampleslib.theme.SampleTypography

@Composable
fun ClusterInfoContent(
    popupTitle: String,
    clusterInfoList: MutableList<AnnotatedString>
) {
    Column {
        Text(
            modifier = Modifier.padding(horizontal = 30.dp, vertical = 12.dp),
            text = popupTitle,
            style = SampleTypography.displaySmall
        )

        LazyColumn() {
            items(clusterInfoList.size) { index ->
                Divider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = Color.LightGray
                )
                Text(
                    modifier = Modifier.padding(horizontal = 30.dp, vertical = 16.dp),
                    text = clusterInfoList[index]
                )
            }
        }
        Spacer(modifier = Modifier.size(24.dp))
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun ClusterInfoContentPreview() {
    SampleAppTheme {
        ClusterInfoContent(
            popupTitle = "Cluster summary",
            clusterInfoList = mutableListOf(
                AnnotatedString("This is a cluster description")
            )
        )
    }
}