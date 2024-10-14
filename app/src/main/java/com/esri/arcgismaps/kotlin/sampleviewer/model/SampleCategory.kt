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

// TODO -- I'm not sure I fully understand why we need to serialize the enum
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