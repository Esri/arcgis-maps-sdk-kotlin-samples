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

package com.esri.arcgismaps.sample.showlabelsonlayerin3d.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.arcade.ArcadeExpression
import com.arcgismaps.arcgisservices.LabelingPlacement
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.labeling.ArcadeLabelExpression
import com.arcgismaps.mapping.labeling.LabelDefinition
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.GroupLayer
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.portal.Portal
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class ShowLabelsOnLayerIn3DViewModel(app: Application) : AndroidViewModel(app) {

    val arcGISScene = ArcGISScene(
        item = PortalItem(
            portal = Portal.arcGISOnline(Portal.Connection.Anonymous),
            itemId = "850dfee7d30f4d9da0ebca34a533c169"
        )
    )

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            // Load the scene
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
            // Find the group layer and feature layer
            val groupLayer = arcGISScene.operationalLayers
                .filterIsInstance<GroupLayer>()
                .first { it.name == "Gas" }
            val featureLayer = groupLayer.layers
                .filterIsInstance<FeatureLayer>()
                .first { it.name == "Gas Main" }
            
            // Enable labels and add the label definition to the feature layer
            featureLayer.apply {
                labelsEnabled = true
                labelDefinitions.clear()
                labelDefinitions.add(labelDefinition)
            }
        }
    }


    fun makeLabelDefinition() : LabelDefinition {
        // Make and stylize the text symbol.
        val textSymbol = TextSymbol().apply {
            this.color = Color.red
            this.haloColor = Color.white
            this.haloWidth = 2.0f
            this.size = 16.0f
        }

        // Create and return a label definition using the text symbol.
        val labelDefinition = LabelDefinition(
            labelExpression = ArcadeLabelExpression(
                arcadeExpression = ArcadeExpression(
                    expression = $$"Text($feature.INSTALLATIONDATE, `DD MMM YY`)"
                )
            ),
            textSymbol = textSymbol
        ).apply {
            placement = LabelingPlacement.LineAboveAlong
            useCodedValues = true
        }
        return labelDefinition
    }
}

