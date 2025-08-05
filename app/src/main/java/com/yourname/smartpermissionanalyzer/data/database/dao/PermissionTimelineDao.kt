package com.yourname.smartpermissionanalyzer.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.yourname.smartpermissionanalyzer.data.database.entities.PermissionTimelineDbEntity

@Dao
interface PermissionTimelineDao {

    @Query("SELECT * FROM permission_timeline ORDER BY timestamp DESC")
    fun getAllPermissionChanges(): Flow<List<PermissionTimelineDbEntity>>

    @Query("SELECT * FROM permission_timeline WHERE package_name = :packageName ORDER BY timestamp DESC")
    fun getPermissionChangesForApp(packageName: String): Flow<List<PermissionTimelineDbEntity>>

    @Query("SELECT * FROM permission_timeline WHERE timestamp >= :fromTime ORDER BY timestamp DESC")
    fun getPermissionChangesSince(fromTime: Long): Flow<List<PermissionTimelineDbEntity>>

    @Insert
    suspend fun insertPermissionChange(change: PermissionTimelineDbEntity)

    @Insert
    suspend fun insertPermissionChanges(changes: List<PermissionTimelineDbEntity>)

    @Query("DELETE FROM permission_timeline WHERE timestamp < :beforeTime")
    suspend fun deleteOldPermissionChanges(beforeTime: Long)

    @Query("DELETE FROM permission_timeline WHERE package_name = :packageName")
    suspend fun deletePermissionChangesForApp(packageName: String)
}
