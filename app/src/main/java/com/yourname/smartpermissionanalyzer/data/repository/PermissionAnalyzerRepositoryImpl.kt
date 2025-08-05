package com.yourname.smartpermissionanalyzer.data.repository

import com.yourname.smartpermissionanalyzer.data.local.dao.AppEntityDao
import com.yourname.smartpermissionanalyzer.data.local.dao.ExclusionDao
import com.yourname.smartpermissionanalyzer.data.local.entities.ExclusionEntityDb
import com.yourname.smartpermissionanalyzer.data.mapper.EntityMapper
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.repository.PermissionAnalyzerRepository
import com.yourname.smartpermissionanalyzer.domain.monitoring.SecurityAlert
import com.yourname.smartpermissionanalyzer.domain.entities.PermissionChangeLog
import com.yourname.smartpermissionanalyzer.domain.entities.NetworkActivityLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class PermissionAnalyzerRepositoryImpl @Inject constructor(
    private val dao: AppEntityDao,
    private val exclusionDao: ExclusionDao,
    private val mapper: EntityMapper,
    @ApplicationContext private val context: Context // ✅ FIXED: Added missing context injection
) : PermissionAnalyzerRepository {

    override suspend fun saveAnalysisResults(apps: List<AppEntity>): Result<Unit> {
        return try {
            // Filter out excluded apps before saving
            val excludedPackages = try {
                exclusionDao.getExcludedPackageNames().toSet()
            } catch (e: Exception) {
                // If exclusion table doesn't exist yet, continue without filtering
                emptySet()
            }

            val filteredApps = apps.filter { it.packageName !in excludedPackages }
            val dbEntities = filteredApps.map { mapper.mapToDbEntity(it) }
            dao.insertAll(dbEntities)

            println("Repository: Saved ${dbEntities.size} apps (${apps.size - filteredApps.size} excluded)")
            Result.success(Unit)
        } catch (e: Exception) {
            println("Repository: Error saving apps - ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getAnalysisResults(): Result<List<AppEntity>> {
        return try {
            val dbEntities = dao.getAllApps()
            val entities = dbEntities.map { mapper.mapToEntity(it) }
            Result.success(entities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppByPackageName(packageName: String): Result<AppEntity?> {
        return try {
            val dbEntity = dao.getAppByPackageName(packageName)
            val entity = dbEntity?.let { mapper.mapToEntity(it) }
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateApp(app: AppEntity): Result<Unit> {
        return try {
            val dbEntity = mapper.mapToDbEntity(app)
            dao.updateApp(dbEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteApp(packageName: String): Result<Unit> {
        return try {
            // Get app info before deletion
            val app = dao.getAppByPackageName(packageName)
            val appName = app?.appName ?: "Unknown App"

            // Delete from main database
            dao.deleteAppByPackageName(packageName)

            // Add to exclusion list to prevent re-analysis
            try {
                val exclusionEntity = ExclusionEntityDb(
                    packageName = packageName,
                    appName = appName,
                    excludedAt = System.currentTimeMillis()
                )
                exclusionDao.insertExclusion(exclusionEntity)
            } catch (e: Exception) {
                println("Repository: Warning - could not add to exclusion list: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAllAnalysisResults(): Result<Unit> {
        return try {
            dao.deleteAllApps()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAnalysisResults(): Flow<List<AppEntity>> {
        return dao.observeAllApps().map { dbEntities ->
            dbEntities.map { mapper.mapToEntity(it) }
        }
    }

    override suspend fun getHighRiskApps(): Result<List<AppEntity>> {
        return try {
            val dbEntities = dao.getHighRiskApps(60)
            val entities = dbEntities.map { mapper.mapToEntity(it) }
            Result.success(entities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppsByCategory(category: String): Result<List<AppEntity>> {
        return try {
            val dbEntities = dao.getAppsByCategory(category)
            val entities = dbEntities.map { mapper.mapToEntity(it) }
            Result.success(entities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchApps(query: String): Result<List<AppEntity>> {
        return try {
            val dbEntities = dao.searchApps("%$query%")
            val entities = dbEntities.map { mapper.mapToEntity(it) }
            Result.success(entities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLastKnownApps(): List<AppEntity> {
        return try {
            val dbEntities = dao.getAllApps()
            dbEntities.map { mapper.mapToEntity(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun updateKnownApps(apps: List<AppEntity>) {
        try {
            // Filter out excluded apps
            val excludedPackages = try {
                exclusionDao.getExcludedPackageNames().toSet()
            } catch (e: Exception) {
                emptySet()
            }

            val filteredApps = apps.filter { it.packageName !in excludedPackages }
            dao.deleteAllApps()
            val dbEntities = filteredApps.map { mapper.mapToDbEntity(it) }
            dao.insertAll(dbEntities)
        } catch (e: Exception) {
            // Handle error silently for monitoring service
        }
    }

    override suspend fun saveSecurityAlert(alert: SecurityAlert) {
        // Implementation for saving security alerts
        // For now, we'll skip this as it would require additional database tables
    }

    override suspend fun scanInstalledApps(): List<AppEntity> {
        return try {
            val dbEntities = dao.getAllApps()
            dbEntities.map { mapper.mapToEntity(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Exclusion list implementation
    override suspend fun addToExclusionList(packageName: String, appName: String): Result<Unit> {
        return try {
            val exclusionEntity = ExclusionEntityDb(
                packageName = packageName,
                appName = appName,
                excludedAt = System.currentTimeMillis()
            )
            exclusionDao.insertExclusion(exclusionEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFromExclusionList(packageName: String): Result<Unit> {
        return try {
            exclusionDao.removeExclusion(packageName)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getExclusionList(): Result<List<String>> {
        return try {
            val packageNames = exclusionDao.getExcludedPackageNames()
            Result.success(packageNames)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isExcluded(packageName: String): Result<Boolean> {
        return try {
            val isExcluded = exclusionDao.isPackageExcluded(packageName)
            Result.success(isExcluded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ FIXED: All methods now have access to context
    override suspend fun savePermissionChange(permissionChange: PermissionChangeLog): Result<Unit> {
        return try {
            // Simple SharedPreferences storage (replace with Room database in production)
            val sharedPrefs = context.getSharedPreferences("permission_logs", Context.MODE_PRIVATE)
            val existingLogs = sharedPrefs.getString("logs", "") ?: ""

            val logEntry = "${permissionChange.packageName}|${permissionChange.changeType}|${permissionChange.timestamp}"
            val updatedLogs = if (existingLogs.isEmpty()) logEntry else "$existingLogs,$logEntry"

            sharedPrefs.edit().putString("logs", updatedLogs).apply()

            android.util.Log.d("Repository", "Permission change saved: ${permissionChange.packageName}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Failed to save permission change", e)
            Result.failure(e)
        }
    }

    override suspend fun saveNetworkActivity(networkActivity: NetworkActivityLog): Result<Unit> {
        return try {
            // Simple SharedPreferences storage (replace with Room database in production)
            val sharedPrefs = context.getSharedPreferences("network_logs", Context.MODE_PRIVATE)
            val existingLogs = sharedPrefs.getString("network_logs", "") ?: ""

            val logEntry = "${networkActivity.timestamp}|${networkActivity.bytesReceived}|${networkActivity.bytesSent}"
            val updatedLogs = if (existingLogs.isEmpty()) logEntry else "$existingLogs,$logEntry"

            sharedPrefs.edit().putString("network_logs", updatedLogs).apply()

            android.util.Log.d("Repository", "Network activity saved: ${networkActivity.bytesReceived} bytes")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Failed to save network activity", e)
            Result.failure(e)
        }
    }

    override suspend fun markAppAsTrusted(packageName: String): Result<Unit> {
        return try {
            val sharedPrefs = context.getSharedPreferences("trusted_apps", Context.MODE_PRIVATE)
            val trustedApps = sharedPrefs.getStringSet("apps", emptySet())?.toMutableSet() ?: mutableSetOf()
            trustedApps.add(packageName)
            sharedPrefs.edit().putStringSet("apps", trustedApps).apply()

            android.util.Log.d("Repository", "App marked as trusted: $packageName")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Failed to mark app as trusted", e)
            Result.failure(e)
        }
    }

    override suspend fun clearExclusionList(): Result<Unit> {
        return try {
            exclusionDao.clearAllExclusions()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
