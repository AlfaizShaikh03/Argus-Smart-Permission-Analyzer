package com.yourname.smartpermissionanalyzer.domain.repository

import com.yourname.smartpermissionanalyzer.domain.entities.PermissionTimelineEntity
import kotlinx.coroutines.flow.Flow

interface PermissionTimelineRepository {
    fun getPermissionTimeline(packageName: String): Flow<List<PermissionTimelineEntity>>
    suspend fun addTimelineEntry(entry: PermissionTimelineEntity): Result<Unit>
    suspend fun clearTimeline(packageName: String): Result<Unit>
}
