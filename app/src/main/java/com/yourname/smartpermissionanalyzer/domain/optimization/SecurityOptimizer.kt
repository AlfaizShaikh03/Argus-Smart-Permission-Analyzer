package com.yourname.smartpermissionanalyzer.domain.optimization

import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity

interface SecurityOptimizer {
    suspend fun optimizeAppSecurity(app: AppEntity): OptimizationResult
    suspend fun optimizeAllApps(apps: List<AppEntity>): List<OptimizationResult>
}

data class OptimizationResult(
    val packageName: String,
    val appName: String,
    val originalRiskScore: Int,
    val optimizedRiskScore: Int,
    val totalPotentialRiskReduction: Int,
    val optimizationRecommendations: List<String>
)
