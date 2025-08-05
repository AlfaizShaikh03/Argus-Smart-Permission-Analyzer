package com.yourname.smartpermissionanalyzer.data.mappers

import com.yourname.smartpermissionanalyzer.data.database.entities.PermissionEntity
import com.yourname.smartpermissionanalyzer.domain.entities.PermissionHistoryEntity
import com.yourname.smartpermissionanalyzer.domain.entities.PermissionActionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionMapper @Inject constructor() {

    fun toHistoryEntity(dbEntity: PermissionEntity): PermissionHistoryEntity {
        return PermissionHistoryEntity(
            id = dbEntity.id,
            packageName = dbEntity.packageName,
            permissionName = dbEntity.permissionName,
            action = if (dbEntity.isGranted) PermissionActionEntity.GRANTED else PermissionActionEntity.DENIED,
            timestamp = dbEntity.timestamp,
            previousState = null,
            newState = if (dbEntity.isGranted) "GRANTED" else "DENIED",
            userTriggered = false
        )
    }
}
