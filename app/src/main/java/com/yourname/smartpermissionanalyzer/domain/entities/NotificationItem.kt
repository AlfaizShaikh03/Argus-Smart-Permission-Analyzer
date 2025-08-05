package com.yourname.smartpermissionanalyzer.domain.entities

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val priority: NotificationPriority,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val actionRequired: Boolean = false
)

enum class NotificationType {
    SECURITY_ALERT,
    SCAN_COMPLETED,
    APP_INSTALLED,
    PERMISSION_CHANGED,
    THREAT_DETECTED,
    SYSTEM_UPDATE
}

enum class NotificationPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}
