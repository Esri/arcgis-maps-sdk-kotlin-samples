package com.esri.arcgismaps.kotlin.sampleviewer.model

/**
 * A repository interface to fetch sample information
 */
interface SampleInfoRepository {

    fun getSamplesIn(sampleCategory: SampleCategory): List<Sample>

    fun getSamplesIn(sampleCategoryString: String): List<Sample>

    fun getSampleByName(sampleName: String): Sample

    fun getAllSamples(): List<Sample>
}
