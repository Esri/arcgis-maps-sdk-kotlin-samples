package com.esri.arcgismaps.kotlin.sampleviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.esri.arcgismaps.kotlin.sampleviewer.model.DefaultSampleInfoRepository
import com.esri.arcgismaps.kotlin.sampleviewer.navigation.NavGraph
import com.esri.arcgismaps.kotlin.sampleviewer.ui.theme.SampleAppTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            setContent {

                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    lifecycleScope.launch {
                        DefaultSampleInfoRepository.load(context)
                    }
                }

                // Control color of navigation bar when user changes theme
                val isSystemInDarkMode = isSystemInDarkTheme()
                val systemUiController = rememberSystemUiController()
                SideEffect {
                    systemUiController.setNavigationBarColor(
                        color = if (isSystemInDarkMode) Color.Black else Color.White,
                        darkIcons = true
                    )
                }

                SampleAppTheme {
                    Surface(color = MaterialTheme.colorScheme.background) { NavGraph() }
                }
            }
        }
    }
}
