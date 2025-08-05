package com.yourname.smartpermissionanalyzer.domain.entities

data class PermissionChangeLog(
    val id: String = "",
    val packageName: String,
    val appName: String,
    val changeType: String, // ADDED, REMOVED, MODIFIED
    val permissions: List<String>,
    val timestamp: Long
)
