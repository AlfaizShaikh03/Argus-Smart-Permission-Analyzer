package com.yourname.smartpermissionanalyzer.presentation.events

import com.yourname.smartpermissionanalyzer.domain.entities.AppCategoryEntity

sealed class UiEvent {
    object StartManualScan : UiEvent()
    object StartPeriodicScan : UiEvent()
    data class RecordUserFeedback(val packageName: String, val isTrusted: Boolean) : UiEvent()
    object BackupLearningData : UiEvent()
    data class RestoreLearningData(val backupId: String) : UiEvent()
    data class EnableBackgroundMonitoring(val enabled: Boolean) : UiEvent()
    data class FilterByCategory(val category: AppCategoryEntity?) : UiEvent()
    data class SearchApps(val query: String) : UiEvent()
    data class ViewPermissionTimeline(val packageName: String) : UiEvent()
    data class RefreshAppAnalysis(val packageName: String) : UiEvent()
    data class SetScanFrequency(val hours: Int) : UiEvent()
    data class SetRequireCharging(val require: Boolean) : UiEvent()
    data class SetRequireWifi(val require: Boolean) : UiEvent()
    object ClearLearningData : UiEvent()
    data class NavigateToPermissionTimeline(val packageName: String) : UiEvent()
    object NavigateToSettings : UiEvent()
    object ShowRestoreDialog : UiEvent()
    data class ExportData(val format: String) : UiEvent()
    data class ImportData(val data: String) : UiEvent()
    data class ViewAppDetails(val packageName: String) : UiEvent()
}
