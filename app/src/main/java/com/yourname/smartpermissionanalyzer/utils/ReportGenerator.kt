package com.yourname.smartpermissionanalyzer.utils

import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.data.models.AppPermissionInfo
import com.yourname.smartpermissionanalyzer.data.models.RiskLevel
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportGenerator @Inject constructor() {

    fun generateSecurityReport(apps: List<AppEntity>): String {
        val reportBuilder = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentTime = dateFormat.format(Date())

        reportBuilder.append("=".repeat(60)).append("\n")
        reportBuilder.append("        SMART PERMISSION ANALYZER REPORT\n")
        reportBuilder.append("=".repeat(60)).append("\n")
        reportBuilder.append("Generated: $currentTime\n\n")

        // Summary Statistics
        val totalApps = apps.size
        val criticalRiskApps = apps.count { it.riskLevel.name == "CRITICAL" }
        val highRiskApps = apps.count { it.riskLevel.name == "HIGH" }
        val mediumRiskApps = apps.count { it.riskLevel.name == "MEDIUM" }
        val lowRiskApps = apps.count { it.riskLevel.name == "LOW" }

        reportBuilder.append("SUMMARY\n")
        reportBuilder.append("-".repeat(20)).append("\n")
        reportBuilder.append("Total Apps Analyzed: $totalApps\n")
        reportBuilder.append("Critical Risk Apps: $criticalRiskApps\n")
        reportBuilder.append("High Risk Apps: $highRiskApps\n")
        reportBuilder.append("Medium Risk Apps: $mediumRiskApps\n")
        reportBuilder.append("Low Risk Apps: $lowRiskApps\n\n")

        // Risk Distribution
        val riskPercentage = if (totalApps > 0) {
            ((criticalRiskApps + highRiskApps).toFloat() / totalApps * 100).toInt()
        } else 0

        reportBuilder.append("Security Risk Level: ${getRiskDescription(riskPercentage)}\n")
        reportBuilder.append("High Risk Percentage: $riskPercentage%\n\n")

        // Critical and High Risk Apps Details
        val dangerousApps = apps.filter {
            it.riskLevel.name == "CRITICAL" || it.riskLevel.name == "HIGH"
        }

        if (dangerousApps.isNotEmpty()) {
            reportBuilder.append("HIGH PRIORITY SECURITY CONCERNS\n")
            reportBuilder.append("-".repeat(35)).append("\n")

            dangerousApps.sortedByDescending { it.riskScore }.forEach { app ->
                reportBuilder.append("App: ${app.appName}\n")
                reportBuilder.append("Package: ${app.packageName}\n")
                reportBuilder.append("Risk Level: ${app.riskLevel.name}\n")
                reportBuilder.append("Risk Score: ${app.riskScore}/100\n")
                reportBuilder.append("Permissions: ${app.permissions.size}\n")
                reportBuilder.append("Suspicious Permissions: ${app.suspiciousPermissionCount}\n")

                if (app.suspiciousPermissions.isNotEmpty()) {
                    reportBuilder.append("Security Concerns:\n")
                    app.suspiciousPermissions.take(3).forEach { concern ->
                        reportBuilder.append("  • $concern\n")
                    }
                }

                reportBuilder.append("\n")
            }
        }

        // Detailed App Analysis
        reportBuilder.append("DETAILED APP ANALYSIS\n")
        reportBuilder.append("-".repeat(25)).append("\n")

        apps.sortedByDescending { it.riskScore }.forEach { app ->
            reportBuilder.append("${app.appName} (${app.packageName})\n")
            reportBuilder.append("  Risk: ${app.riskLevel.name} (${app.riskScore}/100)\n")
            reportBuilder.append("  Category: ${app.appCategory.name}\n")
            reportBuilder.append("  Permissions: ${app.permissions.size}\n")
            reportBuilder.append("  System App: ${if (app.isSystemApp) "Yes" else "No"}\n")
            reportBuilder.append("  Trust Score: ${"%.1f".format(app.trustScore)}/100\n")

            if (app.permissions.isNotEmpty()) {
                reportBuilder.append("  Key Permissions:\n")
                val keyPermissions = app.permissions.filter { permission ->
                    listOf("CAMERA", "LOCATION", "CONTACTS", "SMS", "RECORD_AUDIO", "READ_CALL_LOG")
                        .any { permission.contains(it) }
                }.take(5)

                keyPermissions.forEach { permission ->
                    reportBuilder.append("    - ${permission.substringAfterLast(".")}\n")
                }
            }

            reportBuilder.append("\n")
        }

        // Recommendations
        reportBuilder.append("SECURITY RECOMMENDATIONS\n")
        reportBuilder.append("-".repeat(28)).append("\n")

        when {
            criticalRiskApps > 0 -> {
                reportBuilder.append("🔴 IMMEDIATE ACTION REQUIRED:\n")
                reportBuilder.append("  • Review and potentially uninstall critical risk apps\n")
                reportBuilder.append("  • Revoke unnecessary permissions from high-risk apps\n")
                reportBuilder.append("  • Enable enhanced monitoring for suspicious apps\n")
            }
            highRiskApps > 0 -> {
                reportBuilder.append("🟡 ATTENTION NEEDED:\n")
                reportBuilder.append("  • Review permissions for high-risk apps\n")
                reportBuilder.append("  • Consider alternatives for apps with excessive permissions\n")
                reportBuilder.append("  • Monitor app behavior regularly\n")
            }
            else -> {
                reportBuilder.append("✅ GOOD SECURITY POSTURE:\n")
                reportBuilder.append("  • Continue regular security scans\n")
                reportBuilder.append("  • Review permissions for new app installations\n")
                reportBuilder.append("  • Keep apps updated to latest versions\n")
            }
        }

        reportBuilder.append("\nGeneral Security Tips:\n")
        reportBuilder.append("  • Only install apps from trusted sources\n")
        reportBuilder.append("  • Review app permissions before granting access\n")
        reportBuilder.append("  • Regularly audit and revoke unused permissions\n")
        reportBuilder.append("  • Keep your device and apps updated\n")
        reportBuilder.append("  • Use strong authentication methods\n\n")

        reportBuilder.append("=".repeat(60)).append("\n")
        reportBuilder.append("End of Report - Stay Secure!\n")
        reportBuilder.append("=".repeat(60))

        return reportBuilder.toString()
    }

    fun generateAppPermissionInfo(apps: List<AppEntity>): List<AppPermissionInfo> {
        return apps.map { app ->
            AppPermissionInfo(
                packageName = app.packageName,
                appName = app.appName,
                appIcon = null, // Icon handled in UI layer
                permissions = app.permissions,
                riskLevel = mapRiskLevel(app.riskLevel.name),
                riskScore = app.riskScore,
                appCategory = mapAppCategory(app.appCategory.name),
                suspiciousPermissions = app.suspiciousPermissions,
                suspiciousPermissionCount = app.suspiciousPermissionCount,
                installTime = app.installTime,
                lastUpdateTime = app.lastUpdateTime
            )
        }
    }

    private fun getRiskDescription(riskPercentage: Int): String {
        return when {
            riskPercentage >= 50 -> "High Risk - Immediate attention required"
            riskPercentage >= 25 -> "Medium Risk - Review recommended"
            riskPercentage >= 10 -> "Low Risk - Monitor regularly"
            else -> "Minimal Risk - Good security posture"
        }
    }

    private fun mapRiskLevel(riskLevelName: String): RiskLevel {
        return when (riskLevelName.uppercase()) {
            "CRITICAL" -> RiskLevel.CRITICAL
            "HIGH" -> RiskLevel.HIGH
            "MEDIUM" -> RiskLevel.MEDIUM
            "LOW" -> RiskLevel.LOW
            else -> RiskLevel.LOW
        }
    }

    private fun mapAppCategory(categoryName: String): com.yourname.smartpermissionanalyzer.data.models.AppCategory {
        return when (categoryName.uppercase()) {
            "SYSTEM" -> com.yourname.smartpermissionanalyzer.data.models.AppCategory.SYSTEM
            "GAME" -> com.yourname.smartpermissionanalyzer.data.models.AppCategory.GAME
            "SOCIAL" -> com.yourname.smartpermissionanalyzer.data.models.AppCategory.SOCIAL
            "FINANCE" -> com.yourname.smartpermissionanalyzer.data.models.AppCategory.FINANCE
            "UTILITY" -> com.yourname.smartpermissionanalyzer.data.models.AppCategory.UTILITY
            "COMMUNICATION" -> com.yourname.smartpermissionanalyzer.data.models.AppCategory.COMMUNICATION
            "MEDIA" -> com.yourname.smartpermissionanalyzer.data.models.AppCategory.MEDIA
            "PRODUCTIVITY" -> com.yourname.smartpermissionanalyzer.data.models.AppCategory.PRODUCTIVITY
            "HEALTH" -> com.yourname.smartpermissionanalyzer.data.models.AppCategory.HEALTH
            "EDUCATION" -> com.yourname.smartpermissionanalyzer.data.models.AppCategory.EDUCATION
            else -> com.yourname.smartpermissionanalyzer.data.models.AppCategory.UNKNOWN
        }
    }
}
