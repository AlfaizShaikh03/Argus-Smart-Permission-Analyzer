package com.yourname.smartpermissionanalyzer.domain.entities

data class LearningDataEntity(
    val id: String,
    val patterns: Map<String, Float>,
    val accuracy: Float,
    val totalSamples: Int,
    val lastUpdated: Long,
    val version: Int = 1
)
