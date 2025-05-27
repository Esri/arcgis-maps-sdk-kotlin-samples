package com.esri.arcgismaps.sample.displaymapfromportalitem.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

/**
 * ViewModel for displaying a map from a portal item.
 */
class DisplayMapFromPortalItemViewModel(app: Application) : AndroidViewModel(app) {
    /**
     * Data class representing a portal item map option.
     */
    data class PortalItemMap(
        val title: String,
        val itemId: String
    ) {
        val portalItem: PortalItem by lazy {
            PortalItem(
                portal = Portal.arcGISOnline(Portal.Connection.Anonymous),
                itemId = itemId
            )
        }
    }

    // List of available portal item map options (matching Swift sample)
    val mapOptions = listOf(
        PortalItemMap(
            title = "Terrestrial Ecosystems of the World",
            itemId = "5be0bc3ee36c4e058f7b3cebc21c74e6"
        ),
        PortalItemMap(
            title = "Recent Hurricanes, Cyclones and Typhoons",
            itemId = "064f2e898b094a17b84e4a4cd5e5f549"
        ),
        PortalItemMap(
            title = "Geology of United States",
            itemId = "92ad152b9da94dee89b9e387dfe21acd"
        )
    )

    // The currently selected portal item map option
    var currentMapOption by mutableStateOf(mapOptions.first())
        private set

    // The ArcGISMap to display
    var arcGISMap by mutableStateOf(
        ArcGISMap(item = mapOptions.first().portalItem)
    )
        private set

    // Message dialog for error handling
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Updates the map to display the selected portal item map option.
     */
    fun selectMapOption(option: PortalItemMap) {
        if (option != currentMapOption) {
            currentMapOption = option
            arcGISMap = ArcGISMap(item = option.portalItem)
            viewModelScope.launch {
                arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
            }
        }
    }
}
