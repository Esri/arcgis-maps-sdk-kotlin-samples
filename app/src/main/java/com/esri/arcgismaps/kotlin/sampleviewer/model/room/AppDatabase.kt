package com.esri.arcgismaps.kotlin.sampleviewer.model.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.esri.arcgismaps.kotlin.sampleviewer.BuildConfig

/**
 * [AppDatabase] inherits from Room and makes an instance of [SampleDao]
 */
@Database(
    entities = [SampleEntity::class],
    version = BuildConfig.VERSION_CODE,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sampleDao(): SampleDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
