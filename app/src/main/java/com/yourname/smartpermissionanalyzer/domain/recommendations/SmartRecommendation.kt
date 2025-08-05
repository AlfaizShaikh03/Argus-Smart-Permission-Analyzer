package com.yourname.smartpermissionanalyzer.domain.recommendations

data class SmartRecommendation(
    val id: String,
    val type: String, // ✅ REQUIRED PARAMETER
    val priority: RecommendationPriority,
    val title: String,
    val message: String,
    val actionable: Boolean = true,
    val category: String = "Security", // ✅ REQUIRED PARAMETER
    val confidenceScore: Float = 0.8f, // ✅ REQUIRED PARAMETER
    val impact: String = "Medium", // ✅ REQUIRED PARAMETER
    val timestamp: Long = System.currentTimeMillis() // ✅ REQUIRED PARAMETER
)

enum class RecommendationPriority {
    CRITICAL, HIGH, MEDIUM, LOW
}
