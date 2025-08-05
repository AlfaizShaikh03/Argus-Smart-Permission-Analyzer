package com.yourname.smartpermissionanalyzer.data.repository

import com.yourname.smartpermissionanalyzer.domain.repository.PermissionTimelineRepository
import com.yourname.smartpermissionanalyzer.domain.entities.PermissionTimelineEntity
import com.yourname.smartpermissionanalyzer.data.database.dao.PermissionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionTimelineRepositoryImpl @Inject constructor(
    private val permissionDao: PermissionDao
) : PermissionTimelineRepository {

    override fun getPermissionTimeline(packageName: String): Flow<List<PermissionTimelineEntity>> {
        return permissionDao.getPermissionsForApp(packageName)
            .map { permissions ->
                permissions.map { permission ->
                    PermissionTimelineEntity(
                        id = permission.id,
                        packageName = permission.packageName,
                        appName = permission.packageName, // Use package name as fallback
                        permissionName = permission.permissionName,
                        friendlyPermissionName = getFriendlyName(permission.permissionName),
                        action = if (permission.isGranted) "GRANTED" else "DENIED",
                        timestamp = permission.timestamp,
                        previousRiskLevel = null,
                        newRiskLevel = if (permission.isGranted) "GRANTED" else "DENIED",
                        category = permission.permissionGroup ?: "UNKNOWN",
                        impact = "MEDIUM",
                        context = "Permission change",
                        userTriggered = false
                    )
                }
            }
            .catch { emit(emptyList()) }
    }

    override suspend fun addTimelineEntry(entry: PermissionTimelineEntity): Result<Unit> {
        return try {
            // Implementation would save timeline entry
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearTimeline(packageName: String): Result<Unit> {
        return try {
            permissionDao.deletePermissionsForApp(packageName)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFriendlyName(permissionName: String): String {
        return when {
            permissionName.contains("CAMERA") -> "Camera Access"
            permissionName.contains("LOCATION") -> "Location Access"
            permissionName.contains("CONTACTS") -> "Contacts Access"
            permissionName.contains("SMS") -> "SMS Access"
            permissionName.contains("CALL") -> "Phone Access"
            else -> permissionName.substringAfterLast(".").replace("_", " ")
        }
    }
}
