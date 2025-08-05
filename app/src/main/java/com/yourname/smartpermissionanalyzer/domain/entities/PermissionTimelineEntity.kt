package com.yourname.smartpermissionanalyzer.domain.entities

data class PermissionTimelineEntity(
    val id: String,
    val packageName: String,
    val appName: String,
    val permissionName: String,
    val friendlyPermissionName: String,
    val action: String,
    val timestamp: Long,
    val previousRiskLevel: String? = null,
    val newRiskLevel: String? = null,
    val category: String = "",
    val impact: String = "",
    val context: String = "",
    val userTriggered: Boolean = false
)
