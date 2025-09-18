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
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.MultipointBuilder
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Camera
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.DictionaryRenderer
import com.arcgismaps.mapping.symbology.DictionarySymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStreamReader

/**
 * ViewModel for the "Apply dictionary renderer to graphics overlay" sample.
 *
 * Responsibilities:
 *  - Create an ArcGISScene and a GraphicsOverlay
 *  - Load a DictionarySymbolStyle from a portal item and apply it as a DictionaryRenderer
 *  - Parse a local XML resource containing MIL-STD-2525D messages and create graphics
 *  - Center the scene on the graphics extent and set a Camera as the initial viewpoint
 */
class ApplyDictionaryRendererToGraphicsOverlayViewModel(private val app: Application) : AndroidViewModel(app) {

    // The scene shown in the SceneView composable
    val arcGISScene = ArcGISScene(basemapStyle = BasemapStyle.ArcGISTopographic).apply {
        // conservative default viewpoint while resources load
        initialViewpoint = Viewpoint(34.0, -98.0, 1e7)
    }

    // Graphics overlay that will hold the message graphics
    val graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    // A SceneViewProxy to enable programmatic viewpoint changes
    val sceneViewProxy = SceneViewProxy()

    // Camera used to set the initial viewpoint of the scene. Exposed as a Compose-observable state so
    // the composable SceneView can react when it is set.
    var camera by mutableStateOf<Camera?>(null)
        private set

    // Used to display error messages
    val messageDialogVM = MessageDialogViewModel()

    // Keep a reference to the loaded dictionary symbol style (if needed later)
    private var loadedDictionarySymbolStyle: DictionarySymbolStyle? = null

    init {
        // Kick-off loading of the scene and style and parsing of messages
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Load the scene
                arcGISScene.load().onFailure { throwable ->
                    messageDialogVM.showMessageDialog(throwable)
                }

                // Attempt to create and apply the MIL-STD-2525D renderer
                val dictionaryRenderer = createMil2525dDictionaryRenderer()
                if (dictionaryRenderer != null) {
                    // Apply the renderer to the graphics overlay (UI object) on the main thread
                    viewModelScope.launch {
                        graphicsOverlay.renderer = dictionaryRenderer
                    }
                }

                // Parse the local XML file into message objects
                val messageGeometries = parseMessagesFromLocalXml()

                // Create graphics and add them to the graphics overlay
                if (messageGeometries.isNotEmpty()) {
                    val graphics = messageGeometries.map { (multipoint, attributes) ->
                        Graphic(geometry = multipoint, attributes = attributes)
                    }

                    // Add graphics and update viewpoint/camera on the main thread
                    viewModelScope.launch(Dispatchers.Main) {
                        graphicsOverlay.graphics.addAll(graphics)

                        try {
                            val extent = graphicsOverlay.extent
                            if (extent != null) {
                                // Create a pitched camera that looks at the extent center
                                val extentCenter = extent.center
                                val newCamera = Camera(
                                    lookAtPoint = extentCenter,
                                    distance = 15000.0,
                                    heading = 0.0,
                                    pitch = 70.0,
                                    roll = 0.0
                                )

                                // Store camera so Compose SceneView can consume it
                                camera = newCamera

                                // Also set the scene's initial viewpoint with the camera
                                arcGISScene.initialViewpoint = Viewpoint(camera = newCamera)

                                // Attempt to animate the SceneView to the camera using the proxy.
                                // If the proxy is not attached yet this call will be a no-op.
                                sceneViewProxy.setViewpointAnimated(Viewpoint(camera = newCamera))
                            }
                        } catch (ex: Exception) {
                            // Not critical — log and continue
                            Log.e("ApplyDictRendererVM", "Failed to update viewpoint/camera: ${'$'}{ex.message}")
                        }
                    }
                } else {
                    // No messages were parsed — show an informational dialog
                    messageDialogVM.showMessageDialog(
                        title = "No message graphics",
                        description = "No messages were parsed from the local XML resource."
                    )
                }

            } catch (t: Throwable) {
                // Surface any unexpected errors to the samples dialog
                messageDialogVM.showMessageDialog(t)
            }
        }
    }

    /**
     * Create and load a DictionarySymbolStyle from the MIL-STD-2525D portal item and return a DictionaryRenderer.
     * Returns null if the style could not be created/loaded.
     */
    private suspend fun createMil2525dDictionaryRenderer(): DictionaryRenderer? {
        return try {
            // PortalItem id for MIL-STD-2525D dictionary style on ArcGIS Online
            val portal = Portal.arcGISOnline(connection = Portal.Connection.Anonymous)
            val portalItem = PortalItem(portal = portal, itemId = "d815f3bdf6e6452bb8fd153b654c94ca")

            // Create the dictionary symbol style and load it
            val dictionarySymbolStyle = DictionarySymbolStyle(portalItem = portalItem)
            dictionarySymbolStyle.load().onFailure { throwable ->
                // Forward to UI dialog
                messageDialogVM.showMessageDialog(throwable)
            }

            // Choose the draw rule configuration to use ordered anchor points if available.
            try {
                dictionarySymbolStyle.configurations.firstOrNull { it.name.equals("model", ignoreCase = true) }
                    ?.let { configuration ->
                        configuration.value = "ORDERED ANCHOR POINTS"
                    }
            } catch (ignored: Exception) {
                // Best-effort; not all styles expose the same config API.
            }

            // Keep loaded style and create renderer
            loadedDictionarySymbolStyle = dictionarySymbolStyle
            DictionaryRenderer(dictionarySymbolStyle = dictionarySymbolStyle)
        } catch (t: Throwable) {
            messageDialogVM.showMessageDialog(t)
            null
        }
    }

    /**
     * Parse a local XML resource named "mil2525dmessages.xml" in the raw resources.
     * Returns a list of pairs: (Multipoint geometry, attributes map) for each message.
     */
    private fun parseMessagesFromLocalXml(): List<Pair<com.arcgismaps.geometry.Multipoint, Map<String, Any>>> {
        val results = mutableListOf<Pair<com.arcgismaps.geometry.Multipoint, Map<String, Any>>>()

        try {
            // Resolve the raw resource id by name. The sample should include a raw resource named mil2525dmessages.xml
            val resId = app.resources.getIdentifier("mil2525dmessages", "raw", app.packageName)
            if (resId == 0) {
                messageDialogVM.showMessageDialog(
                    title = "Resource not found",
                    description = "The sample could not find the local mil2525dmessages.xml resource."
                )
                return results
            }

            app.resources.openRawResource(resId).use { inputStream ->
                val reader = InputStreamReader(inputStream, Charsets.UTF_8)

                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(reader)

                var eventType = parser.eventType
                var currentMessageAttributes = mutableMapOf<String, Any>()
                var currentControlPointsText = StringBuilder()
                var currentWkidText = ""
                var insideMessage = false
                var currentElementName: String? = null

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            currentElementName = parser.name
                            if (currentElementName == "message") {
                                insideMessage = true
                                currentMessageAttributes = mutableMapOf()
                                currentControlPointsText = StringBuilder()
                                currentWkidText = ""
                            }
                        }

                        XmlPullParser.TEXT -> {
                            val text = parser.text ?: ""
                            if (insideMessage && currentElementName != null) {
                                when (currentElementName) {
                                    "_control_points" -> currentControlPointsText.append(text)
                                    "_wkid" -> currentWkidText += text.trim()
                                    else -> {
                                        val existing = currentMessageAttributes[currentElementName] ?: ""
                                        currentMessageAttributes[currentElementName] = (existing.toString() + text).trim()
                                    }
                                }
                            }
                        }

                        XmlPullParser.END_TAG -> {
                            val name = parser.name
                            if (name == "message") {
                                // Build geometry from control points and wkid
                                try {
                                    val wkid = currentWkidText.toIntOrNull() ?: SpatialReference.wgs84().wkid
                                    val spatialReference = SpatialReference(wkid = wkid)

                                    // Parse control points string like "x1,y1;x2,y2;..."
                                    val pairs = currentControlPointsText.toString().split(';').mapNotNull { pairText ->
                                        val trimmed = pairText.trim()
                                        if (trimmed.isEmpty()) return@mapNotNull null
                                        val coords = trimmed.split(',')
                                        if (coords.size < 2) return@mapNotNull null
                                        val x = coords[0].trim().toDoubleOrNull()
                                        val y = coords[1].trim().toDoubleOrNull()
                                        if (x == null || y == null) return@mapNotNull null
                                        Pair(x, y)
                                    }

                                    if (pairs.isNotEmpty()) {
                                        val multipoint = MultipointBuilder(spatialReference) {
                                            pairs.forEach { (x, y) ->
                                                addPoint(Point(x = x, y = y, spatialReference = spatialReference))
                                            }
                                        }.toGeometry()

                                        val mp = multipoint as com.arcgismaps.geometry.Multipoint

                                        // Copy attributes to immutable map
                                        val attributesCopy: Map<String, Any> = currentMessageAttributes.toMap()

                                        results.add(Pair(mp, attributesCopy))
                                    }
                                } catch (ex: Exception) {
                                    Log.e("ApplyDictRendererVM", "Error parsing message geometry: ${'$'}{ex.message}")
                                }

                                // reset message parsing state
                                insideMessage = false
                                currentElementName = null
                                currentMessageAttributes = mutableMapOf()
                                currentControlPointsText = StringBuilder()
                                currentWkidText = ""
                            }
                            currentElementName = null
                        }
                    }

                    eventType = parser.next()
                }
            }
        } catch (t: Throwable) {
            // Surface the parsing error to the UI
            messageDialogVM.showMessageDialog(t)
        }

        return results
    }
}
