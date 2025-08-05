package com.yourname.smartpermissionanalyzer.domain.entities

// Extension properties for AppEntity to support the new features
val AppEntity.hasCamera: Boolean
    get() = permissions.any { it.contains("CAMERA", ignoreCase = true) }

val AppEntity.hasMicrophone: Boolean
    get() = permissions.any { it.contains("RECORD_AUDIO", ignoreCase = true) ||
            it.contains("MICROPHONE", ignoreCase = true) }

val AppEntity.hasLocation: Boolean
    get() = permissions.any { it.contains("ACCESS_FINE_LOCATION", ignoreCase = true) ||
            it.contains("ACCESS_COARSE_LOCATION", ignoreCase = true) }

val AppEntity.canAccessContacts: Boolean
    get() = permissions.any { it.contains("READ_CONTACTS", ignoreCase = true) }

val AppEntity.canAccessSMS: Boolean
    get() = permissions.any { it.contains("READ_SMS", ignoreCase = true) ||
            it.contains("SEND_SMS", ignoreCase = true) }

val AppEntity.canAccessCallLog: Boolean
    get() = permissions.any { it.contains("READ_CALL_LOG", ignoreCase = true) }

val AppEntity.hasStorageAccess: Boolean
    get() = permissions.any { it.contains("READ_EXTERNAL_STORAGE", ignoreCase = true) ||
            it.contains("WRITE_EXTERNAL_STORAGE", ignoreCase = true) }

fun AppEntity.isNavigationApp(): Boolean {
    return appCategory == AppCategoryEntity.MAPS_AND_NAVIGATION ||
            appName.contains("maps", ignoreCase = true) ||
            appName.contains("navigation", ignoreCase = true) ||
            appName.contains("gps", ignoreCase = true)
}
