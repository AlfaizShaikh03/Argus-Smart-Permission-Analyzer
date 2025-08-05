package com.yourname.smartpermissionanalyzer.ui.theme

import androidx.compose.ui.graphics.Color

// Security-themed color palette
object SecurityColors {
    // Risk Level Colors
    val CriticalRisk = Color(0xFFD32F2F)      // Red
    val HighRisk = Color(0xFFF57C00)          // Orange
    val MediumRisk = Color(0xFFFBC02D)        // Yellow
    val LowRisk = Color(0xFF388E3C)           // Green
    val SafeApp = Color(0xFF1976D2)           // Blue

    // Security Status Colors
    val Secure = Color(0xFF4CAF50)            // Green
    val Warning = Color(0xFFFF9800)           // Orange
    val Danger = Color(0xFFF44336)            // Red
    val Unknown = Color(0xFF9E9E9E)           // Gray

    // Background variations
    val SecureBackground = Color(0xFFE8F5E8)  // Light green
    val WarningBackground = Color(0xFFFFF3E0) // Light orange
    val DangerBackground = Color(0xFFFFEBEE)  // Light red
    val InfoBackground = Color(0xFFE3F2FD)    // Light blue

    // Permission Category Colors
    val CameraPermission = Color(0xFF9C27B0)  // Purple
    val LocationPermission = Color(0xFF3F51B5) // Indigo
    val ContactsPermission = Color(0xFF009688) // Teal
    val StoragePermission = Color(0xFF795548)  // Brown
    val NetworkPermission = Color(0xFF607D8B)  // Blue Gray
}
