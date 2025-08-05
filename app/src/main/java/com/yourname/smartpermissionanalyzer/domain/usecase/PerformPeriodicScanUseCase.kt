package com.yourname.smartpermissionanalyzer.domain.usecase

import android.content.Context
import com.yourname.smartpermissionanalyzer.domain.analyzer.PermissionAnalyzer
import com.yourname.smartpermissionanalyzer.domain.repository.PermissionAnalyzerRepository
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.UserFeedback
import com.yourname.smartpermissionanalyzer.domain.entities.FeedbackType
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformPeriodicScanUseCase @Inject constructor(
    private val permissionAnalyzer: PermissionAnalyzer,
    private val repository: PermissionAnalyzerRepository,
    @ApplicationContext private val context: Context
) {

    suspend fun execute(): Result<List<AppEntity>> {
        return try {
            println("PerformPeriodicScanUseCase: Starting periodic scan...")

            val scanResult = permissionAnalyzer.scanInstalledApps()

            scanResult.fold(
                onSuccess = { newApps ->
                    println("PerformPeriodicScanUseCase: Periodic scan found ${newApps.size} apps")

                    // ✅ FIXED: Load existing user feedback and merge with new scan results
                    val existingApps = repository.getAnalysisResults().getOrNull() ?: emptyList()
                    val userFeedback = loadUserFeedback()

                    val mergedApps = mergeAppsWithUserFeedback(newApps, existingApps, userFeedback)

                    println("PerformPeriodicScanUseCase: Merged ${mergedApps.size} apps with user feedback")

                    // Save merged results to repository
                    repository.saveAnalysisResults(mergedApps)
                    Result.success(mergedApps)
                },
                onFailure = { error ->
                    println("PerformPeriodicScanUseCase: Periodic scan failed - ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("PerformPeriodicScanUseCase: Exception during periodic scan - ${e.message}")
            Result.failure(e)
        }
    }

    // ✅ NEW: Merge new scan results with existing user feedback
    private fun mergeAppsWithUserFeedback(
        newApps: List<AppEntity>,
        existingApps: List<AppEntity>,
        userFeedback: Map<String, UserFeedback>
    ): List<AppEntity> {
        return newApps.map { newApp ->
            val existingApp = existingApps.find { it.packageName == newApp.packageName }
            val feedback = userFeedback[newApp.packageName]

            if (feedback != null) {
                // User has provided feedback for this app
                val permissionsChanged = existingApp?.permissions != newApp.permissions

                if (permissionsChanged) {
                    // Permissions changed - recalculate base score but apply user feedback
                    println("PerformPeriodicScanUseCase: ${newApp.appName} permissions changed, recalculating with user feedback")

                    val adjustedRiskScore = when (feedback.type) {
                        FeedbackType.TRUSTED -> maxOf(5, newApp.riskScore - feedback.riskAdjustment)
                        FeedbackType.FLAGGED -> minOf(100, newApp.riskScore + feedback.riskAdjustment)
                    }

                    newApp.copy(
                        riskScore = adjustedRiskScore,
                        riskLevel = calculateRiskLevel(adjustedRiskScore),
                        trustScore = feedback.trustScore
                    )
                } else {
                    // Permissions unchanged - preserve existing scores completely
                    println("PerformPeriodicScanUseCase: ${newApp.appName} permissions unchanged, preserving user feedback")

                    existingApp?.copy(
                        // Update only metadata that might change without affecting scores
                        versionName = newApp.versionName,
                        versionCode = newApp.versionCode,
                        lastUpdateTime = newApp.lastUpdateTime,
                        appSize = newApp.appSize,
                        isEnabled = newApp.isEnabled,
                        lastScannedTime = System.currentTimeMillis()
                    ) ?: newApp
                }
            } else {
                // No user feedback - use fresh scan results
                println("PerformPeriodicScanUseCase: ${newApp.appName} no user feedback, using fresh scores")
                newApp
            }
        }
    }

    // ✅ NEW: Load user feedback from persistent storage
    private fun loadUserFeedback(): Map<String, UserFeedback> {
        return try {
            val sharedPrefs = context.getSharedPreferences("user_feedback", Context.MODE_PRIVATE)
            val feedbackData = sharedPrefs.getString("feedback_data", "") ?: ""
            val userFeedbackMap = mutableMapOf<String, UserFeedback>()

            if (feedbackData.isNotEmpty()) {
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
                println("PerformPeriodicScanUseCase: Loaded ${userFeedbackMap.size} user feedback entries")
            }

            userFeedbackMap
        } catch (e: Exception) {
            println("PerformPeriodicScanUseCase: Error loading user feedback: ${e.message}")
            emptyMap()
        }
    }

    // ✅ NEW: Calculate risk level from score
    private fun calculateRiskLevel(riskScore: Int): RiskLevelEntity {
        return when {
            riskScore >= 85 -> RiskLevelEntity.CRITICAL
            riskScore >= 70 -> RiskLevelEntity.HIGH
            riskScore >= 50 -> RiskLevelEntity.MEDIUM
            riskScore >= 30 -> RiskLevelEntity.LOW
            else -> RiskLevelEntity.MINIMAL
        }
    }
}
