package com.yourname.smartpermissionanalyzer.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exclusion_list")
data class ExclusionEntityDb(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val excludedAt: Long
)
