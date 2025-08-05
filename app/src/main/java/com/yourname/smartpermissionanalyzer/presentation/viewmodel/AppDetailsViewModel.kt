package com.yourname.smartpermissionanalyzer.presentation.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.yourname.smartpermissionanalyzer.domain.repository.PermissionAnalyzerRepository
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity
import com.yourname.smartpermissionanalyzer.domain.entities.UserFeedback
import com.yourname.smartpermissionanalyzer.domain.entities.FeedbackType
import com.yourname.smartpermissionanalyzer.domain.recommendations.SmartRecommendationEngine
import com.yourname.smartpermissionanalyzer.domain.recommendations.SmartRecommendation
import com.yourname.smartpermissionanalyzer.domain.recommendations.UserSecurityPreferences
import com.yourname.smartpermissionanalyzer.domain.recommendations.SecurityLevel
import com.yourname.smartpermissionanalyzer.domain.optimization.SecurityOptimizer
import com.yourname.smartpermissionanalyzer.domain.optimization.OptimizationResult
import com.yourname.smartpermissionanalyzer.domain.privacy.PrivacyImpactCalculator
import com.yourname.smartpermissionanalyzer.domain.privacy.PrivacyScore
import com.yourname.smartpermissionanalyzer.data.export.ReportExporter
import javax.inject.Inject

@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    private val repository: PermissionAnalyzerRepository,
    private val smartRecommendationEngine: SmartRecommendationEngine,
    private val securityOptimizer: SecurityOptimizer,
    private val privacyImpactCalculator: PrivacyImpactCalculator,
    private val reportExporter: ReportExporter,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDetailsUiState())
    val uiState: StateFlow<AppDetailsUiState> = _uiState.asStateFlow()

    // Track user feedback for persistence
    private val userFeedbackMap = mutableMapOf<String, UserFeedback>()

    // ✅ NEW: Action completion flags for UI navigation
    private val _trustActionCompleted = MutableSharedFlow<Unit>()
    val trustActionCompleted = _trustActionCompleted.asSharedFlow()

    private val _flagActionCompleted = MutableSharedFlow<Unit>()
    val flagActionCompleted = _flagActionCompleted.asSharedFlow()

    fun loadAppDetails(packageName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = repository.getAppByPackageName(packageName)

                result.fold(
                    onSuccess = { appEntity: AppEntity? ->
                        if (appEntity != null) {
                            // Generate recommendations for single app
                            val userPreferences = getUserSecurityPreferences()
                            val allRecommendations = smartRecommendationEngine.generatePersonalizedRecommendations(
                                apps = listOf(appEntity),
                                userPreferences = userPreferences,
                                context = context
                            )

                            val optimizationResult = securityOptimizer.optimizeAppSecurity(appEntity)
                            val privacyScore = privacyImpactCalculator.calculatePrivacyScore(appEntity)

                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                appEntity = appEntity,
                                recommendations = allRecommendations,
                                optimizationResult = optimizationResult,
                                privacyScore = privacyScore,
                                error = null
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "App not found: $packageName"
                            )
                        }
                    },
                    onFailure = { exception: Throwable ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load app details: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error loading app: ${e.message}"
                )
            }
        }
    }

    private suspend fun getUserSecurityPreferences(): UserSecurityPreferences {
        val sharedPrefs = context.getSharedPreferences("smart_permission_analyzer_settings", Context.MODE_PRIVATE)

        return UserSecurityPreferences(
            securityLevel = when {
                sharedPrefs.getBoolean("high_security_mode", false) -> SecurityLevel.HIGH
                sharedPrefs.getBoolean("balanced_mode", true) -> SecurityLevel.BALANCED
                else -> SecurityLevel.PERMISSIVE
            },
            allowDataCollection = sharedPrefs.getBoolean("data_collection", false),
            autoOptimize = sharedPrefs.getBoolean("auto_optimize", false),
            privacyFocused = sharedPrefs.getBoolean("privacy_focused", true)
        )
    }

    fun markAppAsTrusted(packageName: String) {
        viewModelScope.launch {
            val currentApp = _uiState.value.appEntity
            if (currentApp != null) {
                try {
                    println("AppDetailsViewModel: Starting trust operation for ${currentApp.appName}")
                    println("AppDetailsViewModel: Current risk score: ${currentApp.riskScore}")

                    val newRiskScore = maxOf(5, currentApp.riskScore - 25)
                    val newTrustScore = minOf(1.0f, currentApp.trustScore + 0.3f)

                    val updatedApp = currentApp.copy(
                        riskScore = newRiskScore,
                        riskLevel = calculateRiskLevel(newRiskScore),
                        trustScore = newTrustScore
                    )

                    println("AppDetailsViewModel: New risk score: ${newRiskScore}")

                    // ✅ FIXED: Update UI state first so user sees immediate change
                    _uiState.value = _uiState.value.copy(
                        appEntity = updatedApp,
                        message = "✅ App marked as trusted"
                    )

                    // Save user feedback persistently
                    saveUserFeedback(packageName, UserFeedback(
                        type = FeedbackType.TRUSTED,
                        riskAdjustment = 25,
                        trustScore = newTrustScore,
                        timestamp = System.currentTimeMillis()
                    ))

                    // ✅ FIXED: Update in repository and wait for completion
                    val result = repository.updateApp(updatedApp)
                    result.fold(
                        onSuccess = { _: Unit ->
                            println("AppDetailsViewModel: Successfully saved updated app to repository")

                            // ✅ FIXED: Short delay to ensure database write completes
                            delay(200)

                            // Signal completion for navigation
                            _trustActionCompleted.emit(Unit)
                            println("AppDetailsViewModel: Trust operation completed successfully")
                        },
                        onFailure = { exception: Throwable ->
                            // Revert UI state on failure
                            _uiState.value = _uiState.value.copy(
                                appEntity = currentApp,
                                error = "Failed to mark app as trusted: ${exception.message}"
                            )
                            println("AppDetailsViewModel: Trust operation failed: ${exception.message}")
                        }
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Error during trust operation: ${e.message}"
                    )
                    println("AppDetailsViewModel: Trust operation exception: ${e.message}")
                }
            }
        }
    }

    fun flagAppAsRisky(packageName: String) {
        viewModelScope.launch {
            val currentApp = _uiState.value.appEntity
            if (currentApp != null) {
                try {
                    println("AppDetailsViewModel: Starting flag operation for ${currentApp.appName}")
                    println("AppDetailsViewModel: Current risk score: ${currentApp.riskScore}")

                    val newRiskScore = minOf(100, currentApp.riskScore + 25)
                    val newTrustScore = maxOf(0.0f, currentApp.trustScore - 0.2f)

                    val updatedApp = currentApp.copy(
                        riskScore = newRiskScore,
                        riskLevel = calculateRiskLevel(newRiskScore),
                        trustScore = newTrustScore
                    )

                    println("AppDetailsViewModel: New risk score: ${newRiskScore}")

                    // ✅ FIXED: Update UI state first so user sees immediate change
                    _uiState.value = _uiState.value.copy(
                        appEntity = updatedApp,
                        message = "⚠️ App flagged as risky"
                    )

                    // Save user feedback persistently
                    saveUserFeedback(packageName, UserFeedback(
                        type = FeedbackType.FLAGGED,
                        riskAdjustment = 25,
                        trustScore = newTrustScore,
                        timestamp = System.currentTimeMillis()
                    ))

                    // ✅ FIXED: Update in repository and wait for completion
                    val result = repository.updateApp(updatedApp)
                    result.fold(
                        onSuccess = { _: Unit ->
                            println("AppDetailsViewModel: Successfully saved updated app to repository")

                            // ✅ FIXED: Short delay to ensure database write completes
                            delay(200)

                            // Signal completion for navigation
                            _flagActionCompleted.emit(Unit)
                            println("AppDetailsViewModel: Flag operation completed successfully")
                        },
                        onFailure = { exception: Throwable ->
                            // Revert UI state on failure
                            _uiState.value = _uiState.value.copy(
                                appEntity = currentApp,
                                error = "Failed to flag app as risky: ${exception.message}"
                            )
                            println("AppDetailsViewModel: Flag operation failed: ${exception.message}")
                        }
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Error during flag operation: ${e.message}"
                    )
                    println("AppDetailsViewModel: Flag operation exception: ${e.message}")
                }
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

    // Save user feedback to persistent storage
    private fun saveUserFeedback(packageName: String, feedback: UserFeedback) {
        try {
            userFeedbackMap[packageName] = feedback

            val sharedPrefs = context.getSharedPreferences("user_feedback", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()

            // Load existing feedback
            val existingData = sharedPrefs.getString("feedback_data", "") ?: ""
            val existingMap = mutableMapOf<String, UserFeedback>()

            if (existingData.isNotEmpty()) {
                existingData.split("|").forEach { entry ->
                    if (entry.isNotEmpty()) {
                        val parts = entry.split(":")
                        if (parts.size == 5) {
                            existingMap[parts[0]] = UserFeedback(
                                type = if (parts[1] == "TRUSTED") FeedbackType.TRUSTED else FeedbackType.FLAGGED,
                                riskAdjustment = parts[2].toIntOrNull() ?: 25,
                                trustScore = parts[3].toFloatOrNull() ?: 0.5f,
                                timestamp = parts[4].toLongOrNull() ?: System.currentTimeMillis()
                            )
                        }
                    }
                }
            }

            // Add/update new feedback
            existingMap[packageName] = feedback

            // Save updated feedback
            val feedbackData = existingMap.map { (pkg, fb) ->
                "$pkg:${fb.type.name}:${fb.riskAdjustment}:${fb.trustScore}:${fb.timestamp}"
            }.joinToString("|")

            editor.putString("feedback_data", feedbackData)
            editor.apply()

            println("AppDetailsViewModel: Saved user feedback for $packageName")
        } catch (e: Exception) {
            println("AppDetailsViewModel: Error saving user feedback: ${e.message}")
        }
    }

    fun removeAppFromAnalysis() {
        viewModelScope.launch {
            val app = _uiState.value.appEntity
            if (app != null) {
                try {
                    println("AppDetailsViewModel: Starting removal of ${app.appName}")

                    // Show loading state
                    _uiState.value = _uiState.value.copy(isLoading = true)

                    // Remove app (this will delete from DB and add to exclusion list)
                    val result = repository.deleteApp(app.packageName)

                    result.fold(
                        onSuccess = {
                            println("AppDetailsViewModel: Successfully removed ${app.appName}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                message = "✅ ${app.appName} has been permanently removed from security analysis",
                                isRemoved = true
                            )

                            // Clear message after showing success
                            clearMessage()
                        },
                        onFailure = { exception ->
                            println("AppDetailsViewModel: Failed to remove app - ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "❌ Failed to remove app: ${exception.message}"
                            )
                        }
                    )
                } catch (e: Exception) {
                    println("AppDetailsViewModel: Exception during removal - ${e.message}")
                    e.printStackTrace()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "❌ Error during removal: ${e.message}"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "❌ No app data available for removal"
                )
            }
        }
    }

    fun exportAppReport() {
        viewModelScope.launch {
            val app = _uiState.value.appEntity
            if (app != null) {
                try {
                    _uiState.value = _uiState.value.copy(isLoading = true)

                    val success = reportExporter.exportAppReport(app, context)

                    if (success) {
                        _uiState.value = _uiState.value.copy(
                            message = "Report exported to Downloads folder",
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to export report",
                            isLoading = false
                        )
                    }
                    clearMessage()
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Export failed: ${e.message}",
                        isLoading = false
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "No app data available to export"
                )
            }
        }
    }

    fun getShareIntent(): Intent? {
        val app = _uiState.value.appEntity
        return if (app != null) {
            reportExporter.shareAppReport(app, context)
        } else {
            null
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ✅ FIXED: Only clear message for non-action related messages
    fun clearMessage() = viewModelScope.launch {
        delay(3000)
        val currentMessage = _uiState.value.message
        // Only clear messages that are not trust/flag action messages
        if (currentMessage != null &&
            !currentMessage.contains("trusted", ignoreCase = true) &&
            !currentMessage.contains("risky", ignoreCase = true)) {
            _uiState.value = _uiState.value.copy(message = null)
        }
    }

    // ✅ NEW: Manual message clearing for navigation
    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun getPermissionExplanation(permission: String): String {
        return when {
            permission.contains("CAMERA") -> "Allows the app to take pictures and record videos using the device camera"
            permission.contains("RECORD_AUDIO") -> "Allows the app to record audio using the device microphone"
            permission.contains("ACCESS_FINE_LOCATION") -> "Allows the app to access precise location information using GPS"
            permission.contains("ACCESS_COARSE_LOCATION") -> "Allows the app to access approximate location information"
            permission.contains("READ_CONTACTS") -> "Allows the app to read your contact list and personal information"
            permission.contains("WRITE_CONTACTS") -> "Allows the app to modify your contact list"
            permission.contains("READ_SMS") -> "Allows the app to read your text messages"
            permission.contains("SEND_SMS") -> "Allows the app to send text messages"
            permission.contains("READ_CALL_LOG") -> "Allows the app to read your call history"
            permission.contains("WRITE_CALL_LOG") -> "Allows the app to modify your call history"
            permission.contains("CALL_PHONE") -> "Allows the app to make phone calls"
            permission.contains("READ_PHONE_STATE") -> "Allows the app to access phone information and status"
            permission.contains("READ_EXTERNAL_STORAGE") -> "Allows the app to read files from external storage"
            permission.contains("WRITE_EXTERNAL_STORAGE") -> "Allows the app to write files to external storage"
            permission.contains("INTERNET") -> "Allows the app to access the internet and send data"
            permission.contains("ACCESS_NETWORK_STATE") -> "Allows the app to view network connection information"
            permission.contains("WAKE_LOCK") -> "Allows the app to prevent the device from sleeping"
            permission.contains("VIBRATE") -> "Allows the app to control device vibration"
            permission.contains("BLUETOOTH") -> "Allows the app to connect to Bluetooth devices"
            permission.contains("NFC") -> "Allows the app to use Near Field Communication"
            else -> "Standard Android permission: ${permission.substringAfterLast(".")}"
        }
    }
}

data class AppDetailsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val appEntity: AppEntity? = null,
    val recommendations: List<SmartRecommendation> = emptyList(),
    val optimizationResult: OptimizationResult? = null,
    val privacyScore: PrivacyScore? = null,
    val error: String? = null,
    val message: String? = null,
    val isRemoved: Boolean = false
)
