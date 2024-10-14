package com.esri.arcgismaps.kotlin.sampleviewer.model

/**
 * A repository interface to fetch sample information.
 */
interface SampleInfoRepository {

    fun getSamplesInCategory(sampleCategory: SampleCategory): List<Sample>

    fun getSamplesInCategory(sampleCategoryString: String): List<Sample>

    fun getSampleByName(sampleName: String): Sample

    fun getAllSamples(): List<Sample>
}