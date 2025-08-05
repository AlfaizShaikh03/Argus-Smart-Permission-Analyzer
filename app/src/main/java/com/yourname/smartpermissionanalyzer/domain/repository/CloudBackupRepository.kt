package com.yourname.smartpermissionanalyzer.domain.repository

interface CloudBackupRepository {
    suspend fun backupData(): Result<Unit>
    suspend fun restoreData(): Result<Unit>
    suspend fun syncData(): Result<Unit>
}
