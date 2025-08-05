package com.yourname.smartpermissionanalyzer.di

import com.yourname.smartpermissionanalyzer.domain.events.DomainEventBus
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppInstallationReceiverEntryPoint {
    fun eventBus(): DomainEventBus
}
