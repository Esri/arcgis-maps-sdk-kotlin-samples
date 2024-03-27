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

package com.esri.arcgismaps.sample.displaypointsusingclusteringfeaturereduction.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    clusterInfoList: MutableList<AnnotatedString>,
    onDismiss: () -> Unit = { },
) {
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 30.dp, vertical = 12.dp)
                    .weight(6f),
                text = popupTitle,
                style = SampleTypography.displaySmall
            )

            IconButton(
                modifier = Modifier.weight(1f),
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close button"
                )
            }
        }

        LazyColumn {
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
