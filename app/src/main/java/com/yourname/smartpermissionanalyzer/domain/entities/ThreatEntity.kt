package com.yourname.smartpermissionanalyzer.domain.entities

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
@Immutable
data class ThreatEntity(
    val id: String,
    val packageName: String,
    val appName: String,
    val threatType: ThreatType,
    val severity: RiskLevelEntity,
    val description: String,
    val detectedTime: Long,
    val isResolved: Boolean = false,
    val resolvedTime: Long? = null,
    val affectedPermissions: List<String> = emptyList(),
    val recommendedAction: String = "",
    val evidenceData: Map<String, String> = emptyMap()
)

@Stable
@Immutable
enum class ThreatType(val displayName: String, val description: String) {
    EXCESSIVE_PERMISSIONS("Excessive Permissions", "App requests more permissions than necessary"),
    SUSPICIOUS_COMBINATION("Suspicious Combination", "Dangerous combination of permissions"),
    MALWARE_SIGNATURE("Malware Signature", "App matches known malware patterns"),
    PRIVACY_VIOLATION("Privacy Violation", "App may violate user privacy"),
    SURVEILLANCE("Surveillance Capability", "App has surveillance-like permissions"),
    DATA_HARVESTING("Data Harvesting", "App may collect excessive personal data"),
    FINANCIAL_RISK("Financial Risk", "App may pose financial security risk"),
    DEVICE_CONTROL("Device Control", "App has excessive device control permissions"),
    UNKNOWN_PUBLISHER("Unknown Publisher", "App from unverified or suspicious source")
}
