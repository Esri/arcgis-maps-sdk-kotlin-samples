package com.esri.arcgismaps.kotlin.sampleviewer.model

import kotlinx.serialization.Serializable

/**
 * Represents a single code file in a sample.
 */
@Serializable
data class CodeFile(
    val name: String,
    val code: String
)
