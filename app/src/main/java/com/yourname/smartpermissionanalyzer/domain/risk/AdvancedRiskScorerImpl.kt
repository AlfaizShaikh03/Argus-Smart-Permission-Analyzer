package com.yourname.smartpermissionanalyzer.domain.risk

import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdvancedRiskScorerImpl @Inject constructor() : AdvancedRiskScorer {

    override suspend fun calculateAdvancedRiskScore(app: AppEntity): RiskAnalysis {
        val riskFactors = mutableListOf<RiskFactor>()
        var riskScore = app.riskScore

        // Analyze permission risks
        val dangerousPermissions = app.permissions.filter { isDangerousPermission(it) }
        if (dangerousPermissions.isNotEmpty()) {
            riskFactors.add(
                RiskFactor(
                    factor = "Dangerous Permissions",
                    severity = "HIGH",
                    impact = dangerousPermissions.size * 5,
                    description = "App requests ${dangerousPermissions.size} dangerous permissions"
                )
            )
        }

        // Check suspicious permissions
        if (app.suspiciousPermissions.isNotEmpty()) {
            riskFactors.add(
                RiskFactor(
                    factor = "Suspicious Permissions",
                    severity = "MEDIUM",
                    impact = app.suspiciousPermissions.size * 3,
                    description = "App has ${app.suspiciousPermissions.size} suspicious permission patterns"
                )
            )
        }

        // System app check
        if (app.isSystemApp) {
            riskFactors.add(
                RiskFactor(
                    factor = "System App",
                    severity = "LOW",
                    impact = -10,
                    description = "System app - generally more trusted"
                )
            )
            riskScore -= 10
        }

        // Trust score adjustment
        val trustAdjustment = ((1f - app.trustScore) * 20).toInt()
        if (trustAdjustment > 0) {
            riskFactors.add(
                RiskFactor(
                    factor = "Low Trust Score",
                    severity = "MEDIUM",
                    impact = trustAdjustment,
                    description = "App has low community trust score"
                )
            )
        }

        // Calculate final risk score
        val additionalRisk = riskFactors.sumOf { maxOf(0, it.impact) }
        val finalScore = minOf(100, maxOf(0, riskScore + additionalRisk))

        val riskLevel = when {
            finalScore >= 85 -> RiskLevelEntity.CRITICAL
            finalScore >= 70 -> RiskLevelEntity.HIGH
            finalScore >= 50 -> RiskLevelEntity.MEDIUM
            finalScore >= 25 -> RiskLevelEntity.LOW
            else -> RiskLevelEntity.MINIMAL
        }

        val recommendation = when (riskLevel) {
            RiskLevelEntity.CRITICAL -> "Immediate action required - consider uninstalling"
            RiskLevelEntity.HIGH -> "Review app permissions and consider restrictions"
            RiskLevelEntity.MEDIUM -> "Monitor app behavior and review permissions"
            RiskLevelEntity.LOW -> "Generally safe but periodic review recommended"
            RiskLevelEntity.MINIMAL -> "App appears safe with minimal risk"
            else -> "Unable to assess risk level"
        }

        return RiskAnalysis(
            packageName = app.packageName,
            appName = app.appName,
            finalRiskScore = finalScore,
            riskLevel = riskLevel,
            riskFactors = riskFactors,
            trustScore = app.trustScore,
            recommendation = recommendation
        )
    }

    override suspend fun analyzeRiskTrends(apps: List<AppEntity>): RiskTrendAnalysis {
        val riskAnalyses = apps.map { calculateAdvancedRiskScore(it) }
        val averageRisk = riskAnalyses.map { it.finalRiskScore }.average().toFloat()
        val highRiskCount = riskAnalyses.count { it.finalRiskScore >= 70 }

        val riskDistribution = riskAnalyses.groupingBy { it.riskLevel }.eachCount()
        val topRiskFactors = riskAnalyses.flatMap { it.riskFactors }
            .groupingBy { it.factor }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }

        return RiskTrendAnalysis(
            totalAppsAnalyzed = apps.size,
            averageRiskScore = averageRisk,
            highRiskAppCount = highRiskCount,
            riskDistribution = riskDistribution,
            topRiskFactors = topRiskFactors
        )
    }

    private fun isDangerousPermission(permission: String): Boolean {
        val dangerousPermissions = listOf(
            "CAMERA", "RECORD_AUDIO", "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION",
            "READ_CONTACTS", "WRITE_CONTACTS", "READ_SMS", "SEND_SMS", "READ_CALL_LOG",
            "WRITE_CALL_LOG", "CALL_PHONE", "READ_PHONE_STATE", "READ_CALENDAR",
            "WRITE_CALENDAR", "BODY_SENSORS", "ACCESS_BACKGROUND_LOCATION"
        )

        return dangerousPermissions.any { permission.contains(it, ignoreCase = true) }
    }
}
