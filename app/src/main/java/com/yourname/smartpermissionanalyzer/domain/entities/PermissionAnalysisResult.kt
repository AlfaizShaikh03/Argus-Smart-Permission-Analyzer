package com.yourname.smartpermissionanalyzer.domain.entities

data class PermissionAnalysisResult(
    val permissionName: String,
    val category: PermissionCategoryEntity,
    val riskLevel: RiskLevelEntity,
    val riskImpact: RiskImpact,
    val description: String,
    val isGranted: Boolean,
    val timestamp: Long,
    val recommendation: String
)
