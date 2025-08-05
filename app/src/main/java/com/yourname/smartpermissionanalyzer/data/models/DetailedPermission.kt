package com.yourname.smartpermissionanalyzer.data.models

import androidx.compose.runtime.Immutable

@Immutable
data class DetailedPermission(
    val name: String,
    val friendlyName: String,
    val description: String,
    val risk: RiskLevel
)
