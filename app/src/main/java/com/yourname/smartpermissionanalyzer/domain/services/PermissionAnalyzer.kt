package com.yourname.smartpermissionanalyzer.domain.services

import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity

interface PermissionAnalyzer {
    suspend fun analyzeAllInstalledApps(): List<AppEntity>
    suspend fun analyzeSpecificApp(packageName: String): AppEntity?
    suspend fun refreshAppAnalysis(packageName: String): AppEntity?
}
