package com.yourname.smartpermissionanalyzer.data.mapper

import com.yourname.smartpermissionanalyzer.data.local.entities.AppEntityDb
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.AppCategoryEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntityMapper @Inject constructor() {

    fun mapToEntity(dbEntity: AppEntityDb): AppEntity {
        return AppEntity(
            appName = dbEntity.appName,
            packageName = dbEntity.packageName,
            permissions = fromString(dbEntity.permissions),
            detailedPermissions = fromString(dbEntity.detailedPermissions),
            riskLevel = safeValueOf<RiskLevelEntity>(dbEntity.riskLevel) ?: RiskLevelEntity.UNKNOWN,
            riskScore = dbEntity.riskScore,
            appCategory = safeValueOf<AppCategoryEntity>(dbEntity.appCategory) ?: AppCategoryEntity.UNKNOWN,
            suspiciousPermissions = fromString(dbEntity.suspiciousPermissions),
            suspiciousPermissionCount = dbEntity.suspiciousPermissionCount,
            criticalPermissionCount = dbEntity.criticalPermissionCount,
            installTime = dbEntity.installTime,
            lastUpdateTime = dbEntity.lastUpdateTime,
            lastUpdate = dbEntity.lastUpdate,
            lastScannedTime = dbEntity.lastScannedTime,
            lastScan = dbEntity.lastScan,
            isSystemApp = dbEntity.isSystemApp,
            targetSdkVersion = dbEntity.targetSdkVersion,
            versionName = dbEntity.versionName,
            versionCode = dbEntity.versionCode,
            minSdkVersion = dbEntity.minSdkVersion,
            permissionDensity = dbEntity.permissionDensity,
            riskFactors = fromString(dbEntity.riskFactors),
            permissionChanges = fromString(dbEntity.permissionChanges),
            appSize = dbEntity.appSize,
            lastUsedTime = dbEntity.lastUsedTime,
            isEnabled = dbEntity.isEnabled,
            hasInternetAccess = dbEntity.hasInternetAccess,
            signatureHash = dbEntity.signatureHash,
            trustScore = dbEntity.trustScore
        )
    }

    fun mapToDbEntity(entity: AppEntity): AppEntityDb {
        return AppEntityDb(
            appName = entity.appName,
            packageName = entity.packageName,
            permissions = toString(entity.permissions),
            detailedPermissions = toString(entity.detailedPermissions),
            riskLevel = entity.riskLevel.name,
            riskScore = entity.riskScore,
            appCategory = entity.appCategory.name,
            suspiciousPermissions = toString(entity.suspiciousPermissions),
            suspiciousPermissionCount = entity.suspiciousPermissionCount,
            criticalPermissionCount = entity.criticalPermissionCount,
            installTime = entity.installTime,
            lastUpdateTime = entity.lastUpdateTime,
            lastUpdate = entity.lastUpdate,
            lastScannedTime = entity.lastScannedTime,
            lastScan = entity.lastScan,
            isSystemApp = entity.isSystemApp,
            targetSdkVersion = entity.targetSdkVersion,
            versionName = entity.versionName,
            versionCode = entity.versionCode,
            minSdkVersion = entity.minSdkVersion,
            permissionDensity = entity.permissionDensity,
            riskFactors = toString(entity.riskFactors),
            permissionChanges = toString(entity.permissionChanges),
            appSize = entity.appSize,
            lastUsedTime = entity.lastUsedTime,
            isEnabled = entity.isEnabled,
            hasInternetAccess = entity.hasInternetAccess,
            signatureHash = entity.signatureHash,
            trustScore = entity.trustScore
        )
    }

    private fun fromString(value: String): List<String> {
        return if (value.isBlank()) {
            emptyList()
        } else {
            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    private fun toString(list: List<String>): String {
        return list.joinToString(",")
    }

    private inline fun <reified T : Enum<T>> safeValueOf(name: String): T? {
        return try {
            enumValueOf<T>(name)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
