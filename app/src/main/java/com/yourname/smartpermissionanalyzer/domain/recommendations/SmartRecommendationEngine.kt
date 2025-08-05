package com.yourname.smartpermissionanalyzer.domain.recommendations

import android.content.Context
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity

interface SmartRecommendationEngine {
    suspend fun generatePersonalizedRecommendations(
        apps: List<AppEntity>,
        userPreferences: UserSecurityPreferences,
        context: Context
    ): List<SmartRecommendation>
}
