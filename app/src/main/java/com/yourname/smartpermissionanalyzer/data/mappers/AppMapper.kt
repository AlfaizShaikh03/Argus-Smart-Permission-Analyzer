package com.yourname.smartpermissionanalyzer.data.mappers

import com.yourname.smartpermissionanalyzer.data.database.entities.AppDatabaseEntity
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppMapper @Inject constructor() {
    fun toDbEntity(domain: AppEntity): AppDatabaseEntity = AppDatabaseEntity(
        packageName = domain.packageName,
        appName = domain.appName,
        permissions = domain.permissions,
        riskScore = domain.riskScore,
        riskLevel = domain.riskLevel.name,
        category = domain.appCategory.name,
        suspiciousPermissions = domain.suspiciousPermissions,
        suspiciousPermissionCount = domain.suspiciousPermissionCount,
        criticalPermissionCount = domain.criticalPermissionCount,
        installTime = domain.installTime,
        lastUpdate = domain.lastUpdate,
        lastScan = domain.lastScan,
        isSystemApp = domain.isSystemApp,
        versionCode = domain.versionCode,
        versionName = domain.versionName
    )
    fun toDomainEntity(db: AppDatabaseEntity): AppEntity = AppEntity(
        packageName = db.packageName,
        appName = db.appName,
        permissions = db.permissions,
        riskScore = db.riskScore,
        riskLevel = com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity.valueOf(db.riskLevel),
        appCategory = com.yourname.smartpermissionanalyzer.domain.entities.AppCategoryEntity.valueOf(db.category),
        suspiciousPermissions = db.suspiciousPermissions,
        suspiciousPermissionCount = db.suspiciousPermissionCount,
        criticalPermissionCount = db.criticalPermissionCount,
        installTime = db.installTime,
        lastUpdate = db.lastUpdate,
        lastScan = db.lastScan,
        isSystemApp = db.isSystemApp,
        versionCode = db.versionCode,
        versionName = db.versionName
    )
}
