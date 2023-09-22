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

import android.app.Application
import android.graphics.Typeface
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.popup.FieldsPopupElement
import com.arcgismaps.mapping.popup.TextPopupElement
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.portal.Portal
import com.esri.arcgismaps.sample.displaypointsusingclusteringfeaturereduction.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val application: Application,
    private val sampleCoroutineScope: CoroutineScope
) : AndroidViewModel(application) {
    // set the MapView mutable stateflow
    val mapViewState = MutableStateFlow(MapViewState())

    // create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    // Flag indicating whether feature reduction is enabled or not
    val isFeatureReductionEnabled = mutableStateOf(true)

    // Flag to show or dismiss the LoadingDialog
    val showLoadingDialog = mutableStateOf(false)

    // Flag to show or dismiss the bottom sheet
    val showClusterSummaryBottomSheet = mutableStateOf(false)

    // Initialize clusterInfoList which holds the popup details
    val clusterInfoList = mutableStateListOf<AnnotatedString>()

    // the title of the popup result
    val popupTitle = mutableStateOf("")

    init {
        // show loading dialog to indicate that the map is loading
        showLoadingDialog.value = true
        // load the portal and create a map from the portal item
        val portalItem = PortalItem(
            Portal(application.getString(R.string.portal_url)),
            "8916d50c44c746c1aafae001552bad23"
        )

        // set the map to be displayed in the layout's MapView
        mapViewState.value.arcGISMap = ArcGISMap(portalItem)

        sampleCoroutineScope.launch {
            mapViewState.value.arcGISMap.load().onSuccess {
                showLoadingDialog.value = false
            }
        }
    }

    /**
    `* Toggle the FeatureLayer's featureReduction property
     */
    fun toggleFeatureReduction() {
        val map = mapViewState.value.arcGISMap
        isFeatureReductionEnabled.value = !isFeatureReductionEnabled.value
        if (map.loadStatus.value == LoadStatus.Loaded) {
            map.operationalLayers.forEach { layer ->
                when (layer) {
                    is FeatureLayer -> {
                        layer.featureReduction?.isEnabled = isFeatureReductionEnabled.value
                    }

                    else -> {}
                }
            }
        }
    }

    /**
     * Identify the feature layer results and display the resulting popup element information
     */
    fun handleIdentifyResult(result: Result<List<IdentifyLayerResult>>) {
        sampleCoroutineScope.launch {
            result.onSuccess { identifyResultList ->
                // initialize the string for each tap event resulting in a new identifyResultList
                clusterInfoList.clear()
                popupTitle.value = ""
                identifyResultList.forEach { identifyLayerResult ->
                    val popups = identifyLayerResult.popups
                    popups.forEach { popup ->
                        // set the popup title
                        popupTitle.value = popup.title
                        // show the bottom sheet for the popup content
                        showClusterSummaryBottomSheet.value = true
                        popup.evaluateExpressions().onSuccess {
                            popup.evaluatedElements.forEach { popupElement ->
                                when (popupElement) {
                                    is FieldsPopupElement -> {
                                        popupElement.fields.forEach { popupField ->
                                            // convert popupField.label embedded with html tags using HtmlCompat.fromHtml
                                            clusterInfoList.add(
                                                HtmlCompat.fromHtml(
                                                    popupField.label,
                                                    HtmlCompat.FROM_HTML_MODE_COMPACT
                                                ).toAnnotatedString()
                                            )
                                        }
                                    }

                                    is TextPopupElement -> {
                                        // convert popupElement.text message embedded with html tags using HtmlCompat.fromHtml
                                        clusterInfoList.add(
                                            HtmlCompat.fromHtml(
                                                popupElement.text,
                                                HtmlCompat.FROM_HTML_MODE_COMPACT
                                            ).toAnnotatedString()
                                        )
                                    }

                                    else -> {
                                        clusterInfoList.add(
                                            HtmlCompat.fromHtml(
                                                "Unsupported popup element: ${popupElement.javaClass.name}",
                                                HtmlCompat.FROM_HTML_MODE_COMPACT
                                            ).toAnnotatedString()
                                        )
                                    }
                                }
                            }
                        }.onFailure { error ->
                            messageDialogVM.showMessageDialog(
                                title = "Error in evaluating popup expression: ${error.message.toString()}",
                                description = error.cause.toString()
                            )
                        }
                    }
                }
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Error in identify: ${error.message.toString()}",
                    description = error.cause.toString()
                )
            }
        }
    }

    /**
     * Helper function which converts a [Spanned] into an [AnnotatedString] trying to keep as much formatting as possible.
     * [AnnotatedString] is supported in compose via the [Text] composable
     *
     * Currently supports `bold`, `italic`, `underline` and `color`.
     * More info can be found at:
     * https://stackoverflow.com/questions/66494838/android-compose-how-to-use-html-tags-in-a-text-view and
     * https://medium.com/@kevinskrei/annotated-text-in-jetpack-compose-8dc596ed62d
     */
    private fun Spanned.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
        val spanned = this@toAnnotatedString
        append(spanned.toString())
        getSpans(0, spanned.length, Any::class.java).forEach { span ->
            val start = getSpanStart(span)
            val end = getSpanEnd(span)
            when (span) {
                is StyleSpan -> when (span.style) {
                    Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    Typeface.BOLD_ITALIC -> addStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        ), start, end
                    )
                }

                is UnderlineSpan -> addStyle(
                    SpanStyle(textDecoration = TextDecoration.Underline),
                    start,
                    end
                )

                is ForegroundColorSpan -> addStyle(
                    SpanStyle(color = Color(span.foregroundColor)),
                    start,
                    end
                )
            }
        }
    }
}

/**
 * Class that represents the MapView state
 */
data class MapViewState(
    var arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight),
    val viewpoint: Viewpoint = Viewpoint(39.8, -98.6, 10e7)
)

