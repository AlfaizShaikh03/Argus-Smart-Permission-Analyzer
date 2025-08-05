package com.yourname.smartpermissionanalyzer.domain.entities

data class UserFeedback(
    val type: FeedbackType,
    val riskAdjustment: Int,
    val trustScore: Float,
    val timestamp: Long
)

enum class FeedbackType {
    TRUSTED, FLAGGED
}
