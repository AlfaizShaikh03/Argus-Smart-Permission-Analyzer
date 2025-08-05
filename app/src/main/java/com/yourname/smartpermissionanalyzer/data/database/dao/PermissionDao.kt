package com.yourname.smartpermissionanalyzer.data.database.dao

import androidx.room.*
import com.yourname.smartpermissionanalyzer.data.database.entities.PermissionEntity
import com.yourname.smartpermissionanalyzer.data.database.entities.PermissionUsageStats
import kotlinx.coroutines.flow.Flow

@Dao
interface PermissionDao {

    @Query("SELECT * FROM permissions WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getPermissionsForApp(packageName: String): Flow<List<PermissionEntity>>

    @Query("SELECT permissionName, COUNT(*) as count FROM permissions GROUP BY permissionName")
    suspend fun getPermissionUsageStats(): List<PermissionUsageStats>

    @Query("SELECT COUNT(*) FROM permissions")
    suspend fun getPermissionCount(): Int

    @Query("DELETE FROM permissions WHERE packageName = :packageName")
    suspend fun deletePermissionsForApp(packageName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermission(permission: PermissionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermissions(permissions: List<PermissionEntity>)

    @Query("DELETE FROM permissions")
    suspend fun deleteAllPermissions()

    @Query("SELECT * FROM permissions WHERE permissionName = :permissionName ORDER BY timestamp DESC")
    fun getPermissionHistory(permissionName: String): Flow<List<PermissionEntity>>

    @Query("SELECT * FROM permissions WHERE isDangerous = 1 ORDER BY timestamp DESC")
    fun getDangerousPermissions(): Flow<List<PermissionEntity>>

    @Query("SELECT DISTINCT packageName FROM permissions")
    suspend fun getAllPackageNames(): List<String>

    @Query("SELECT DISTINCT permissionName FROM permissions")
    suspend fun getAllPermissionNames(): List<String>

    @Query("SELECT COUNT(DISTINCT packageName) FROM permissions")
    suspend fun getUniqueAppCount(): Int
}
