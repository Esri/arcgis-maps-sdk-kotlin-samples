package com.esri.arcgismaps.kotlin.sampleviewer.model.room

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


/**
 * Holds all operations that uses Room
 */
@Dao
interface SampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(samples: List<SampleEntity>)

    // In support of ranked search.
    @Query(
        """SELECT name, codeFile, readMe, relevantAPIs, MATCHINFO(samplesDB, 'pcnalx') FROM samplesDB 
        WHERE samplesDB MATCH :query"""
    )
    fun matchInfoSearch(query: String): Cursor

    // In support of search suggestions.
    @Query(
        """SELECT rowid, name, codeFile, readMe, relevantAPIs FROM samplesDB
        WHERE LOWER(name)       LIKE LOWER('%' || :searchQuery || '%' )
        OR LOWER(readMe)        LIKE LOWER('%' || :searchQuery || '%' ) 
        OR LOWER(codeFile)      LIKE LOWER('%' || :searchQuery || '%' )
        OR LOWER(relevantAPIs)  LIKE LOWER('%' || :searchQuery || '%' )"""
    )
    fun getFilteredSamples(searchQuery: String): List<SampleEntity>

    @Query(
        """SELECT rowid, name, codeFile, readMe, relevantAPIs FROM samplesDB
        WHERE LOWER(relevantAPIs) LIKE LOWER('%' || :searchQuery || '%' )"""
    )
    fun getFilteredRelevantAPIs(searchQuery: String): List<SampleEntity>
}
