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
