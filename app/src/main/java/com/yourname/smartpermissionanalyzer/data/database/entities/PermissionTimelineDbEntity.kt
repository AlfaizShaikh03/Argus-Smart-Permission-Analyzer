package com.yourname.smartpermissionanalyzer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "permission_timeline",
    foreignKeys = [
        ForeignKey(
            entity = AppDatabaseEntity::class,
            parentColumns = ["packageName"],
            childColumns = ["package_name"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PermissionTimelineDbEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "app_name")
    val appName: String,
    @ColumnInfo(name = "permission_name")
    val permissionName: String,
    @ColumnInfo(name = "friendly_permission_name")
    val friendlyPermissionName: String,
    @ColumnInfo(name = "action")
    val action: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "previous_risk_level")
    val previousRiskLevel: String? = null,
    @ColumnInfo(name = "new_risk_level")
    val newRiskLevel: String? = null,
    @ColumnInfo(name = "category")
    val category: String = "",
    @ColumnInfo(name = "impact")
    val impact: String = "",
    @ColumnInfo(name = "context")
    val context: String = "",
    @ColumnInfo(name = "user_triggered")
    val userTriggered: Boolean = false
)
