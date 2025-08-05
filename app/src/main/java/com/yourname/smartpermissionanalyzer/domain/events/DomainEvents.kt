package com.yourname.smartpermissionanalyzer.domain.events

import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity

sealed class DomainEvent {
    object PeriodicScanStarted : DomainEvent()
    object PeriodicScanRequested : DomainEvent()  // Added missing event
    data class ScanCompleted(val apps: List<AppEntity>) : DomainEvent()
    data class ScanFailed(val errorMessage: String) : DomainEvent()
    data class ThreatDetected(val app: AppEntity, val threats: List<String>) : DomainEvent()

    data class AppInstalled(val packageName: String) : DomainEvent()
    data class AppUninstalled(val packageName: String) : DomainEvent()
    data class AppUpdated(
        val packageName: String,
        val previousVersion: String,
        val newVersion: String
    ) : DomainEvent()

    data class UserFeedbackRecorded(
        val packageName: String,
        val feedback: String,
        val rating: Int
    ) : DomainEvent()

    data class LearningDataBackedUp(val timestamp: Long) : DomainEvent()

    data class LearningDataRestored(val timestamp: Long) : DomainEvent()
}
