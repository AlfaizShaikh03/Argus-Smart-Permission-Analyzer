package com.yourname.smartpermissionanalyzer.domain.optimization

import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityOptimizerImpl @Inject constructor() : SecurityOptimizer {

    override suspend fun optimizeAppSecurity(app: AppEntity): OptimizationResult {
        val recommendations = mutableListOf<String>()
        var potentialRiskReduction = 0

        // Analyze dangerous permissions
        val dangerousPermissions = app.permissions.filter { isDangerousPermission(it) }
        if (dangerousPermissions.isNotEmpty()) {
            recommendations.add("Revoke ${dangerousPermissions.size} dangerous permissions")
            potentialRiskReduction += dangerousPermissions.size * 5
        }

        // Check suspicious permissions
        if (app.suspiciousPermissions.isNotEmpty()) {
            recommendations.add("Review ${app.suspiciousPermissions.size} suspicious permissions")
            potentialRiskReduction += app.suspiciousPermissions.size * 3
        }

        // Check if system app but high risk
        if (app.isSystemApp && app.riskScore > 70) {
            recommendations.add("System app with high risk - monitor closely")
            potentialRiskReduction += 10
        }

        val optimizedScore = maxOf(0, app.riskScore - potentialRiskReduction)

        return OptimizationResult(
            packageName = app.packageName,
            appName = app.appName,
            originalRiskScore = app.riskScore,
            optimizedRiskScore = optimizedScore,
            totalPotentialRiskReduction = potentialRiskReduction,
            optimizationRecommendations = recommendations
        )
    }

    override suspend fun optimizeAllApps(apps: List<AppEntity>): List<OptimizationResult> {
        return apps.map { optimizeAppSecurity(it) }
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
