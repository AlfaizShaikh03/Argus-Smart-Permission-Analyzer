package com.yourname.smartpermissionanalyzer.domain.entities

enum class PermissionActionEntity {
    GRANTED, DENIED, REQUESTED, REVOKED, UPDATED, SYSTEM_GRANTED, USER_DENIED;

    companion object {
        fun fromString(value: String): PermissionActionEntity =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DENIED
    }
}
