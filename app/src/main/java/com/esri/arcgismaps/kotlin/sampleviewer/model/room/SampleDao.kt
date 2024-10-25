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

    // Offers samples as search suggestions.
    @Query(
        """SELECT rowid, name, codeFile, readMe, relevantAPIs FROM samplesDB
        WHERE LOWER(name)       LIKE LOWER('%' || :searchQuery || '%' )
        OR LOWER(readMe)        LIKE LOWER('%' || :searchQuery || '%' ) 
        OR LOWER(codeFile)      LIKE LOWER('%' || :searchQuery || '%' )
        OR LOWER(relevantAPIs)  LIKE LOWER('%' || :searchQuery || '%' )"""
    )
    fun getFilteredSamples(searchQuery: String): List<SampleEntity>

    // Offers relevant APIs as search suggestions.
    @Query(
        """SELECT rowid, name, codeFile, readMe, relevantAPIs FROM samplesDB
        WHERE LOWER(relevantAPIs) LIKE LOWER('%' || :searchQuery || '%' )"""
    )
    fun getFilteredRelevantAPIs(searchQuery: String): List<SampleEntity>
}
