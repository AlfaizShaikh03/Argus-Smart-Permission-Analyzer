package com.yourname.smartpermissionanalyzer.domain.entities

data class UserFeedbackEntity(
    val id: String,
    val packageName: String,
    val appName: String,
    val isTrusted: Boolean,
    val timestamp: Long,
    val permissions: List<String>,
    val riskScore: Int,
    val userComment: String? = null
)
