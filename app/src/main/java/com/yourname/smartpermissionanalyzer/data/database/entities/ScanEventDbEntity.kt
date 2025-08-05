package com.yourname.smartpermissionanalyzer.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "scan_events")
data class ScanEventDbEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "scan_type")
    val scanType: String, // MANUAL, AUTOMATIC, TRIGGERED

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "apps_scanned")
    val appsScanned: Int,

    @ColumnInfo(name = "new_threats_found")
    val newThreatsFound: Int,

    @ColumnInfo(name = "duration")
    val duration: Long
)
