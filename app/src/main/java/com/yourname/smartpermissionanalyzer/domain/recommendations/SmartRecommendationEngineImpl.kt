package com.yourname.smartpermissionanalyzer.domain.recommendations

import android.content.Context
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartRecommendationEngineImpl @Inject constructor() : SmartRecommendationEngine {

    override suspend fun generatePersonalizedRecommendations(
        apps: List<AppEntity>,
        userPreferences: UserSecurityPreferences,
        context: Context
    ): List<SmartRecommendation> {
        val recommendations = mutableListOf<SmartRecommendation>()
        val currentTime = System.currentTimeMillis()

        // 1. Critical Security Threats
        val criticalApps = apps.filter { it.riskLevel == RiskLevelEntity.CRITICAL && !it.isSystemApp }
        criticalApps.forEach { app ->
            recommendations.add(
                SmartRecommendation(
                    id = "critical_${app.packageName}_${currentTime}",
                    type = "UNINSTALL_APP",
                    priority = RecommendationPriority.CRITICAL,
                    title = "⚠️ Remove Critical Threat: ${app.appName}",
                    message = "This app poses serious security risks with ${app.suspiciousPermissionCount} suspicious permissions and a risk score of ${app.riskScore}/100. Consider uninstalling immediately. Package: ${app.packageName}",
                    actionable = true,
                    category = "Security",
                    confidenceScore = 0.95f,
                    impact = "High security risk reduction",
                    timestamp = currentTime
                )
            )
        }

        // 2. Permission-Based Recommendations
        val highRiskApps = apps.filter {
            it.riskLevel == RiskLevelEntity.HIGH &&
                    !it.isSystemApp &&
                    it.riskLevel != RiskLevelEntity.CRITICAL
        }

        highRiskApps.forEach { app ->
            val dangerousPermissions = app.permissions.filter { permission ->
                isDangerousPermission(permission) && permission in app.suspiciousPermissions
            }

            if (dangerousPermissions.isNotEmpty()) {
                recommendations.add(
                    SmartRecommendation(
                        id = "permission_${app.packageName}_${currentTime}",
                        type = "REVOKE_PERMISSION",
                        priority = RecommendationPriority.HIGH,
                        title = "🔒 Review ${app.appName} Permissions",
                        message = "Consider revoking dangerous permissions: ${dangerousPermissions.take(3).joinToString(", ")}. This app has access to sensitive data it may not need. Package: ${app.packageName}",
                        actionable = true,
                        category = "Privacy",
                        confidenceScore = 0.85f,
                        impact = "Reduced privacy exposure",
                        timestamp = currentTime - 1000
                    )
                )
            }
        }

        return recommendations
            .sortedByDescending { it.priority.ordinal * 1000 + (it.confidenceScore * 100).toInt() }
            .distinctBy { "${it.type}_${it.title}" }
            .take(20)
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
