package com.yourname.smartpermissionanalyzer.domain.entities

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
@Immutable
data class DetailedPermissionEntity(
    val name: String,
    val friendlyName: String,
    val description: String,
    val risk: RiskLevelEntity,
    val category: PermissionCategoryEntity,
    val protectionLevel: String = "Unknown",
    val isCustom: Boolean = false,
    val isDangerous: Boolean = false,
    val grantedTime: Long? = null,
    val isGranted: Boolean = false,
    val requestedSdkVersion: Int = 0,
    val usageFrequency: Int = 0,
    val lastUsedTime: Long? = null
)
