/* Copyright 2024 Esri
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

package com.esri.arcgismaps.kotlin.sampleviewer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A data class to hold detailed information about the sample
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
