package com.yourname.smartpermissionanalyzer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.yourname.smartpermissionanalyzer.domain.recommendations.SmartRecommendationEngine
import com.yourname.smartpermissionanalyzer.domain.recommendations.SmartRecommendationEngineImpl
import com.yourname.smartpermissionanalyzer.domain.optimization.SecurityOptimizer
import com.yourname.smartpermissionanalyzer.domain.optimization.SecurityOptimizerImpl
import com.yourname.smartpermissionanalyzer.domain.privacy.PrivacyImpactCalculator
import com.yourname.smartpermissionanalyzer.domain.privacy.PrivacyImpactCalculatorImpl
import com.yourname.smartpermissionanalyzer.domain.risk.AdvancedRiskScorer
import com.yourname.smartpermissionanalyzer.domain.risk.AdvancedRiskScorerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalysisModule {

    @Binds
    @Singleton
    abstract fun bindSmartRecommendationEngine(
        smartRecommendationEngineImpl: SmartRecommendationEngineImpl
    ): SmartRecommendationEngine

    @Binds
    @Singleton
    abstract fun bindSecurityOptimizer(
        securityOptimizerImpl: SecurityOptimizerImpl
    ): SecurityOptimizer

    @Binds
    @Singleton
    abstract fun bindPrivacyImpactCalculator(
        privacyImpactCalculatorImpl: PrivacyImpactCalculatorImpl
    ): PrivacyImpactCalculator

    @Binds
    @Singleton
    abstract fun bindAdvancedRiskScorer(
        advancedRiskScorerImpl: AdvancedRiskScorerImpl
    ): AdvancedRiskScorer
}
