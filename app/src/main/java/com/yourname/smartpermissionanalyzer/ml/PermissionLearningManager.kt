package com.yourname.smartpermissionanalyzer.ml

import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.repository.LearningDataRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class PermissionLearningManager @Inject constructor(
    private val learningDataRepository: LearningDataRepository
) {

    suspend fun analyzePermissionPatterns(apps: List<AppEntity>): Result<Map<String, Float>> {
        return try {
            val patterns = mutableMapOf<String, Float>()

            apps.forEach { app ->
                app.permissions.forEach { permission ->
                    val currentScore = patterns[permission] ?: 0f
                    patterns[permission] = currentScore + calculatePermissionWeight(permission, app)
                }
            }

            Result.success(patterns.toMap())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun predictRiskLevel(permissions: List<String>): Result<Float> {
        return try {
            var riskScore = 0f

            permissions.forEach { permission ->
                riskScore += when {
                    permission.contains("CAMERA", ignoreCase = true) -> 0.8f
                    permission.contains("LOCATION", ignoreCase = true) -> 0.9f
                    permission.contains("CONTACTS", ignoreCase = true) -> 0.7f
                    permission.contains("SMS", ignoreCase = true) -> 0.85f
                    permission.contains("CALL", ignoreCase = true) -> 0.75f
                    else -> 0.1f
                }
            }

            Result.success((riskScore / permissions.size.coerceAtLeast(1)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLearningModel(apps: List<AppEntity>): Result<Unit> {
        return try {
            learningDataRepository.collectLearningData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculatePermissionWeight(permission: String, app: AppEntity): Float {
        val baseWeight = when {
            permission.contains("CAMERA", ignoreCase = true) -> 3.0f
            permission.contains("LOCATION", ignoreCase = true) -> 3.5f
            permission.contains("CONTACTS", ignoreCase = true) -> 2.8f
            permission.contains("SMS", ignoreCase = true) -> 3.2f
            permission.contains("CALL", ignoreCase = true) -> 2.9f
            else -> 1.0f
        }

        // Adjust weight based on app category
        val categoryMultiplier = when (app.appCategory.name) {
            "SYSTEM" -> 0.5f
            "GAME" -> 1.2f
            "SOCIAL" -> 1.1f
            "FINANCE" -> 0.8f
            else -> 1.0f
        }

        return baseWeight * categoryMultiplier
    }
}
