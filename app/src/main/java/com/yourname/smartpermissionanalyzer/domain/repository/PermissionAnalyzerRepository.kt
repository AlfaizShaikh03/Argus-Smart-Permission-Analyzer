package com.yourname.smartpermissionanalyzer.domain.repository

import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.PermissionChangeLog
import com.yourname.smartpermissionanalyzer.domain.entities.NetworkActivityLog
import com.yourname.smartpermissionanalyzer.domain.monitoring.SecurityAlert
import kotlinx.coroutines.flow.Flow

interface PermissionAnalyzerRepository {

    suspend fun saveAnalysisResults(apps: List<AppEntity>): Result<Unit>

    suspend fun getAnalysisResults(): Result<List<AppEntity>>

    suspend fun getAppByPackageName(packageName: String): Result<AppEntity?>

    suspend fun updateApp(app: AppEntity): Result<Unit>

    suspend fun deleteApp(packageName: String): Result<Unit>

    suspend fun clearAllAnalysisResults(): Result<Unit>

    fun observeAnalysisResults(): Flow<List<AppEntity>>

    suspend fun getHighRiskApps(): Result<List<AppEntity>>

    suspend fun getAppsByCategory(category: String): Result<List<AppEntity>>

    suspend fun searchApps(query: String): Result<List<AppEntity>>

    suspend fun getLastKnownApps(): List<AppEntity>

    suspend fun updateKnownApps(apps: List<AppEntity>)

    suspend fun saveSecurityAlert(alert: SecurityAlert)

    suspend fun scanInstalledApps(): List<AppEntity>

    // Exclusion list management methods
    suspend fun addToExclusionList(packageName: String, appName: String): Result<Unit>

    suspend fun removeFromExclusionList(packageName: String): Result<Unit>

    suspend fun getExclusionList(): Result<List<String>>

    suspend fun isExcluded(packageName: String): Result<Boolean>

    suspend fun clearExclusionList(): Result<Unit>

    // ✅ NEW: Enhanced monitoring methods
    suspend fun savePermissionChange(permissionChange: PermissionChangeLog): Result<Unit>

    suspend fun saveNetworkActivity(networkActivity: NetworkActivityLog): Result<Unit>

    suspend fun markAppAsTrusted(packageName: String): Result<Unit>
}
