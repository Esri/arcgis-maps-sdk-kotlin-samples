/* Copyright 2024 Esri
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

package com.esri.arcgismaps.kotlin.sampleviewer.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.kotlin.sampleviewer.R
import com.esri.arcgismaps.kotlin.sampleviewer.model.Category
import com.esri.arcgismaps.kotlin.sampleviewer.model.SampleCategory
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

/**
 *  A composable used in the HomeCategoryScreen to display the category cards.
 */
@Composable
fun CategoryCard(
    category: Category,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .size(width = 175.dp, height = 175.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box {
            CategoryBackground(category)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                CategoryIconBackground(category)
                Text(
                    modifier = Modifier.wrapContentSize(Alignment.Center),
                    text = category.title.text,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun CategoryIconBackground(item: Category) {
    Box(
        Modifier
            .background(
                color = Color.Black.copy(alpha = 0.8f),
                shape = CircleShape
            )
            .padding(8.dp)
    ) {
        Icon(
            modifier = Modifier
                .size(30.dp),
            painter = painterResource(item.icon),
            contentDescription = "Category Icon",
            tint = Color.White
        )
    }
}

@Composable
private fun CategoryBackground(item: Category) {
    Image(
        modifier = Modifier.fillMaxSize(),
        painter = painterResource(item.backgroundImage),
        contentDescription = item.title.text,
        contentScale = ContentScale.Crop
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
    )
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewCategoryCard() {
    SampleAppTheme {
        CategoryCard(
            Category(
                title = SampleCategory.ANALYSIS,
                icon = R.drawable.ic_analysis,
                backgroundImage = R.drawable.analysis_background
            ),
        ) { }
    }
}
