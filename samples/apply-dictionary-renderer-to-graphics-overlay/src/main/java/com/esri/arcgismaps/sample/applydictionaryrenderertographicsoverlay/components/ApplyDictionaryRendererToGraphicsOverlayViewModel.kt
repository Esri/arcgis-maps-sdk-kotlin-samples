/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.applydictionaryrenderertographicsoverlay.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.symbology.DictionaryRenderer
import com.arcgismaps.mapping.symbology.DictionarySymbolStyle
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.applydictionaryrenderertographicsoverlay.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File

/**
 * ViewModel for the "Apply dictionary renderer to graphics overlay" sample.
 */
class ApplyDictionaryRendererToGraphicsOverlayViewModel(private val app: Application) : AndroidViewModel(app) {

    // The scene shown in the SceneView composable
    val arcGISScene = ArcGISScene(basemapStyle = BasemapStyle.ArcGISTopographic)

    // Graphics overlay that will hold the message graphics
    val graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    // SceneViewProxy to enable programmatic viewpoint changes
    val sceneViewProxy = SceneViewProxy()

    // Used to display error messages
    val messageDialogVM = MessageDialogViewModel()

    // Provision path where downloaded sample assets will be placed by the downloader activity
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() + File.separator + app.getString(R.string.apply_dictionary_renderer_to_graphics_overlay_app_name)
    }

    init {
        viewModelScope.launch() {
            // Load the scene first
            arcGISScene.load().onFailure { throwable ->
                messageDialogVM.showMessageDialog(throwable)
            }

            // Create and apply dictionary renderer from a web style
            val dictionaryRendererDeferred = viewModelScope.async(Dispatchers.IO) {
                createMil2525dDictionaryRenderer()
            }

            // Create the point graphics in separate coroutine from a local XML file
            val graphicsDeferred = viewModelScope.async(Dispatchers.IO) {
                makeMessageGraphics()
            }

            val dictionaryRenderer = dictionaryRendererDeferred.await()
            val pointGraphics = graphicsDeferred.await()

            // Set the graphics overlay to use the dictionary renderer and add graphics
            graphicsOverlay.apply {
                renderer = dictionaryRenderer
                graphics.addAll(pointGraphics)
            }

            // Sets the camera to look a the graphics in the graphics overlay
            graphicsOverlay.extent?.let { extent ->
                sceneViewProxy.setViewpointCamera(
                    camera = Camera(
                        lookAtPoint = extent.center,
                        distance = 15000.0,
                        heading = 0.0,
                        pitch = 70.0,
                        roll = 0.0
                    )
                )
            }
        }
    }

    /**
     * Create and load a [DictionarySymbolStyle] from a web style and use it to create a [DictionaryRenderer].
     */
    private suspend fun createMil2525dDictionaryRenderer(): DictionaryRenderer? {
        // Creates a dictionary symbol style from a dictionary style portal item.
        val portalItem = PortalItem(
            portal = Portal.arcGISOnline(Portal.Connection.Anonymous),
            itemId = "d815f3bdf6e6452bb8fd153b654c94ca"
        )

        val dictionarySymbolStyle = DictionarySymbolStyle(portalItem = portalItem)

        dictionarySymbolStyle.load().onFailure { throwable ->
            messageDialogVM.showMessageDialog(throwable)
            return null
        }

        // Uses the "Ordered Anchor Points" for the symbol style draw rule.
        dictionarySymbolStyle.configurations.firstOrNull { it.name.equals("model", ignoreCase = true) }
            ?.let { configuration -> configuration.value = "ORDERED ANCHOR POINTS" }

        return DictionaryRenderer(dictionarySymbolStyle = dictionarySymbolStyle)
    }

    /**
     * Create point graphics from a local XML file containing `MIL-2525-D` message data.
     */
    private fun makeMessageGraphics(): List<Graphic> {
        val xmlFile = File(provisionPath, "mil2525dmessages.xml")
        val messageXml = xmlFile.readText()
        val messages = MessageXmlParser().parse(messageXml)

        return messages.mapNotNull { message ->
            val wkid = message.wkid
            val controlPoints = message.controlPoints
            if (wkid == null || controlPoints.isEmpty()) {
                // Optionally log or handle the error here
                null
            } else {
                try {
                    val spatialReference = SpatialReference(wkid = wkid)
                    val points = controlPoints.map { (x, y) ->
                        Point(x = x, y = y, spatialReference = spatialReference)
                    }
                    Graphic(geometry = Multipoint(points), attributes = message.other)
                } catch (ex: Exception) {
                    messageDialogVM.showMessageDialog(
                        title = "Unable to create graphic from XML file.", description = ex.message.toString()
                    )
                    null
                }
            }
        }
    }
}

private const val TAG_MESSAGES = "messages"
private const val TAG_MESSAGE = "message"
private const val TAG_CONTROL_POINTS = "_control_points"
private const val TAG_WKID = "_wkid"

/**
 * Simple XML parser for the `MIL-STD-2525D` message XML file.
 * This is a basic implementation and does not cover all edge cases.
 */
class MessageXmlParser {

    /**
     * Parses the provided XML string and returns a list of Message objects.
     */
    fun parse(xml: String): List<Message> {
        val messages = mutableListOf<Message>()

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())

        var eventType = parser.eventType
        var currentControlPoints: List<Pair<Double, Double>> = emptyList()
        var currentWkid: Int? = null
        var currentAttrs = mutableMapOf<String, String?>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (val tagName = parser.name) {
                        TAG_CONTROL_POINTS -> {
                            val controlPointsText = parser.nextText()
                            currentControlPoints = controlPointsText.split(";")
                                .mapNotNull {
                                    val coords = it.split(",")
                                    if (coords.size == 2) {
                                        val x = coords[0].toDoubleOrNull()
                                        val y = coords[1].toDoubleOrNull()
                                        if (x != null && y != null) Pair(x, y) else null
                                    } else null
                                }
                        }

                        TAG_WKID -> {
                            currentWkid = parser.nextText().toIntOrNull()
                        }

                        TAG_MESSAGE, TAG_MESSAGES -> { /* ignore container tags */
                        }

                        else -> {
                            if (tagName != null) {
                                currentAttrs[tagName] = parser.nextText()
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == TAG_MESSAGE) {
                        messages.add(
                            Message(
                                controlPoints = currentControlPoints,
                                wkid = currentWkid,
                                other = currentAttrs
                            )
                        )
                        currentControlPoints = emptyList()
                        currentWkid = null
                        currentAttrs = mutableMapOf()
                    }
                }
            }

            eventType = parser.next()
        }

        return messages
    }
}

data class Message(
    val controlPoints: List<Pair<Double, Double>>,
    val wkid: Int?,
    val other: Map<String, String?> = emptyMap()
)
