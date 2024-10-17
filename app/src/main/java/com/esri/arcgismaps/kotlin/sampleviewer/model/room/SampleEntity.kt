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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.esri.arcgismaps.kotlin.sampleviewer.model.CodeFile

/**
 * Define the structure of the Database records. The first column and the primary key is an integer
 * rowid, which is a requirement of FTS4 tables. The other columns are the fields of the Sample
 * class.
 */
@Fts4
@Entity(tableName = "samplesDB")
@TypeConverters(Converters::class)
data class SampleEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val id: Int = 0,
    @ColumnInfo(name = "name") val sampleName: String,
    @ColumnInfo(name = "codeFile") val sampleCodeFile: List<CodeFile>,
    @ColumnInfo(name = "readMe") val sampleReadMe: String,
    @ColumnInfo(name = "relevantAPIs") val sampleRelevantApi: List<String>,
)
