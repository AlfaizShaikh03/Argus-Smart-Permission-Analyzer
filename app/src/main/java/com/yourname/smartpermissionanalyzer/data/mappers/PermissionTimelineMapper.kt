package com.yourname.smartpermissionanalyzer.data.mappers

import com.yourname.smartpermissionanalyzer.data.database.entities.PermissionTimelineDbEntity
import com.yourname.smartpermissionanalyzer.domain.entities.PermissionTimelineEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionTimelineMapper @Inject constructor() {

    fun toDbEntity(domain: PermissionTimelineEntity): PermissionTimelineDbEntity {
        return PermissionTimelineDbEntity(
            id = domain.id,
            packageName = domain.packageName,
            appName = domain.appName,
            permissionName = domain.permissionName,
            friendlyPermissionName = domain.friendlyPermissionName,
            action = domain.action,
            timestamp = domain.timestamp,
            previousRiskLevel = domain.previousRiskLevel,
            newRiskLevel = domain.newRiskLevel,
            category = domain.category,
            impact = domain.impact,
            context = domain.context,
            userTriggered = domain.userTriggered
        )
    }

    fun toDomainEntity(db: PermissionTimelineDbEntity): PermissionTimelineEntity {
        return PermissionTimelineEntity(
            id = db.id,
            packageName = db.packageName,
            appName = db.appName,
            permissionName = db.permissionName,
            friendlyPermissionName = db.friendlyPermissionName,
            action = db.action,
            timestamp = db.timestamp,
            previousRiskLevel = db.previousRiskLevel,
            newRiskLevel = db.newRiskLevel,
            category = db.category,
            impact = db.impact,
            context = db.context,
            userTriggered = db.userTriggered
        )
    }
}
