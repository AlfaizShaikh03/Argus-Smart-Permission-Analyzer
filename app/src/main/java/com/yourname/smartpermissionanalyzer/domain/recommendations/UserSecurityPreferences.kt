package com.yourname.smartpermissionanalyzer.domain.recommendations

data class UserSecurityPreferences(
    val securityLevel: SecurityLevel,
    val allowDataCollection: Boolean,
    val autoOptimize: Boolean,
    val privacyFocused: Boolean
)

enum class SecurityLevel {
    HIGH, BALANCED, PERMISSIVE
}
