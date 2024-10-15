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
import androidx.navigation.compose.rememberNavController
import com.esri.arcgismaps.kotlin.sampleviewer.R
import com.esri.arcgismaps.kotlin.sampleviewer.model.Category
import com.esri.arcgismaps.kotlin.sampleviewer.model.SampleCategory
import com.esri.arcgismaps.kotlin.sampleviewer.ui.theme.SampleAppTheme

/**
 *  A reusable composable class mainly used for the categories home page
 */
@Composable
fun CardItem(
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
            BackgroundImageBox(category)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                IconWithBackgroundCircle(category)
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
private fun IconWithBackgroundCircle(item: Category) {
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
private fun BackgroundImageBox(item: Category) {
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
fun PreviewCategoryCardItem() {
    SampleAppTheme {
        CardItem(
            Category(
                title = SampleCategory.ANALYSIS,
                icon = R.drawable.ic_analysis,
                backgroundImage = R.drawable.analysis_background
            ),
        ) { }
    }
}
