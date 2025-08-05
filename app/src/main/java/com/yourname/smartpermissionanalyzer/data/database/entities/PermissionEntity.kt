package com.yourname.smartpermissionanalyzer.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "permissions")
data class PermissionEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "packageName")
    val packageName: String,

    @ColumnInfo(name = "permissionName")
    val permissionName: String,

    @ColumnInfo(name = "isGranted")
    val isGranted: Boolean,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "permissionGroup")
    val permissionGroup: String? = null,

    @ColumnInfo(name = "protectionLevel")
    val protectionLevel: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "isCustom")
    val isCustom: Boolean = false,

    @ColumnInfo(name = "isDangerous")
    val isDangerous: Boolean = false
)
