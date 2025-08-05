package com.yourname.smartpermissionanalyzer.domain.entities

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
@Immutable
data class PermissionChangeEntity(
    val id: String = "",
    val permissionName: String,
    val changeType: PermissionChangeType,
    val timestamp: Long,
    val riskImpact: RiskLevelEntity,
    val previousState: PermissionState? = null,
    val newState: PermissionState? = null,
    val reason: String = "",
    val userInitiated: Boolean = false
)

@Stable
@Immutable
enum class PermissionChangeType(val displayName: String, val iconCode: String) {
    ADDED("Permission Added", "add"),
    REMOVED("Permission Removed", "remove"),
    GRANTED("Permission Granted", "grant"),
    REVOKED("Permission Revoked", "revoke"),
    MODIFIED("Permission Modified", "modify"),
    UPGRADED("Permission Upgraded", "upgrade"),
    DOWNGRADED("Permission Downgraded", "downgrade")
}

@Stable
@Immutable
data class PermissionState(
    val isGranted: Boolean,
    val protectionLevel: String,
    val flags: List<String> = emptyList(),
    val grantTime: Long? = null
)
