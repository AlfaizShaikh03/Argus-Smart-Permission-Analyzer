package com.yourname.smartpermissionanalyzer.core.analyzer

import com.yourname.smartpermissionanalyzer.data.models.AppCategory

class SuspiciousPatternDetector {

    fun detectPatterns(
        packageName: String,
        appName: String,
        permissions: List<String>,
        category: AppCategory
    ): List<String> {
        val suspiciousPatterns = mutableListOf<String>()

        // Category-specific suspicious patterns
        when (category) {
            AppCategory.GAME -> {
                suspiciousPatterns.addAll(detectGameSuspiciousPatterns(permissions))
            }
            AppCategory.UTILITY -> {
                suspiciousPatterns.addAll(detectUtilitySuspiciousPatterns(appName, permissions))
            }
            AppCategory.SOCIAL -> {
                suspiciousPatterns.addAll(detectSocialSuspiciousPatterns(permissions))
            }
            else -> {
                suspiciousPatterns.addAll(detectGeneralSuspiciousPatterns(permissions))
            }
        }

        // Universal suspicious combinations
        suspiciousPatterns.addAll(detectUniversalSuspiciousPatterns(permissions))

        return suspiciousPatterns.distinct()
    }

    private fun detectGameSuspiciousPatterns(permissions: List<String>): List<String> {
        val patterns = mutableListOf<String>()

        // Games shouldn't need SMS/Call access
        if (permissions.any { it.contains("SMS") || it.contains("READ_SMS") }) {
            patterns.add("Game requesting SMS access")
        }

        if (permissions.any { it.contains("CALL_LOG") || it.contains("READ_CALL_LOG") }) {
            patterns.add("Game requesting call log access")
        }

        if (permissions.any { it.contains("READ_CONTACTS") }) {
            patterns.add("Game requesting contacts access")
        }

        return patterns
    }

    private fun detectUtilitySuspiciousPatterns(appName: String, permissions: List<String>): List<String> {
        val patterns = mutableListOf<String>()

        // Flashlight apps with excessive permissions
        if (appName.contains("flashlight", ignoreCase = true) ||
            appName.contains("torch", ignoreCase = true)) {

            if (permissions.any { it.contains("CAMERA") } &&
                permissions.any { it.contains("CONTACTS") }) {
                patterns.add("Flashlight app requesting camera and contacts")
            }

            if (permissions.any { it.contains("LOCATION") }) {
                patterns.add("Flashlight app requesting location access")
            }
        }

        return patterns
    }

    private fun detectSocialSuspiciousPatterns(permissions: List<String>): List<String> {
        val patterns = mutableListOf<String>()

        // Too many sensitive permissions for social apps
        var sensitiveCount = 0
        if (permissions.any { it.contains("CAMERA") }) sensitiveCount++
        if (permissions.any { it.contains("RECORD_AUDIO") }) sensitiveCount++
        if (permissions.any { it.contains("FINE_LOCATION") }) sensitiveCount++
        if (permissions.any { it.contains("READ_SMS") }) sensitiveCount++

        if (sensitiveCount >= 3) {
            patterns.add("Social app with excessive sensitive permissions")
        }

        return patterns
    }

    private fun detectGeneralSuspiciousPatterns(permissions: List<String>): List<String> {
        val patterns = mutableListOf<String>()

        // Apps with too many high-risk permissions
        val highRiskPerms = permissions.filter {
            it.contains("CAMERA") || it.contains("RECORD_AUDIO") ||
                    it.contains("FINE_LOCATION") || it.contains("READ_SMS") ||
                    it.contains("READ_CONTACTS")
        }

        if (highRiskPerms.size >= 4) {
            patterns.add("App requesting multiple high-risk permissions")
        }

        return patterns
    }

    private fun detectUniversalSuspiciousPatterns(permissions: List<String>): List<String> {
        val patterns = mutableListOf<String>()

        // Dangerous permission combinations
        if (permissions.any { it.contains("RECORD_AUDIO") } &&
            permissions.any { it.contains("FINE_LOCATION") }) {
            patterns.add("Audio recording with precise location tracking")
        }

        if (permissions.any { it.contains("CAMERA") } &&
            permissions.any { it.contains("READ_CONTACTS") } &&
            permissions.any { it.contains("FINE_LOCATION") }) {
            patterns.add("Camera, contacts, and location access combination")
        }

        return patterns
    }
}
