package com.yourname.smartpermissionanalyzer.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.yourname.smartpermissionanalyzer.data.local.entities.AppEntityDb
import com.yourname.smartpermissionanalyzer.data.local.entities.ExclusionEntityDb
import com.yourname.smartpermissionanalyzer.data.local.dao.AppEntityDao
import com.yourname.smartpermissionanalyzer.data.local.dao.ExclusionDao

@Database(
    entities = [
        AppEntityDb::class,
        ExclusionEntityDb::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appEntityDao(): AppEntityDao
    abstract fun exclusionDao(): ExclusionDao

    companion object {
        const val DATABASE_NAME = "smart_permission_analyzer_db"

        // Migration from version 1 to 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create exclusion table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `exclusion_list` (
                        `packageName` TEXT NOT NULL,
                        `appName` TEXT NOT NULL,
                        `excludedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`packageName`)
                    )
                """.trimIndent())
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
