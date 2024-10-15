package com.esri.arcgismaps.kotlin.sampleviewer.model

import com.esri.arcgismaps.kotlin.sampleviewer.R

/**
 * This data class is used to hold information in each CardItem in home screen
 */
data class Category(
    val title: SampleCategory,
    val icon: Int,
    val backgroundImage: Int
) {
    companion object {
        val SAMPLE_CATEGORIES = listOf(
            Category(
                SampleCategory.ANALYSIS,
                R.drawable.ic_analysis,
                R.drawable.analysis_background,
            ),
            Category(
                SampleCategory.AUGMENTED_REALITY,
                R.drawable.ic_augmented_reality,
                R.drawable.augmented_reality_background,
            ),
            Category(
                SampleCategory.CLOUD_AND_PORTAL,
                R.drawable.ic_cloud,
                R.drawable.cloud_background,
            ),
            Category(
                SampleCategory.EDIT_AND_MANAGE_DATA,
                R.drawable.ic_manage_data,
                R.drawable.manage_data_background,
            ),
            Category(
                SampleCategory.LAYERS,
                R.drawable.ic_layers,
                R.drawable.layers_background,
            ),
            Category(
                SampleCategory.MAPS,
                R.drawable.ic_map,
                R.drawable.maps_and_scenes_background,
            ),
            Category(
                SampleCategory.ROUTING_AND_LOGISTICS,
                R.drawable.ic_routing_and_logistics,
                R.drawable.routing_and_logistics_background,
            ),
            Category(
                SampleCategory.SCENES,
                R.drawable.ic_scenes,
                R.drawable.scenes_background,
            ),
            Category(
                SampleCategory.SEARCH_AND_QUERY,
                R.drawable.ic_search_and_query,
                R.drawable.search_and_query_background,
            ),
            Category(
                SampleCategory.UTILITY_NETWORKS,
                R.drawable.ic_utility,
                R.drawable.utility_background,
            ),
            Category(
                SampleCategory.VISUALIZATION,
                R.drawable.ic_visualization,
                R.drawable.visualization_background,
            ),
            Category(
                SampleCategory.FAVORITES,
                R.drawable.ic_favorite_selected,
                R.drawable.maps_and_scenes_background,
            ),
        )
    }
}
