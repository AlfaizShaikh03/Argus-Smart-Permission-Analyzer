package com.yourname.smartpermissionanalyzer.domain.analyzer

import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity

interface PermissionAnalyzer {

    suspend fun scanInstalledApps(): Result<List<AppEntity>>

    suspend fun analyzeAppPermissions(packageName: String): Result<AppEntity>

    suspend fun analyzePermissionRisk(permissions: List<String>): Result<Int>
}
