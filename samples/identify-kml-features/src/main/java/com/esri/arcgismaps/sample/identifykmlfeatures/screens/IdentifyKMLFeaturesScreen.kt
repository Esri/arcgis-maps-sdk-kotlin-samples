/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.identifykmlfeatures.screens

import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.Ref
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.toolkit.geoviewcompose.GeoViewScope
import com.arcgismaps.toolkit.geoviewcompose.LeaderPosition
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.identifykmlfeatures.R
import com.esri.arcgismaps.sample.identifykmlfeatures.components.IdentifyKMLFeaturesViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun IdentifyKMLFeaturesScreen(
    mapViewModel: IdentifyKMLFeaturesViewModel = viewModel()
) {
    val pointAndHtml = mapViewModel.pointAndHtml.collectAsState().value
    val point = pointAndHtml?.first
    val htmlText = pointAndHtml?.second
    val offset = Offset.Zero
    val tapLocationGraphicsOverlay = mapViewModel.tapLocationGraphicsOverlay.collectAsState().value
    var rotateOffsetWithGeoView by rememberSaveable { mutableStateOf(false) }
    var calloutVisibility by rememberSaveable { mutableStateOf(true) }
    // animate to a visible transition state
    val calloutVisibleState = remember { MutableTransitionState(false) }.apply {
        targetState = point != null && calloutVisibility
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.identify_kml_features_app_name)) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    graphicsOverlays = listOf(tapLocationGraphicsOverlay),
                    onSingleTapConfirmed = mapViewModel::onSingleTapConfirmed,
                    content = {
                        GeoViewScopeContent(
                            mapPoint = point,
                            htmlText = htmlText,
                            calloutVisibleState = calloutVisibleState,
                            rotateOffsetWithGeoView = rotateOffsetWithGeoView,
                            offset = offset
                        )
                    }
                )
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

/**
 * Displays the callout at the tapped location on the map or scene.
 *
 * This function is responsible for managing callout at the coordinates of the [mapPoint].
 * It uses animated visibility to show or hide the callout based on the state provided.
 */
@Composable
fun GeoViewScope.GeoViewScopeContent(
    mapPoint: Point?,
    htmlText: String?,
    calloutVisibleState: MutableTransitionState<Boolean>,
    rotateOffsetWithGeoView: Boolean,
    offset: Offset
) {
    val lastMapPoint = remember { Ref<Point>() }
    lastMapPoint.value = mapPoint ?: lastMapPoint.value

    AnimatedVisibility(
        calloutVisibleState,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        lastMapPoint.value?.let {
            Callout(
                modifier = Modifier.wrapContentSize(),
                location = it,
                leaderPosition = LeaderPosition.Automatic,
                rotateOffsetWithGeoView = rotateOffsetWithGeoView,
                offset = offset
            ) {
                Column(Modifier.padding(4.dp)) {
                    HtmlText(
                        html = htmlText ?: "",
                        htmlFlag = HtmlCompat.FROM_HTML_MODE_COMPACT,
                        textColor = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

/**
 * AndroidView wrapper for the view-based [TextView] which is able to display a styled spannable [html].
 * Currently, Compose does not provide a tool to buildAnnotatedString for HTML styled spannable text.
 */
@Composable
fun HtmlText(modifier: Modifier = Modifier, html: String, htmlFlag: Int, textColor: Color) {
    AndroidView(
        modifier = modifier,
        factory = { context -> TextView(context) },
        update = {
            it.text = HtmlCompat.fromHtml(html, htmlFlag)
            it.setTextColor(textColor.hashCode())
        }
    )
}

