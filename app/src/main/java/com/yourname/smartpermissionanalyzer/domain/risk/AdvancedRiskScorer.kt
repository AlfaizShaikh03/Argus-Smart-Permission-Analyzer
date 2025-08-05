package com.yourname.smartpermissionanalyzer.domain.risk

import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity

interface AdvancedRiskScorer {
    suspend fun calculateAdvancedRiskScore(app: AppEntity): RiskAnalysis
    suspend fun analyzeRiskTrends(apps: List<AppEntity>): RiskTrendAnalysis
}

data class RiskAnalysis(
    val packageName: String,
    val appName: String,
    val finalRiskScore: Int,
    val riskLevel: RiskLevelEntity,
    val riskFactors: List<RiskFactor>,
    val trustScore: Float,
    val recommendation: String
)

data class RiskFactor(
    val factor: String,
    val severity: String,
    val impact: Int,
    val description: String
)

data class RiskTrendAnalysis(
    val totalAppsAnalyzed: Int,
    val averageRiskScore: Float,
    val highRiskAppCount: Int,
    val riskDistribution: Map<RiskLevelEntity, Int>,
    val topRiskFactors: List<String>
)
