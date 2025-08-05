package com.yourname.smartpermissionanalyzer.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_entities")
data class AppEntityDb(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val permissions: String,
    val detailedPermissions: String,
    val riskLevel: String,
    val riskScore: Int,
    val appCategory: String,
    val suspiciousPermissions: String,
    val suspiciousPermissionCount: Int,
    val criticalPermissionCount: Int,
    val installTime: Long,
    val lastUpdateTime: Long,
    val lastUpdate: Long,
    val lastScannedTime: Long,
    val lastScan: Long,
    val isSystemApp: Boolean,
    val targetSdkVersion: Int,
    val versionName: String,
    val versionCode: Int,
    val minSdkVersion: Int,
    val permissionDensity: Float,
    val riskFactors: String,
    val permissionChanges: String,
    val appSize: Long,
    val lastUsedTime: Long,
    val isEnabled: Boolean,
    val hasInternetAccess: Boolean,
    val signatureHash: String,
    val trustScore: Float
)
