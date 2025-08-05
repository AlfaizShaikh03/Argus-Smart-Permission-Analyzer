package com.yourname.smartpermissionanalyzer.data.models

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Immutable

@Immutable
enum class RiskLevel(val label: String, val color: Color) {
    CRITICAL("Critical Risk", Color.Red),
    HIGH("High Risk", Color(0xFFFFA500)), // Orange
    MEDIUM("Medium Risk", Color(0xFFFFD700)), // Gold
    LOW("Low Risk", Color.Green)
}
