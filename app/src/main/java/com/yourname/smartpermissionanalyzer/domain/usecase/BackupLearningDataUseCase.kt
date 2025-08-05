package com.yourname.smartpermissionanalyzer.domain.usecase

import com.yourname.smartpermissionanalyzer.domain.repository.CloudBackupRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupLearningDataUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository
) {
    suspend fun execute(): Result<Unit> {
        return cloudBackupRepository.backupData()
    }
}
