package com.yourname.smartpermissionanalyzer.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "apps")
data class AppDatabaseEntity(
    @PrimaryKey
    val packageName: String,

    @ColumnInfo(name = "appName")
    val appName: String,

    @ColumnInfo(name = "permissions")
    val permissions: List<String>,

    @ColumnInfo(name = "riskScore")
    val riskScore: Int,

    @ColumnInfo(name = "riskLevel")
    val riskLevel: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "suspiciousPermissions")
    val suspiciousPermissions: List<String>,

    @ColumnInfo(name = "suspiciousPermissionCount")
    val suspiciousPermissionCount: Int,

    @ColumnInfo(name = "criticalPermissionCount")
    val criticalPermissionCount: Int,

    @ColumnInfo(name = "installTime")
    val installTime: Long,

    @ColumnInfo(name = "lastUpdate")
    val lastUpdate: Long,

    @ColumnInfo(name = "lastScan")
    val lastScan: Long,

    @ColumnInfo(name = "isSystemApp")
    val isSystemApp: Boolean,

    @ColumnInfo(name = "versionCode")
    val versionCode: Int,

    @ColumnInfo(name = "versionName")
    val versionName: String
)
