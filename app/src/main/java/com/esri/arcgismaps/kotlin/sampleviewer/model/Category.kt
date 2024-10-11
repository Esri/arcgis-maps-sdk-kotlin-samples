package com.esri.arcgismaps.kotlin.sampleviewer.model

/**
 * This data class is used to hold information in each CardItem in home screen
 */
data class Category(
    val title: SampleCategory,
    val icon: Int,
    val backgroundImage: Int,
)
