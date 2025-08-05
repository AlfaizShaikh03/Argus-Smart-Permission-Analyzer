package com.yourname.smartpermissionanalyzer.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.yourname.smartpermissionanalyzer.domain.usecase.PerformManualScanUseCase
import com.yourname.smartpermissionanalyzer.domain.repository.PermissionAnalyzerRepository
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity
import com.yourname.smartpermissionanalyzer.domain.entities.UserFeedback
import com.yourname.smartpermissionanalyzer.domain.entities.FeedbackType
import com.yourname.smartpermissionanalyzer.data.export.ReportExporter
import com.yourname.smartpermissionanalyzer.data.export.ReportFormat
import com.yourname.smartpermissionanalyzer.data.export.ExportResult
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val scanUseCase: PerformManualScanUseCase,
    private val repository: PermissionAnalyzerRepository,
    private val reportExporter: ReportExporter,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _scanProgress = MutableStateFlow(0)
    val scanProgress: StateFlow<Int> = _scanProgress.asStateFlow()

    private val _allApps = MutableStateFlow<List<AppEntity>>(emptyList())

    // Risk filter state
    private val _selectedRiskFilter = MutableStateFlow<RiskLevelEntity?>(null)
    val selectedRiskFilter: StateFlow<RiskLevelEntity?> = _selectedRiskFilter.asStateFlow()

    // Track user feedback to preserve during rescans
    private val userFeedbackMap = mutableMapOf<String, UserFeedback>()

    // ✅ FIXED: Add manual refresh trigger to force updates
    private val _manualRefreshTrigger = MutableStateFlow(0)

    // Combined filtering with search and risk level
    val filteredResults = combine(
        _allApps,
        _searchQuery,
        _selectedRiskFilter
    ) { apps, query, riskFilter ->
        apps.filter { app ->
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                app.appName.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true)
            }

            val matchesRiskFilter = riskFilter?.let { filter ->
                app.riskLevel == filter
            } ?: true

            matchesSearch && matchesRiskFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadExistingData()
        loadUserFeedback()

        // Listen for repository changes to refresh UI automatically
        observeRepositoryChanges()
    }

    // ✅ FIXED: Enhanced repository observation with manual refresh trigger
    private fun observeRepositoryChanges() {
        viewModelScope.launch {
            // Combine repository flow with manual refresh trigger
            combine(
                repository.observeAnalysisResults(),
                _manualRefreshTrigger
            ) { repoApps, _ ->
                repoApps
            }.collect { updatedApps ->
                if (updatedApps.isNotEmpty()) {
                    // Apply user feedback to updated apps
                    val mergedApps = mergeAppsWithUserFeedback(updatedApps)
                    _allApps.value = mergedApps
                    updateStateFromApps(mergedApps)
                    println("DashboardViewModel: UI refreshed with ${mergedApps.size} apps")
                }
            }
        }
    }

    // ✅ FIXED: Enhanced refresh method that forces data reload
    fun refreshData() {
        viewModelScope.launch {
            try {
                println("DashboardViewModel: Starting manual data refresh...")

                // Force reload from database
                val result = repository.getAnalysisResults()
                result.fold(
                    onSuccess = { apps ->
                        println("DashboardViewModel: Loaded ${apps.size} apps from database")
                        val mergedApps = mergeAppsWithUserFeedback(apps)
                        _allApps.value = mergedApps
                        updateStateFromApps(mergedApps)

                        // ✅ FIXED: Trigger manual refresh to update UI
                        _manualRefreshTrigger.value += 1

                        println("DashboardViewModel: Manual refresh completed with ${mergedApps.size} apps")

                        // Show brief confirmation
                        showMessage("🔄 Data refreshed")
                    },
                    onFailure = { error ->
                        println("DashboardViewModel: Failed to refresh data: ${error.message}")
                        _state.value = _state.value.copy(error = "Failed to refresh: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                println("DashboardViewModel: Error refreshing data: ${e.message}")
                _state.value = _state.value.copy(error = "Refresh error: ${e.message}")
            }
        }
    }

    fun startScan() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            _scanProgress.value = 0

            try {
                println("DashboardViewModel: Starting comprehensive scan...")

                // Show scanning progress
                for (i in 1..30) {
                    delay(50)
                    _scanProgress.value = i
                }

                val result = scanUseCase.execute()

                // Continue progress animation
                for (i in 31..90) {
                    delay(20)
                    _scanProgress.value = i
                }

                result.fold(
                    onSuccess = { newApps: List<AppEntity> ->
                        println("DashboardViewModel: Scan successful - ${newApps.size} apps found")

                        // Complete progress
                        _scanProgress.value = 100
                        delay(500)

                        // Merge new scan results with existing user feedback
                        val mergedApps = mergeAppsWithUserFeedback(newApps)

                        _allApps.value = mergedApps
                        updateStateFromApps(mergedApps)
                        _state.value = _state.value.copy(
                            isLoading = false,
                            lastScanTime = System.currentTimeMillis(),
                            message = "✅ Scan completed! Analyzed ${mergedApps.size} apps"
                        )
                        clearMessage()
                    },
                    onFailure = { error: Throwable ->
                        println("DashboardViewModel: Scan failed - ${error.message}")
                        error.printStackTrace()
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Scan failed: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                println("DashboardViewModel: Exception during scan - ${e.message}")
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error during scan: ${e.message}"
                )
            } finally {
                _scanProgress.value = 0
            }
        }
    }

    // Merge new scan results with preserved user feedback
    private suspend fun mergeAppsWithUserFeedback(newApps: List<AppEntity>): List<AppEntity> {
        return newApps.map { newApp ->
            val userFeedback = userFeedbackMap[newApp.packageName]

            if (userFeedback != null) {
                // Check if permissions have changed
                val oldApp = _allApps.value.find { it.packageName == newApp.packageName }
                val permissionsChanged = oldApp?.permissions != newApp.permissions

                if (permissionsChanged) {
                    // Permissions changed - recalculate but preserve user feedback influence
                    val baseRiskScore = newApp.riskScore
                    val adjustedRiskScore = when (userFeedback.type) {
                        FeedbackType.TRUSTED -> maxOf(10, baseRiskScore - userFeedback.riskAdjustment)
                        FeedbackType.FLAGGED -> minOf(100, baseRiskScore + userFeedback.riskAdjustment)
                    }

                    newApp.copy(
                        riskScore = adjustedRiskScore,
                        riskLevel = calculateRiskLevel(adjustedRiskScore),
                        trustScore = userFeedback.trustScore
                    )
                } else {
                    // Permissions unchanged - keep existing scores with updated metadata
                    oldApp?.copy(
                        // Update only metadata that might change
                        versionName = newApp.versionName,
                        versionCode = newApp.versionCode,
                        lastUpdateTime = newApp.lastUpdateTime,
                        appSize = newApp.appSize,
                        isEnabled = newApp.isEnabled
                    ) ?: newApp
                }
            } else {
                // No user feedback - use new scan results as-is
                newApp
            }
        }
    }

    // Calculate risk level from score
    private fun calculateRiskLevel(riskScore: Int): RiskLevelEntity {
        return when {
            riskScore >= 85 -> RiskLevelEntity.CRITICAL
            riskScore >= 70 -> RiskLevelEntity.HIGH
            riskScore >= 50 -> RiskLevelEntity.MEDIUM
            riskScore >= 30 -> RiskLevelEntity.LOW
            else -> RiskLevelEntity.MINIMAL
        }
    }

    fun searchApps(query: String) {
        _searchQuery.value = query
    }

    // Risk filter function
    fun setRiskFilter(riskLevel: RiskLevelEntity?) {
        _selectedRiskFilter.value = riskLevel
    }

    fun markAppAsTrusted(packageName: String) {
        viewModelScope.launch {
            try {
                val apps = _allApps.value.map { app ->
                    if (app.packageName == packageName) {
                        val newRiskScore = maxOf(10, app.riskScore - 25)
                        val newTrustScore = minOf(1.0f, app.trustScore + 0.3f)

                        // Store user feedback persistently
                        userFeedbackMap[packageName] = UserFeedback(
                            type = FeedbackType.TRUSTED,
                            riskAdjustment = 25,
                            trustScore = newTrustScore,
                            timestamp = System.currentTimeMillis()
                        )
                        saveUserFeedback()

                        app.copy(
                            riskScore = newRiskScore,
                            riskLevel = calculateRiskLevel(newRiskScore),
                            trustScore = newTrustScore
                        )
                    } else {
                        app
                    }
                }

                _allApps.value = apps
                updateStateFromApps(apps)

                val updatedApp = apps.find { it.packageName == packageName }
                updatedApp?.let { app ->
                    // ✅ FIXED: Save to repository and trigger refresh
                    repository.updateApp(app).fold(
                        onSuccess = {
                            _manualRefreshTrigger.value += 1
                            println("DashboardViewModel: App trusted and saved to repository")
                        },
                        onFailure = { error ->
                            println("DashboardViewModel: Failed to save trusted app: ${error.message}")
                        }
                    )
                }

                showMessage("✅ App marked as trusted")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to mark app as trusted: ${e.message}")
            }
        }
    }

    fun flagAppAsRisky(packageName: String) {
        viewModelScope.launch {
            try {
                val apps = _allApps.value.map { app ->
                    if (app.packageName == packageName) {
                        val newRiskScore = minOf(100, app.riskScore + 25)
                        val newTrustScore = maxOf(0.0f, app.trustScore - 0.2f)

                        // Store user feedback persistently
                        userFeedbackMap[packageName] = UserFeedback(
                            type = FeedbackType.FLAGGED,
                            riskAdjustment = 25,
                            trustScore = newTrustScore,
                            timestamp = System.currentTimeMillis()
                        )
                        saveUserFeedback()

                        app.copy(
                            riskScore = newRiskScore,
                            riskLevel = calculateRiskLevel(newRiskScore),
                            trustScore = newTrustScore
                        )
                    } else {
                        app
                    }
                }

                _allApps.value = apps
                updateStateFromApps(apps)

                val updatedApp = apps.find { it.packageName == packageName }
                updatedApp?.let { app ->
                    // ✅ FIXED: Save to repository and trigger refresh
                    repository.updateApp(app).fold(
                        onSuccess = {
                            _manualRefreshTrigger.value += 1
                            println("DashboardViewModel: App flagged and saved to repository")
                        },
                        onFailure = { error ->
                            println("DashboardViewModel: Failed to save flagged app: ${error.message}")
                        }
                    )
                }

                showMessage("⚠️ App flagged as risky")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to flag app: ${e.message}")
            }
        }
    }

    // Load user feedback from persistent storage
    private fun loadUserFeedback() {
        try {
            val sharedPrefs = context.getSharedPreferences("user_feedback", Context.MODE_PRIVATE)
            val feedbackData = sharedPrefs.getString("feedback_data", "") ?: ""

            if (feedbackData.isNotEmpty()) {
                // Parse the stored feedback data
                feedbackData.split("|").forEach { entry ->
                    if (entry.isNotEmpty()) {
                        val parts = entry.split(":")
                        if (parts.size == 5) {
                            val packageName = parts[0]
                            val type = if (parts[1] == "TRUSTED") FeedbackType.TRUSTED else FeedbackType.FLAGGED
                            val riskAdjustment = parts[2].toIntOrNull() ?: 25
                            val trustScore = parts[3].toFloatOrNull() ?: 0.5f
                            val timestamp = parts[4].toLongOrNull() ?: System.currentTimeMillis()

                            userFeedbackMap[packageName] = UserFeedback(
                                type = type,
                                riskAdjustment = riskAdjustment,
                                trustScore = trustScore,
                                timestamp = timestamp
                            )
                        }
                    }
                }
                println("DashboardViewModel: Loaded ${userFeedbackMap.size} user feedback entries")
            }
        } catch (e: Exception) {
            println("DashboardViewModel: Error loading user feedback: ${e.message}")
        }
    }

    // Save user feedback to persistent storage
    private fun saveUserFeedback() {
        try {
            val sharedPrefs = context.getSharedPreferences("user_feedback", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()

            // Convert feedback map to simple string format
            val feedbackData = userFeedbackMap.map { (packageName, feedback) ->
                "$packageName:${feedback.type.name}:${feedback.riskAdjustment}:${feedback.trustScore}:${feedback.timestamp}"
            }.joinToString("|")

            editor.putString("feedback_data", feedbackData)
            editor.apply()

            println("DashboardViewModel: Saved user feedback for ${userFeedbackMap.size} apps")
        } catch (e: Exception) {
            println("DashboardViewModel: Error saving user feedback: ${e.message}")
        }
    }

    // Export functions
    fun exportAnalysisData() {
        exportAnalysisDataAsText()
    }

    fun exportAnalysisDataAsCsv() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true)
                val apps = _allApps.value
                if (apps.isEmpty()) {
                    showMessage("❌ No data to export. Please run a scan first.")
                    return@launch
                }
                val exportResult = reportExporter.exportFullReportAsCsv(apps, context)
                if (exportResult) {
                    showMessage("📊 CSV report exported to Downloads folder")
                } else {
                    _state.value = _state.value.copy(error = "Failed to export CSV report")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "CSV export failed: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun exportAnalysisDataAsText() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true)
                val apps = _allApps.value
                if (apps.isEmpty()) {
                    showMessage("❌ No data to export. Please run a scan first.")
                    return@launch
                }
                val exportResult = reportExporter.exportFullReportAsText(apps, context)
                if (exportResult) {
                    showMessage("📄 Text report exported to Downloads folder")
                } else {
                    _state.value = _state.value.copy(error = "Failed to export text report")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Text export failed: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun exportAnalysisDataAsPdf() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true)
                val apps = _allApps.value
                if (apps.isEmpty()) {
                    showMessage("❌ No data to export. Please run a scan first.")
                    return@launch
                }
                val result = reportExporter.exportAdvancedReport(
                    apps = apps,
                    format = ReportFormat.PDF,
                    includeSettings = false,
                    settingsData = emptyMap(),
                    context = context
                )
                when (result) {
                    is ExportResult.Success -> {
                        showMessage("📄 PDF report exported to Downloads folder")
                    }
                    is ExportResult.Error -> {
                        _state.value = _state.value.copy(error = "PDF export failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "PDF export failed: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun exportData(format: ExportFormat = ExportFormat.TEXT) {
        when (format) {
            ExportFormat.CSV -> exportAnalysisDataAsCsv()
            ExportFormat.TEXT -> exportAnalysisDataAsText()
            ExportFormat.PDF -> exportAnalysisDataAsPdf()
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun loadExistingData() {
        viewModelScope.launch {
            try {
                val result = repository.getAnalysisResults()
                result.fold(
                    onSuccess = { apps: List<AppEntity> ->
                        if (apps.isNotEmpty()) {
                            val mergedApps = mergeAppsWithUserFeedback(apps)
                            _allApps.value = mergedApps
                            updateStateFromApps(mergedApps)
                            println("DashboardViewModel: Loaded ${mergedApps.size} apps from database")
                        }
                    },
                    onFailure = {
                        println("DashboardViewModel: No existing data found")
                    }
                )
            } catch (e: Exception) {
                println("DashboardViewModel: Error loading data - ${e.message}")
            }
        }
    }

    private fun updateStateFromApps(apps: List<AppEntity>) {
        val highRiskApps = apps.count {
            it.riskLevel == RiskLevelEntity.HIGH || it.riskLevel == RiskLevelEntity.CRITICAL
        }

        _state.value = _state.value.copy(
            totalAppsScanned = apps.size,
            highRiskApps = highRiskApps
        )
    }

    private fun showMessage(message: String) {
        _state.value = _state.value.copy(message = message)
        clearMessage()
    }

    private fun clearMessage() = viewModelScope.launch {
        delay(2000) // ✅ FIXED: Shorter delay for refresh message
        _state.value = _state.value.copy(message = null)
    }
}

enum class ExportFormat {
    CSV, TEXT, PDF
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val totalAppsScanned: Int = 0,
    val highRiskApps: Int = 0,
    val lastScanTime: Long = 0,
    val error: String? = null,
    val message: String? = null
)
