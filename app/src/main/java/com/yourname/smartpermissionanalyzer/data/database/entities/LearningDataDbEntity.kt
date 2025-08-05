package com.yourname.smartpermissionanalyzer.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "learning_data")
data class LearningDataDbEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_category")
    val appCategory: String,

    @ColumnInfo(name = "permissions")
    val permissions: List<String>,

    @ColumnInfo(name = "user_flagged")
    val userFlagged: Boolean,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "confidence")
    val confidence: Float = 1.0f
)
