package com.yourname.smartpermissionanalyzer.domain.repository

import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getAllApps(): Flow<List<AppEntity>>
    suspend fun getAppByPackageName(packageName: String): AppEntity?
    fun getAppsByRiskLevel(riskLevel: String): Flow<List<AppEntity>>
    fun getAppsByRiskScore(minScore: Int): Flow<List<AppEntity>>
    suspend fun getAppByPackage(packageName: String): AppEntity?
    fun searchApps(query: String): Flow<List<AppEntity>>
    fun getAppsByCategory(category: String): Flow<List<AppEntity>>
    suspend fun saveApp(app: AppEntity): Result<Unit>
    suspend fun saveApps(apps: List<AppEntity>): Result<Unit>
    suspend fun deleteApp(packageName: String): Result<Unit>
    suspend fun clearApps(): Result<Unit>
    suspend fun getAppCount(): Int
    fun getHighRiskApps(): Flow<List<AppEntity>>
    fun getSystemApps(): Flow<List<AppEntity>>
    fun getUserApps(): Flow<List<AppEntity>>
    suspend fun refreshApp(packageName: String): Result<AppEntity?>
    suspend fun updateApp(app: AppEntity): Result<Unit>
    suspend fun getAppsByPackageNames(packageNames: List<String>): List<AppEntity>
    suspend fun getRecentlyScannedApps(limit: Int = 10): List<AppEntity>
}
