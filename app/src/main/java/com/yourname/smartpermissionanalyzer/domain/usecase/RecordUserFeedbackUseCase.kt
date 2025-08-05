package com.yourname.smartpermissionanalyzer.domain.usecase

import android.content.Context
import com.yourname.smartpermissionanalyzer.domain.repository.PermissionAnalyzerRepository
import com.yourname.smartpermissionanalyzer.domain.entities.UserFeedback
import com.yourname.smartpermissionanalyzer.domain.entities.FeedbackType
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordUserFeedbackUseCase @Inject constructor(
    private val repository: PermissionAnalyzerRepository,
    @ApplicationContext private val context: Context
) {

    // ✅ NEW: Record trust feedback
    suspend fun recordTrustFeedback(packageName: String): Result<Unit> {
        return try {
            println("RecordUserFeedbackUseCase: Recording trust feedback for $packageName")

            // Get current app data
            val appResult = repository.getAppByPackageName(packageName)

            appResult.fold(
                onSuccess = { app ->
                    if (app != null) {
                        // Calculate new scores
                        val newRiskScore = maxOf(5, app.riskScore - 25)
                        val newTrustScore = minOf(1.0f, app.trustScore + 0.3f)

                        // Create user feedback record
                        val feedback = UserFeedback(
                            type = FeedbackType.TRUSTED,
                            riskAdjustment = 25,
                            trustScore = newTrustScore,
                            timestamp = System.currentTimeMillis()
                        )

                        // Save feedback to persistent storage
                        saveFeedbackToPersistentStorage(packageName, feedback)

                        // Update app in repository
                        val updatedApp = app.copy(
                            riskScore = newRiskScore,
                            riskLevel = calculateRiskLevel(newRiskScore),
                            trustScore = newTrustScore
                        )

                        repository.updateApp(updatedApp)

                        println("RecordUserFeedbackUseCase: Successfully recorded trust feedback for ${app.appName}")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("App not found: $packageName"))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("RecordUserFeedbackUseCase: Error recording trust feedback: ${e.message}")
            Result.failure(e)
        }
    }

    // ✅ NEW: Record risky feedback
    suspend fun recordRiskyFeedback(packageName: String): Result<Unit> {
        return try {
            println("RecordUserFeedbackUseCase: Recording risky feedback for $packageName")

            // Get current app data
            val appResult = repository.getAppByPackageName(packageName)

            appResult.fold(
                onSuccess = { app ->
                    if (app != null) {
                        // Calculate new scores
                        val newRiskScore = minOf(100, app.riskScore + 25)
                        val newTrustScore = maxOf(0.0f, app.trustScore - 0.2f)

                        // Create user feedback record
                        val feedback = UserFeedback(
                            type = FeedbackType.FLAGGED,
                            riskAdjustment = 25,
                            trustScore = newTrustScore,
                            timestamp = System.currentTimeMillis()
                        )

                        // Save feedback to persistent storage
                        saveFeedbackToPersistentStorage(packageName, feedback)

                        // Update app in repository
                        val updatedApp = app.copy(
                            riskScore = newRiskScore,
                            riskLevel = calculateRiskLevel(newRiskScore),
                            trustScore = newTrustScore
                        )

                        repository.updateApp(updatedApp)

                        println("RecordUserFeedbackUseCase: Successfully recorded risky feedback for ${app.appName}")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("App not found: $packageName"))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("RecordUserFeedbackUseCase: Error recording risky feedback: ${e.message}")
            Result.failure(e)
        }
    }

    // ✅ NEW: Record general feedback with rating
    suspend fun recordGeneralFeedback(packageName: String, feedback: String, rating: Int): Result<Unit> {
        return try {
            println("RecordUserFeedbackUseCase: Recording general feedback for $packageName - Rating: $rating")

            // Determine feedback type based on rating
            val feedbackType = when {
                rating >= 4 -> FeedbackType.TRUSTED  // 4-5 stars = trusted
                rating <= 2 -> FeedbackType.FLAGGED  // 1-2 stars = risky
                else -> null  // 3 stars = neutral, no action
            }

            if (feedbackType != null) {
                // Apply feedback based on rating
                when (feedbackType) {
                    FeedbackType.TRUSTED -> recordTrustFeedback(packageName)
                    FeedbackType.FLAGGED -> recordRiskyFeedback(packageName)
                }
            } else {
                // Just save the general feedback without changing scores
                saveGeneralFeedbackToStorage(packageName, feedback, rating)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            println("RecordUserFeedbackUseCase: Error recording general feedback: ${e.message}")
            Result.failure(e)
        }
    }

    // ✅ NEW: Get user feedback for an app
    suspend fun getUserFeedback(packageName: String): Result<UserFeedback?> {
        return try {
            val feedback = loadFeedbackFromPersistentStorage(packageName)
            Result.success(feedback)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ NEW: Remove user feedback (reset to original scores)
    suspend fun removeFeedback(packageName: String): Result<Unit> {
        return try {
            println("RecordUserFeedbackUseCase: Removing feedback for $packageName")

            // Remove from persistent storage
            removeFeedbackFromPersistentStorage(packageName)

            // Get app and reset to default analysis scores
            val appResult = repository.getAppByPackageName(packageName)
            appResult.fold(
                onSuccess = { app ->
                    if (app != null) {
                        // Note: This would ideally recalculate the original risk score
                        // For now, we'll reset trust score and recalculate risk level
                        val updatedApp = app.copy(
                            trustScore = 0.5f  // Reset to neutral
                            // Risk score would need to be recalculated from original analysis
                        )

                        repository.updateApp(updatedApp)
                        println("RecordUserFeedbackUseCase: Successfully removed feedback for ${app.appName}")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("App not found: $packageName"))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("RecordUserFeedbackUseCase: Error removing feedback: ${e.message}")
            Result.failure(e)
        }
    }

    // ✅ PRIVATE: Save feedback to persistent storage
    private fun saveFeedbackToPersistentStorage(packageName: String, feedback: UserFeedback) {
        try {
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

            println("RecordUserFeedbackUseCase: Saved feedback to persistent storage for $packageName")
        } catch (e: Exception) {
            println("RecordUserFeedbackUseCase: Error saving feedback: ${e.message}")
        }
    }

    // ✅ PRIVATE: Load feedback from persistent storage
    private fun loadFeedbackFromPersistentStorage(packageName: String): UserFeedback? {
        return try {
            val sharedPrefs = context.getSharedPreferences("user_feedback", Context.MODE_PRIVATE)
            val feedbackData = sharedPrefs.getString("feedback_data", "") ?: ""

            if (feedbackData.isNotEmpty()) {
                feedbackData.split("|").forEach { entry ->
                    if (entry.isNotEmpty()) {
                        val parts = entry.split(":")
                        if (parts.size == 5 && parts[0] == packageName) {
                            return UserFeedback(
                                type = if (parts[1] == "TRUSTED") FeedbackType.TRUSTED else FeedbackType.FLAGGED,
                                riskAdjustment = parts[2].toIntOrNull() ?: 25,
                                trustScore = parts[3].toFloatOrNull() ?: 0.5f,
                                timestamp = parts[4].toLongOrNull() ?: System.currentTimeMillis()
                            )
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            println("RecordUserFeedbackUseCase: Error loading feedback: ${e.message}")
            null
        }
    }

    // ✅ PRIVATE: Remove feedback from persistent storage
    private fun removeFeedbackFromPersistentStorage(packageName: String) {
        try {
            val sharedPrefs = context.getSharedPreferences("user_feedback", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()

            // Load existing feedback
            val existingData = sharedPrefs.getString("feedback_data", "") ?: ""
            val existingMap = mutableMapOf<String, UserFeedback>()

            if (existingData.isNotEmpty()) {
                existingData.split("|").forEach { entry ->
                    if (entry.isNotEmpty()) {
                        val parts = entry.split(":")
                        if (parts.size == 5 && parts[0] != packageName) {  // Exclude the package we want to remove
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

            // Save updated feedback (without the removed package)
            val feedbackData = existingMap.map { (pkg, fb) ->
                "$pkg:${fb.type.name}:${fb.riskAdjustment}:${fb.trustScore}:${fb.timestamp}"
            }.joinToString("|")

            editor.putString("feedback_data", feedbackData)
            editor.apply()

            println("RecordUserFeedbackUseCase: Removed feedback from persistent storage for $packageName")
        } catch (e: Exception) {
            println("RecordUserFeedbackUseCase: Error removing feedback: ${e.message}")
        }
    }

    // ✅ PRIVATE: Save general feedback to separate storage
    private fun saveGeneralFeedbackToStorage(packageName: String, feedback: String, rating: Int) {
        try {
            val sharedPrefs = context.getSharedPreferences("general_feedback", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()

            val feedbackEntry = "$packageName:$rating:${System.currentTimeMillis()}:$feedback"
            val existingFeedback = sharedPrefs.getString("feedback_entries", "") ?: ""

            val updatedFeedback = if (existingFeedback.isEmpty()) {
                feedbackEntry
            } else {
                "$existingFeedback|$feedbackEntry"
            }

            editor.putString("feedback_entries", updatedFeedback)
            editor.apply()

            println("RecordUserFeedbackUseCase: Saved general feedback for $packageName")
        } catch (e: Exception) {
            println("RecordUserFeedbackUseCase: Error saving general feedback: ${e.message}")
        }
    }

    // ✅ PRIVATE: Calculate risk level from score
    private fun calculateRiskLevel(riskScore: Int): RiskLevelEntity {
        return when {
            riskScore >= 85 -> RiskLevelEntity.CRITICAL
            riskScore >= 70 -> RiskLevelEntity.HIGH
            riskScore >= 50 -> RiskLevelEntity.MEDIUM
            riskScore >= 30 -> RiskLevelEntity.LOW
            else -> RiskLevelEntity.MINIMAL
        }
    }

    // ✅ LEGACY: Support for old method signature
    suspend fun execute(packageName: String, feedback: String, rating: Int): Result<Unit> {
        return recordGeneralFeedback(packageName, feedback, rating)
    }
}
