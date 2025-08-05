package com.yourname.smartpermissionanalyzer.domain.entities

enum class PermissionCategoryEntity {
    CAMERA,
    LOCATION,
    MICROPHONE,
    STORAGE,
    CONTACTS,
    PHONE,
    SMS,
    CALENDAR,
    SENSORS,
    DEVICE_INFO,
    NETWORK,
    SYSTEM,
    COMMUNICATION,
    OTHER,
    UNKNOWN;

    companion object {
        fun fromString(value: String): PermissionCategoryEntity {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
