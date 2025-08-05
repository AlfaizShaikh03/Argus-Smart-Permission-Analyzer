package com.yourname.smartpermissionanalyzer.domain.repository

interface LearningDataRepository {
    suspend fun collectLearningData(): Result<Unit>
    suspend fun uploadLearningData(): Result<Unit>
    suspend fun processLearningData(): Result<Unit>
}
