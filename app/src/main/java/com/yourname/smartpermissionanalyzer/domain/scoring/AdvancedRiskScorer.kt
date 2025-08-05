package com.yourname.smartpermissionanalyzer.domain.scoring

import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdvancedRiskScorer @Inject constructor() {

    // This is our secret sauce - a sophisticated scoring algorithm
    fun calculateAdvancedRiskScore(app: AppEntity): RiskAnalysisResult {
        // Step 1: Analyze permission patterns (40% of score)
        val permissionScore = analyzePermissionPatterns(app)

        // Step 2: Check app metadata (25% of score)
        val metadataScore = analyzeAppMetadata(app)

        // Step 3: Behavioral analysis (20% of score)
        val behaviorScore = analyzeBehaviorPatterns(app)

        // Step 4: Threat intelligence (15% of score)
        val threatScore = checkThreatIntelligence(app)

        // Calculate weighted final score
        val finalScore = (permissionScore * 0.4f +
                metadataScore * 0.25f +
                behaviorScore * 0.2f +
                threatScore * 0.15f).toInt()

        return RiskAnalysisResult(
            score = finalScore,
            level = determineRiskLevel(finalScore),
            factors = buildRiskFactors(app, permissionScore, metadataScore, behaviorScore, threatScore),
            recommendations = generateRecommendations(finalScore, app)
        )
    }

    private fun analyzePermissionPatterns(app: AppEntity): Int {
        var riskScore = 0

        // Critical permissions that significantly increase risk
        val criticalPermissions = listOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_SMS",
            "android.permission.CALL_PHONE",
            "android.permission.READ_CALL_LOG"
        )

        // Check for dangerous permission combinations
        val hasCamera = app.permissions.contains("android.permission.CAMERA")
        val hasMicrophone = app.permissions.contains("android.permission.RECORD_AUDIO")
        val hasLocation = app.permissions.contains("android.permission.ACCESS_FINE_LOCATION")
        val hasInternet = app.permissions.contains("android.permission.INTERNET")

        // Suspicious combination: Camera + Microphone + Internet = Spyware potential
        if (hasCamera && hasMicrophone && hasInternet) {
            riskScore += 40
        }

        // Location tracking with internet = Privacy concern
        if (hasLocation && hasInternet) {
            riskScore += 25
        }

        // Count critical permissions
        val criticalCount = app.permissions.count { it in criticalPermissions }
        riskScore += criticalCount * 8

        // Excessive permissions for app category
        riskScore += analyzePermissionExcess(app)

        return minOf(100, riskScore)
    }

    private fun analyzeAppMetadata(app: AppEntity): Int {
        var riskScore = 0

        // System apps are generally safer
        if (app.isSystemApp) {
            riskScore -= 20
        }

        // New apps or recently updated apps need scrutiny
        val daysSinceInstall = (System.currentTimeMillis() - app.installTime) / (1000 * 60 * 60 * 24)
        if (daysSinceInstall < 7) {
            riskScore += 15
        }

        // Apps with very low target SDK are risky
        if (app.targetSdkVersion < 26) { // Below Android 8.0
            riskScore += 25
        }

        // Unknown or suspicious developer
        if (app.versionName.contains("debug", ignoreCase = true) ||
            app.versionName.contains("test", ignoreCase = true)) {
            riskScore += 30
        }

        return maxOf(0, minOf(100, riskScore))
    }

    private fun analyzeBehaviorPatterns(app: AppEntity): Int {
        var riskScore = 0

        // This is where we'd analyze actual app behavior
        // For now, we'll use heuristics based on available data

        // Apps that request permissions they don't need
        riskScore += analyzePermissionUsage(app)

        // Apps with unusual naming patterns
        if (hasObfuscatedName(app.packageName)) {
            riskScore += 20
        }

        return minOf(100, riskScore)
    }

    private fun checkThreatIntelligence(app: AppEntity): Int {
        // This would connect to real threat intelligence feeds
        // For now, simulate with known patterns

        val knownMaliciousPatterns = listOf(
            "com.fake", "com.malware", "com.suspicious"
        )

        return if (knownMaliciousPatterns.any { app.packageName.contains(it) }) {
            100
        } else {
            0
        }
    }

    private fun analyzePermissionExcess(app: AppEntity): Int {
        // Different app categories should have different permission expectations
        return when (app.appCategory.name) {
            "GAME" -> if (app.permissions.size > 8) 15 else 0
            "COMMUNICATION" -> if (app.permissions.size > 12) 10 else 0
            "MEDIA" -> if (app.permissions.size > 10) 12 else 0
            "FINANCE" -> if (app.permissions.size > 6) 20 else 0
            else -> if (app.permissions.size > 15) 18 else 0
        }
    }

    private fun analyzePermissionUsage(app: AppEntity): Int {
        // Heuristic: apps with many permissions but simple names are suspicious
        val nameComplexity = app.appName.split(" ").size
        val permissionCount = app.permissions.size

        return if (permissionCount > nameComplexity * 3) 25 else 0
    }

    private fun hasObfuscatedName(packageName: String): Boolean {
        // Check for random-looking package names
        val segments = packageName.split(".")
        return segments.any { segment ->
            segment.length > 8 && segment.count { it.isDigit() } > segment.length / 2
        }
    }

    private fun determineRiskLevel(score: Int): RiskLevelEntity {
        return when {
            score >= 80 -> RiskLevelEntity.CRITICAL  // 80-100: Very dangerous
            score >= 60 -> RiskLevelEntity.HIGH      // 60-79: Dangerous
            score >= 40 -> RiskLevelEntity.MEDIUM    // 40-59: Moderate risk
            score >= 20 -> RiskLevelEntity.LOW       // 20-39: Low risk
            score >= 10 -> RiskLevelEntity.MINIMAL   // ✅ ADDED: 10-19: Very low risk
            else -> RiskLevelEntity.UNKNOWN          // 0-9: No significant risk
        }
    }

    private fun buildRiskFactors(
        app: AppEntity,
        permissionScore: Int,
        metadataScore: Int,
        behaviorScore: Int,
        threatScore: Int
    ): List<String> {
        val factors = mutableListOf<String>()

        if (permissionScore > 30) {
            factors.add("Requests excessive or suspicious permissions")
        }
        if (metadataScore > 20) {
            factors.add("App metadata indicates potential security concerns")
        }
        if (behaviorScore > 25) {
            factors.add("Unusual behavior patterns detected")
        }
        if (threatScore > 0) {
            factors.add("Matches known threat intelligence indicators")
        }

        return factors
    }

    private fun generateRecommendations(score: Int, app: AppEntity): List<String> {
        val recommendations = mutableListOf<String>()

        when {
            score >= 80 -> {
                recommendations.add("Consider uninstalling this app immediately")
                recommendations.add("Review what data this app may have accessed")
                recommendations.add("Check for any suspicious account activity")
            }
            score >= 60 -> {
                recommendations.add("Revoke unnecessary permissions")
                recommendations.add("Monitor this app's behavior closely")
                recommendations.add("Consider finding a safer alternative")
            }
            score >= 40 -> {
                recommendations.add("Review and limit permissions")
                recommendations.add("Keep the app updated")
                recommendations.add("Be cautious when granting new permissions")
            }
            else -> {
                recommendations.add("This app appears to be relatively safe")
                recommendations.add("Continue monitoring permission usage")
            }
        }

        return recommendations
    }
}

data class RiskAnalysisResult(
    val score: Int,
    val level: RiskLevelEntity,
    val factors: List<String>,
    val recommendations: List<String>
)
