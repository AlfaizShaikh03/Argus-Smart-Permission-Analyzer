package com.yourname.smartpermissionanalyzer.presentation.viewmodel

data class LearningStats(
    val totalFeedback: Int = 0,
    val trustedApps: Int = 0,
    val flaggedApps: Int = 0,
    val patternsLearned: Int = 0,
    val accuracyPercentage: Float = 0f
)
