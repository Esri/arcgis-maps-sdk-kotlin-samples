package com.esri.arcgismaps.kotlin.sampleviewer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A data class to hold detailed information about the [Sample]
 */
@Serializable
data class SampleMetadata(
    @Serializable(with = SampleCategorySerializer::class)
    @SerialName("category") val sampleCategory: SampleCategory,
    val description: String,
    @SerialName("formal_name") val formalName: String,
    val ignore: Boolean?,
    @SerialName("images") val imagePaths: List<String>,
    val keywords: List<String>,
    @SerialName("relevant_apis") val relevantApis: List<String>,
    @SerialName("snippets") val codePaths: List<String>,
    val title: String
)
