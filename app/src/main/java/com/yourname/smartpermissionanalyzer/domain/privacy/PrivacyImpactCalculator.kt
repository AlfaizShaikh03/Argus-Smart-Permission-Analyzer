package com.yourname.smartpermissionanalyzer.domain.privacy

import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity

interface PrivacyImpactCalculator {
    suspend fun calculatePrivacyScore(app: AppEntity): PrivacyScore
    suspend fun calculatePrivacyImpact(apps: List<AppEntity>): PrivacyImpactReport
}

data class PrivacyScore(
    val packageName: String,
    val appName: String,
    val privacyScore: Float, // 0.0 to 1.0, where 1.0 is best privacy
    val privacyLevel: PrivacyLevel,
    val privacyConcerns: List<String>,
    val dataCollectionRisk: String
)

enum class PrivacyLevel {
    EXCELLENT, GOOD, MODERATE, POOR, CRITICAL
}

data class PrivacyImpactReport(
    val totalAppsAnalyzed: Int,
    val averagePrivacyScore: Float,
    val criticalPrivacyApps: List<String>,
    val overallPrivacyRating: PrivacyLevel
)
