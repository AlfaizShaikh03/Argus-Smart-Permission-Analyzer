package com.yourname.smartpermissionanalyzer.di

import com.yourname.smartpermissionanalyzer.data.scanner.PermissionScannerImpl
import com.yourname.smartpermissionanalyzer.domain.analyzer.PermissionAnalyzer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ScannerModule {

    @Binds
    @Singleton
    abstract fun bindPermissionAnalyzer(
        impl: PermissionScannerImpl
    ): PermissionAnalyzer
}
