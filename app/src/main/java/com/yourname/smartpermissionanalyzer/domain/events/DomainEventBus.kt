package com.yourname.smartpermissionanalyzer.domain.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class DomainEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<DomainEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    // This is the correct method name - emit(), not publish()
    suspend fun emit(event: DomainEvent) = _events.emit(event)

    fun tryEmit(event: DomainEvent): Boolean = _events.tryEmit(event)
}
