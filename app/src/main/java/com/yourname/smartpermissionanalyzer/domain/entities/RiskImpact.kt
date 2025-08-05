package com.yourname.smartpermissionanalyzer.domain.entities

enum class RiskImpact {
    CRITICAL, HIGH, MEDIUM, LOW, MINIMAL;

    companion object {
        fun fromString(value: String): RiskImpact {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LOW
        }

        fun fromScore(score: Int): RiskImpact {
            return when {
                score >= 90 -> CRITICAL
                score >= 70 -> HIGH
                score >= 50 -> MEDIUM
                score >= 30 -> LOW
                else -> MINIMAL
            }
        }
    }
}
