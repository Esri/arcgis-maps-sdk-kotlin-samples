package com.esri.arcgismaps.kotlin.sampleviewer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A data class to hold detailed information about the [Sample]
 */
@Serializable
data class SampleMetadata(
    @SerialName(value = "formal_name") val formalName: String,
    @SerialName(value = "images") val imagePaths: List<String>,
    @SerialName(value = "relevant_apis") val relevantApis: List<String>,
    @Serializable(with = SampleCategorySerializer::class)
    @SerialName(value = "category") val sampleCategory: SampleCategory,
    @SerialName(value = "redirectFrom") val redirectFrom: List<String>? = emptyList(),
    val description: String,
    val ignore: Boolean?,
    val keywords: List<String>,
    val language: String?,
    val snippets: List<String>,
    val title: String
)
