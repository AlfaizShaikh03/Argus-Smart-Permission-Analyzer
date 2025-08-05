package com.yourname.smartpermissionanalyzer.data.models

import android.graphics.drawable.Drawable

data class AppPermissionInfo(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable?, // Fixed: Made nullable
    val permissions: List<String>,
    val riskLevel: RiskLevel,
    val riskScore: Int,
    val appCategory: AppCategory,
    val suspiciousPermissions: List<String>,
    val suspiciousPermissionCount: Int,
    val installTime: Long,
    val lastUpdateTime: Long
)
