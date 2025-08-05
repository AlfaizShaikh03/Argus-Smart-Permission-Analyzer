package com.yourname.smartpermissionanalyzer.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.yourname.smartpermissionanalyzer.domain.entities.NotificationItem
import com.yourname.smartpermissionanalyzer.domain.entities.NotificationType
import com.yourname.smartpermissionanalyzer.domain.entities.NotificationPriority
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity
import com.yourname.smartpermissionanalyzer.domain.repository.PermissionAnalyzerRepository
import com.yourname.smartpermissionanalyzer.domain.monitoring.SecurityAlert
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PermissionAnalyzerRepository
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "argus_notifications_state"
        private const val KEY_READ_NOTIFICATIONS = "read_notifications"
        private const val KEY_DELETED_NOTIFICATIONS = "deleted_notifications"
        private const val TAG = "NotificationsViewModel"
    }

    // ✅ FIXED: Add SharedPreferences for persistent state
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ✅ FIXED: Mutable notifications that preserve user interactions
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    private val notifications = _notifications.asStateFlow()

    private val _selectedFilter = MutableStateFlow<NotificationType?>(null)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _showOnlyUnread = MutableStateFlow(false)
    val showOnlyUnread = _showOnlyUnread.asStateFlow()

    // ✅ FIXED: Track user interactions with persistent storage
    private val readNotifications = mutableSetOf<String>()
    private val deletedNotifications = mutableSetOf<String>()
    private var lastKnownApps: List<AppEntity> = emptyList()
    private var scanCompletedNotified = false

    val filteredNotifications = combine(
        notifications,
        selectedFilter,
        showOnlyUnread
    ) { notificationList, filter, unreadOnly ->
        notificationList
            .filter { notification ->
                val matchesFilter = filter?.let { it == notification.type } ?: true
                val matchesUnread = if (unreadOnly) !notification.isRead else true
                matchesFilter && matchesUnread
            }
            .sortedByDescending { it.timestamp }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ✅ FIXED: Immediate state updates with proper logging
    val unreadCount = notifications.map { notificationList ->
        val count = notificationList.count { !it.isRead }
        android.util.Log.d(TAG, "Unread count calculated: $count from ${notificationList.size} notifications")
        count
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly, // ✅ FIXED: Changed to Eagerly for immediate updates
        initialValue = 0
    )

    init {
        android.util.Log.d(TAG, "NotificationsViewModel initialized")
        // ✅ FIXED: Load persistent state first, then load real notifications only
        loadPersistedState()
        // ✅ REMOVED: generateTestNotifications() - No more dummy notifications!
        loadRealNotifications()
    }

    // ✅ REMOVED: generateTestNotifications() function completely

    // ✅ NEW: Load persisted read/deleted state
    private fun loadPersistedState() {
        try {
            android.util.Log.d(TAG, "Loading persisted state...")

            // Load read notifications
            val readNotificationsString = sharedPreferences.getString(KEY_READ_NOTIFICATIONS, "") ?: ""
            if (readNotificationsString.isNotEmpty()) {
                readNotifications.addAll(readNotificationsString.split(",").filter { it.isNotBlank() })
            }

            // Load deleted notifications
            val deletedNotificationsString = sharedPreferences.getString(KEY_DELETED_NOTIFICATIONS, "") ?: ""
            if (deletedNotificationsString.isNotEmpty()) {
                deletedNotifications.addAll(deletedNotificationsString.split(",").filter { it.isNotBlank() })
            }

            android.util.Log.d(TAG, "Loaded ${readNotifications.size} read notifications, ${deletedNotifications.size} deleted")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading persisted state", e)
        }
    }

    // ✅ NEW: Save state to persistent storage
    private fun savePersistedState() {
        try {
            android.util.Log.d(TAG, "Saving persisted state...")
            sharedPreferences.edit().apply {
                putString(KEY_READ_NOTIFICATIONS, readNotifications.joinToString(","))
                putString(KEY_DELETED_NOTIFICATIONS, deletedNotifications.joinToString(","))
                apply()
            }
            android.util.Log.d(TAG, "State saved successfully")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error saving persisted state", e)
        }
    }

    // ✅ REAL: Generate notifications from actual app analysis ONLY
    private fun loadRealNotifications() {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "Loading real notifications...")

                // Get current analysis results
                val analysisResult = repository.getAnalysisResults()
                analysisResult.fold(
                    onSuccess = { currentApps ->
                        if (currentApps.isNotEmpty()) {
                            android.util.Log.d(TAG, "Found ${currentApps.size} apps to analyze for notifications")
                            generateNotificationsFromAnalysis(currentApps)
                        } else {
                            android.util.Log.d(TAG, "No apps found, setting empty notifications")
                            _notifications.value = emptyList() // ✅ FIXED: No dummy notifications
                        }
                    },
                    onFailure = {
                        android.util.Log.d(TAG, "No analysis data yet, setting empty notifications")
                        _notifications.value = emptyList() // ✅ FIXED: No dummy notifications
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading real notifications", e)
                _notifications.value = emptyList() // ✅ FIXED: No dummy notifications
            }
        }
    }

    // ✅ FIXED: Generate notifications while preserving user interactions
    private suspend fun generateNotificationsFromAnalysis(apps: List<AppEntity>) {
        val currentNotifications = mutableListOf<NotificationItem>()
        val currentTime = System.currentTimeMillis()

        android.util.Log.d(TAG, "Generating notifications from ${apps.size} apps...")

        // 1. ✅ REAL: Scan completion notification
        val highRiskCount = apps.count {
            it.riskLevel == RiskLevelEntity.HIGH || it.riskLevel == RiskLevelEntity.CRITICAL
        }
        val criticalCount = apps.count { it.riskLevel == RiskLevelEntity.CRITICAL }

        val scanNotificationId = "scan_complete_${currentTime / 60000}" // Group by minute
        if (!deletedNotifications.contains(scanNotificationId)) {
            currentNotifications.add(
                NotificationItem(
                    id = scanNotificationId,
                    type = NotificationType.SCAN_COMPLETED,
                    priority = if (criticalCount > 0) NotificationPriority.HIGH else NotificationPriority.MEDIUM,
                    title = "Security Scan Complete",
                    message = "Scanned ${apps.size} apps. Found $highRiskCount apps with elevated risk scores. " +
                            "${apps.size - highRiskCount} apps are secure.",
                    timestamp = currentTime,
                    isRead = readNotifications.contains(scanNotificationId),
                    actionRequired = highRiskCount > 0 && !readNotifications.contains(scanNotificationId)
                )
            )
        }

        // 2. ✅ REAL: Critical risk app alerts
        val criticalApps = apps.filter { it.riskLevel == RiskLevelEntity.CRITICAL && !it.isSystemApp }.take(3)
        criticalApps.forEachIndexed { index, app ->
            val notificationId = "critical_${app.packageName}"
            if (!deletedNotifications.contains(notificationId)) {
                currentNotifications.add(
                    NotificationItem(
                        id = notificationId,
                        type = NotificationType.THREAT_DETECTED,
                        priority = NotificationPriority.CRITICAL,
                        title = "Critical Risk App Detected",
                        message = "${app.appName} has a critical risk score (${app.riskScore}/100). " +
                                "Review ${app.suspiciousPermissionCount} suspicious permissions immediately.",
                        timestamp = currentTime - (index * 1000),
                        isRead = readNotifications.contains(notificationId),
                        actionRequired = !readNotifications.contains(notificationId)
                    )
                )
            }
        }

        // 3. ✅ REAL: High risk app alerts
        val highRiskApps = apps.filter {
            it.riskLevel == RiskLevelEntity.HIGH && !it.isSystemApp && it.riskLevel != RiskLevelEntity.CRITICAL
        }.take(3)

        highRiskApps.forEachIndexed { index, app ->
            val notificationId = "high_risk_${app.packageName}"
            if (!deletedNotifications.contains(notificationId)) {
                currentNotifications.add(
                    NotificationItem(
                        id = notificationId,
                        type = NotificationType.SECURITY_ALERT,
                        priority = NotificationPriority.HIGH,
                        title = "High Risk App Found",
                        message = "${app.appName} has high risk permissions including " +
                                "${app.criticalPermissionCount} critical permissions. Risk score: ${app.riskScore}/100.",
                        timestamp = currentTime - ((index + criticalApps.size) * 2000),
                        isRead = readNotifications.contains(notificationId),
                        actionRequired = !readNotifications.contains(notificationId)
                    )
                )
            }
        }

        // ✅ FIXED: Update notifications while preserving user interactions
        val finalNotifications = currentNotifications.sortedByDescending { it.timestamp }
        _notifications.value = finalNotifications

        android.util.Log.d(TAG, "Generated ${finalNotifications.size} notifications from analysis, unread: ${finalNotifications.count { !it.isRead }}")
    }

    // ✅ ENHANCED: Better action handling for real notifications
    fun handleNotificationAction(notification: NotificationItem, context: Context) {
        viewModelScope.launch {
            when (notification.type) {
                NotificationType.SECURITY_ALERT, NotificationType.THREAT_DETECTED -> {
                    Toast.makeText(context, "🔍 Reviewing security analysis...", Toast.LENGTH_SHORT).show()
                    markAsRead(notification.id)
                }

                NotificationType.PERMISSION_CHANGED -> {
                    Toast.makeText(context, "🔒 Opening permission review...", Toast.LENGTH_SHORT).show()
                    markAsRead(notification.id)
                }

                NotificationType.APP_INSTALLED -> {
                    Toast.makeText(context, "📱 Opening app analysis...", Toast.LENGTH_SHORT).show()
                    markAsRead(notification.id)
                }

                NotificationType.SCAN_COMPLETED -> {
                    Toast.makeText(context, "📊 Opening scan results...", Toast.LENGTH_SHORT).show()
                    markAsRead(notification.id)
                }

                NotificationType.SYSTEM_UPDATE -> {
                    Toast.makeText(context, "⚙️ Checking for updates...", Toast.LENGTH_SHORT).show()
                    try {
                        val intent = Intent("android.settings.SYSTEM_UPDATE_SETTINGS")
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            val fallbackIntent = Intent(android.provider.Settings.ACTION_SETTINGS)
                            fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(fallbackIntent)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Unable to open system settings", Toast.LENGTH_SHORT).show()
                    }
                    markAsRead(notification.id)
                }
            }
        }
    }

    // ✅ FIXED: Refresh without regenerating dummy data
    fun refreshNotifications() {
        android.util.Log.d(TAG, "Refreshing notifications...")
        viewModelScope.launch {
            // ✅ FIXED: Always load real notifications, no test data
            loadRealNotifications()
        }
    }

    // ✅ PUBLIC: Clear all notifications
    fun clearAllNotifications() {
        viewModelScope.launch {
            android.util.Log.d(TAG, "Clearing all notifications")
            _notifications.value = emptyList()
            readNotifications.clear()
            deletedNotifications.clear()
            savePersistedState()
        }
    }

    // ✅ FIXED: Filter methods that trigger recomposition
    fun setFilter(type: NotificationType?) {
        android.util.Log.d(TAG, "Setting filter: $type")
        _selectedFilter.value = type
    }

    fun toggleUnreadFilter() {
        val newValue = !_showOnlyUnread.value
        android.util.Log.d(TAG, "Toggling unread filter: $newValue")
        _showOnlyUnread.value = newValue
    }

    // ✅ FIXED: Mark as read with immediate state updates and proper persistent state tracking
    fun markAsRead(notificationId: String) {
        android.util.Log.d(TAG, "Marking notification as read: $notificationId")
        viewModelScope.launch {
            // Track this notification as read
            readNotifications.add(notificationId)

            // ✅ FIXED: Save to persistent storage immediately
            savePersistedState()

            // ✅ FIXED: Update the current notification list immediately
            val updatedNotifications = _notifications.value.map { notification ->
                if (notification.id == notificationId) {
                    notification.copy(isRead = true, actionRequired = false)
                } else {
                    notification
                }
            }
            _notifications.value = updatedNotifications

            // ✅ FIXED: Force immediate state update
            val newUnreadCount = updatedNotifications.count { !it.isRead }
            android.util.Log.d(TAG, "Notification marked as read. New unread count: $newUnreadCount")
        }
    }

    // ✅ FIXED: Mark all as read with immediate state updates
    fun markAllAsRead() {
        android.util.Log.d(TAG, "Marking all notifications as read")
        viewModelScope.launch {
            // Track all notifications as read
            _notifications.value.forEach { notification ->
                readNotifications.add(notification.id)
            }

            // ✅ FIXED: Save to persistent storage immediately
            savePersistedState()

            // ✅ FIXED: Update all notifications to read immediately
            val updatedNotifications = _notifications.value.map { notification ->
                notification.copy(isRead = true, actionRequired = false)
            }
            _notifications.value = updatedNotifications

            android.util.Log.d(TAG, "All notifications marked as read")
        }
    }

    // ✅ FIXED: Delete notification with immediate state updates
    fun deleteNotification(notificationId: String) {
        android.util.Log.d(TAG, "Deleting notification: $notificationId")
        viewModelScope.launch {
            // Track this notification as deleted
            deletedNotifications.add(notificationId)
            readNotifications.remove(notificationId) // Remove from read tracking too

            // ✅ FIXED: Save to persistent storage immediately
            savePersistedState()

            // ✅ FIXED: Remove from current notification list immediately
            val updatedNotifications = _notifications.value.filter { it.id != notificationId }
            _notifications.value = updatedNotifications

            android.util.Log.d(TAG, "Notification deleted. Remaining: ${updatedNotifications.size}")
        }
    }
}
