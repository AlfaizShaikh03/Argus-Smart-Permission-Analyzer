package com.yourname.smartpermissionanalyzer.presentation.events

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.yourname.smartpermissionanalyzer.data.models.AppPermissionInfo
import com.yourname.smartpermissionanalyzer.presentation.viewmodel.LearningStats

sealed class UiState {

    @Stable
    @Immutable
    data class DashboardUiState(
        val isLoading: Boolean = false,
        val isScanning: Boolean = false,
        val analysisResults: List<AppPermissionInfo> = emptyList(),
        val error: String? = null,
        val message: String? = null,
        val scanProgress: Int = 0,
        val totalAppsToScan: Int = 0,
        val isBackingUp: Boolean = false,
        val isRestoring: Boolean = false,
        val lastBackupId: String? = null,
        val backgroundMonitoringEnabled: Boolean = false,
        val learningStats: LearningStats = LearningStats()
    )

    @Stable
    @Immutable
    data class PermissionTimelineUiState(
        val isLoading: Boolean = false,
        val timelineEntries: List<PermissionTimelineEntry> = emptyList(),
        val error: String? = null
    )

    @Stable
    @Immutable
    data class PermissionTimelineEntry(
        val packageName: String,
        val appName: String,
        val permissionName: String,
        val action: String,
        val timestamp: Long,
        val riskChange: String?
    )
}
