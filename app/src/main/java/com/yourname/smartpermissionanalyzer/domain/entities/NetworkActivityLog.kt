package com.yourname.smartpermissionanalyzer.domain.entities

data class NetworkActivityLog(
    val id: String = "",
    val timestamp: Long,
    val bytesReceived: Long,
    val bytesSent: Long,
    val activeConnections: Int
)
