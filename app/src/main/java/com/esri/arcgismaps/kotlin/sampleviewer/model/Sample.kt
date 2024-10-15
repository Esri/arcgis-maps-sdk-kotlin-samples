package com.esri.arcgismaps.kotlin.sampleviewer.model

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import kotlinx.serialization.Serializable

/**
 * A data class to hold general information about the sample
 * Initialization here is needed otherwise JSON will throw missingFieldsException.
 */
@Serializable
data class Sample(
    val name: String,
    val codeFiles: List<CodeFile>,
    val mainActivity: String,
    val url: String,
    val readMe: String,
    val screenshotURL: String,
    val metadata: SampleMetadata,
    val isFavorite: Boolean = false,
    var score: Double = 0.0
) {
    companion object {
        val PREVIEW_INSTANCE = Sample(
            name = "Analyze hotspots",
            codeFiles = listOf(CodeFile("", "")),
            url = "",
            readMe = "",
            screenshotURL = "",
            metadata = SampleMetadata(
                description = "",
                formalName = "Analyze hotspots",
                ignore = false,
                imagePaths = listOf(""),
                keywords = listOf(""),
                language = "",
                redirectFrom = listOf(""),
                relevantApis = listOf(""),
                sampleCategory = SampleCategory.ANALYSIS,
                snippets = listOf(""),
                title = "Analyze hotspots"
            ),
            isFavorite = false,
            mainActivity = ""
        )
    }
}

fun Sample.startSample(context: Context) {
    val className = Class.forName(mainActivity) as Class<*>
    val sampleViewerActivity = context.getActivityOrNull()
    sampleViewerActivity?.startActivity(Intent(sampleViewerActivity, className))
}

fun Context.getActivityOrNull(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
