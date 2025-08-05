package com.yourname.smartpermissionanalyzer.di

import com.yourname.smartpermissionanalyzer.domain.repository.PermissionAnalyzerRepository
import com.yourname.smartpermissionanalyzer.data.repository.PermissionAnalyzerRepositoryImpl // ✅ FIXED: Added missing import
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPermissionAnalyzerRepository(
        impl: PermissionAnalyzerRepositoryImpl // ✅ FIXED: Changed from PermissionAnalysisRepositoryImpl to PermissionAnalyzerRepositoryImpl
    ): PermissionAnalyzerRepository
}
