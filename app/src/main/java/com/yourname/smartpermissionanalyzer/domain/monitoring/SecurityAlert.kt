package com.yourname.smartpermissionanalyzer.domain.monitoring

data class SecurityAlert(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val appPackageName: String?,
    val timestamp: Long,
    val severity: String
)
