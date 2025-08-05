package com.yourname.smartpermissionanalyzer.domain.privacy

import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyImpactCalculatorImpl @Inject constructor() : PrivacyImpactCalculator {

    override suspend fun calculatePrivacyScore(app: AppEntity): PrivacyScore {
        val concerns = mutableListOf<String>()
        var privacyScore = 1.0f

        // Analyze privacy-sensitive permissions
        val privacySensitivePermissions = app.permissions.filter { isPrivacySensitive(it) }
        if (privacySensitivePermissions.isNotEmpty()) {
            concerns.add("Accesses ${privacySensitivePermissions.size} privacy-sensitive permissions")
            privacyScore -= privacySensitivePermissions.size * 0.1f
        }

        // Check for location access
        if (app.permissions.any { it.contains("LOCATION") }) {
            concerns.add("Can access your location")
            privacyScore -= 0.15f
        }

        // Check for contacts access
        if (app.permissions.any { it.contains("CONTACTS") }) {
            concerns.add("Can access your contacts")
            privacyScore -= 0.15f
        }

        // Check for SMS access
        if (app.permissions.any { it.contains("SMS") }) {
            concerns.add("Can read/send SMS messages")
            privacyScore -= 0.2f
        }

        // Check for camera/microphone
        if (app.permissions.any { it.contains("CAMERA") || it.contains("RECORD_AUDIO") }) {
            concerns.add("Can access camera/microphone")
            privacyScore -= 0.15f
        }

        privacyScore = maxOf(0f, privacyScore)

        val level = when {
            privacyScore >= 0.9f -> PrivacyLevel.EXCELLENT
            privacyScore >= 0.7f -> PrivacyLevel.GOOD
            privacyScore >= 0.5f -> PrivacyLevel.MODERATE
            privacyScore >= 0.3f -> PrivacyLevel.POOR
            else -> PrivacyLevel.CRITICAL
        }

        val dataCollectionRisk = when (level) {
            PrivacyLevel.EXCELLENT -> "Minimal data collection risk"
            PrivacyLevel.GOOD -> "Low data collection risk"
            PrivacyLevel.MODERATE -> "Moderate data collection risk"
            PrivacyLevel.POOR -> "High data collection risk"
            PrivacyLevel.CRITICAL -> "Critical data collection risk"
        }

        return PrivacyScore(
            packageName = app.packageName,
            appName = app.appName,
            privacyScore = privacyScore,
            privacyLevel = level,
            privacyConcerns = concerns,
            dataCollectionRisk = dataCollectionRisk
        )
    }

    override suspend fun calculatePrivacyImpact(apps: List<AppEntity>): PrivacyImpactReport {
        val privacyScores = apps.map { calculatePrivacyScore(it) }
        val averageScore = privacyScores.map { it.privacyScore }.average().toFloat()
        val criticalApps = privacyScores.filter { it.privacyLevel == PrivacyLevel.CRITICAL }
            .map { it.appName }

        val overallRating = when {
            averageScore >= 0.8f -> PrivacyLevel.EXCELLENT
            averageScore >= 0.6f -> PrivacyLevel.GOOD
            averageScore >= 0.4f -> PrivacyLevel.MODERATE
            averageScore >= 0.2f -> PrivacyLevel.POOR
            else -> PrivacyLevel.CRITICAL
        }

        return PrivacyImpactReport(
            totalAppsAnalyzed = apps.size,
            averagePrivacyScore = averageScore,
            criticalPrivacyApps = criticalApps,
            overallPrivacyRating = overallRating
        )
    }

    private fun isPrivacySensitive(permission: String): Boolean {
        val privacySensitivePermissions = listOf(
            "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "ACCESS_BACKGROUND_LOCATION",
            "READ_CONTACTS", "WRITE_CONTACTS", "READ_CALL_LOG", "WRITE_CALL_LOG",
            "READ_SMS", "SEND_SMS", "READ_CALENDAR", "WRITE_CALENDAR",
            "CAMERA", "RECORD_AUDIO", "READ_PHONE_STATE", "BODY_SENSORS"
        )

        return privacySensitivePermissions.any { permission.contains(it, ignoreCase = true) }
    }
}
