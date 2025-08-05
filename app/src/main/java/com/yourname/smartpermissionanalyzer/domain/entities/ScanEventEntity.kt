package com.yourname.smartpermissionanalyzer.domain.entities

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
@Immutable
data class ScanEventEntity(
    val id: Long = 0,
    val scanType: ScanTypeEntity,
    val timestamp: Long,
    val appsScanned: Int,
    val newThreatsFound: Int,
    val duration: Long,
    val totalRiskScore: Int = 0,
    val highRiskApps: Int = 0,
    val criticalRiskApps: Int = 0,
    val newPermissionsFound: Int = 0,
    val scanTrigger: String = "",
    val scanResult: ScanResult = ScanResult.SUCCESS,
    val errorMessage: String? = null,
    val scanDetails: Map<String, String> = emptyMap()
)

@Stable
@Immutable
enum class ScanTypeEntity(val displayName: String, val description: String) {
    MANUAL("Manual Scan", "User-initiated security scan"),
    AUTOMATIC("Automatic Scan", "Scheduled background scan"),
    TRIGGERED("Triggered Scan", "Event-triggered security scan"),
    DEEP("Deep Scan", "Comprehensive security analysis"),
    QUICK("Quick Scan", "Fast security check"),
    UPDATE_SCAN("Update Scan", "Scan after app updates"),
    INSTALL_SCAN("Install Scan", "Scan after new app installation")
}

@Stable
@Immutable
enum class ScanResult(val displayName: String) {
    SUCCESS("Scan Completed Successfully"),
    PARTIAL("Scan Completed with Warnings"),
    FAILED("Scan Failed"),
    CANCELLED("Scan Cancelled"),
    TIMEOUT("Scan Timed Out")
}
