package com.esri.arcgismaps.kotlin.sampleviewer.model

import kotlinx.serialization.Serializable

/**
 * A data class to hold information about the code files for each sample
 */
@Serializable
data class CodeFile(
    val name: String,
    val code: String
)
