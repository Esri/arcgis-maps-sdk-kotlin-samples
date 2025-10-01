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
import android.util.Xml
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
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
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
        viewModelScope.launch {
            // Load the scene first
            arcGISScene.load().onFailure { throwable ->
                messageDialogVM.showMessageDialog(throwable)
            }

            // Create and apply dictionary renderer from a web style
            val dictionaryRenderer = createMil2525dDictionaryRenderer().getOrElse {
                messageDialogVM.showMessageDialog(it)
                return@launch
            }

            // Create the point graphics from a local XML file
            val pointGraphics = makeMessageGraphics().getOrElse {
                messageDialogVM.showMessageDialog(it)
                return@launch
            }


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
    private suspend fun createMil2525dDictionaryRenderer(): Result<DictionaryRenderer> {
        // Creates a dictionary symbol style from a dictionary style portal item.
        val portalItem = PortalItem(
            portal = Portal.arcGISOnline(Portal.Connection.Anonymous),
            itemId = "d815f3bdf6e6452bb8fd153b654c94ca"
        )

        val dictionarySymbolStyle = DictionarySymbolStyle(portalItem = portalItem)

        return dictionarySymbolStyle.load().mapCatching {
            // Uses the "Ordered Anchor Points" for the symbol style draw rule.
            // Get the model configuration from the style's list of configurations.
            val modelConfiguration = dictionarySymbolStyle.configurations.firstOrNull {
                it.name.equals("model", ignoreCase = true)
            }
            if (modelConfiguration != null) {
                // Set the draw rule of the style to "ORDERED ANCHOR POINTS".
                modelConfiguration.value = "ORDERED ANCHOR POINTS"
            }
            DictionaryRenderer(dictionarySymbolStyle = dictionarySymbolStyle)
        }
    }

    /**
     * Create point graphics from a local XML file containing `MIL-STD-2525D` message data.
     */
    private fun makeMessageGraphics(): Result<List<Graphic>> {
        return runCatching {
            val xmlFile = File(provisionPath, "Mil2525DMessages.xml")
            val messageXml = xmlFile.readText()
            val messages = MessageXmlParser().parse(messageXml)

            val graphics = messages.map { message ->
                val wkid = message.wkid
                val controlPoints = message.controlPoints
                if (wkid == null || controlPoints.isEmpty()) {
                    throw IllegalArgumentException("Invalid message: missing wkid or control points")
                }
                val spatialReference = SpatialReference(wkid = wkid)
                val points = controlPoints.map { (x, y) ->
                    Point(x = x, y = y, spatialReference = spatialReference)
                }
                Graphic(geometry = Multipoint(points), attributes = message.other)
            }
            graphics
        }
    }
}

/**
 * Simple XML parser for the `MIL-STD-2525D` message XML file.
 * This is a basic implementation and does not cover all edge cases.
 */
class MessageXmlParser {
    private val messagesTag = "messages"
    private val messageTag = "message"
    private val controlPointsTag = "_control_points"
    private val wkidTag = "_wkid"

    /**
     * Parses the provided XML string and returns a list of Message objects.
     */
    fun parse(xml: String): List<Message> {
        val messages = mutableListOf<Message>()

        val parser = Xml.newPullParser().apply {
            setInput(xml.reader())
        }

        var eventType = parser.eventType
        var currentControlPoints: List<Pair<Double, Double>> = emptyList()
        var currentWkid: Int? = null
        var currentAttrs = mutableMapOf<String, String?>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (val tagName = parser.name) {
                        controlPointsTag -> {
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

                        wkidTag -> {
                            currentWkid = parser.nextText().toIntOrNull()
                        }

                        messageTag, messagesTag -> { /* ignore container tags */
                        }

                        else -> {
                            if (tagName != null) {
                                currentAttrs[tagName] = parser.nextText()
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == messageTag) {
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
