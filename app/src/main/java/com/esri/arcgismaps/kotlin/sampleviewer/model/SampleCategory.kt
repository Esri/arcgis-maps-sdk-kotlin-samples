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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Holds the enum of each Sample Category.
 */
@Serializable
enum class SampleCategory(val text: String) {
    ANALYSIS("Analysis"),
    AUGMENTED_REALITY("Augmented Reality"),
    CLOUD_AND_PORTAL("Cloud and Portal"),
    LAYERS("Layers"),
    EDIT_AND_MANAGE_DATA("Edit and Manage Data"),
    MAPS("Maps"),
    SCENES("Scenes"),
    ROUTING_AND_LOGISTICS("Routing and Logistics"),
    UTILITY_NETWORKS("Utility Networks"),
    SEARCH_AND_QUERY("Search and Query"),
    VISUALIZATION("Visualization"),
    FAVORITES("Favorites");

    /**
     * Return string of category enum.
     */
    override fun toString(): String {
        return text
    }

    /**
     * Return the enum of the category string.
     */
    companion object {
        fun toEnum(categoryString: String): SampleCategory {
            return entries.firstOrNull {it.text == categoryString}!!
        }
    }
}

object SampleCategorySerializer : KSerializer<SampleCategory> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SampleCategory", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): SampleCategory {
        val value = decoder.decodeString()
        return SampleCategory.toEnum(value)
    }

    override fun serialize(encoder: Encoder, value: SampleCategory) {
        encoder.encodeString(value.text)
    }
}