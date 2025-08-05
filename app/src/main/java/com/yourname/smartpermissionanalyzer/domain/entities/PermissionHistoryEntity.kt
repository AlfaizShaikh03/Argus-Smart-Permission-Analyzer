package com.yourname.smartpermissionanalyzer.domain.entities

data class PermissionHistoryEntity(
    val id: String,
    val packageName: String,
    val permissionName: String,
    val action: PermissionActionEntity,
    val timestamp: Long,
    val previousState: String?,
    val newState: String,
    val userTriggered: Boolean
)
