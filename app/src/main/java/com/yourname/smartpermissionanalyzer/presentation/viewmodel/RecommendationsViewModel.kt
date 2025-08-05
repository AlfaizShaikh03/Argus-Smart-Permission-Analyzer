package com.yourname.smartpermissionanalyzer.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity
import com.yourname.smartpermissionanalyzer.domain.recommendations.SmartRecommendation
import com.yourname.smartpermissionanalyzer.domain.recommendations.RecommendationPriority
import com.yourname.smartpermissionanalyzer.domain.recommendations.SmartRecommendationEngine
import com.yourname.smartpermissionanalyzer.domain.recommendations.UserSecurityPreferences
import com.yourname.smartpermissionanalyzer.domain.recommendations.SecurityLevel
import com.yourname.smartpermissionanalyzer.domain.repository.PermissionAnalyzerRepository
import javax.inject.Inject


@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PermissionAnalyzerRepository,
    private val aiEngine: SmartRecommendationEngine // ✅ FIXED: Injected through constructor
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendationsUiState())
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()

    private val _recommendations = MutableStateFlow<List<SmartRecommendation>>(emptyList())
    val recommendations: StateFlow<List<SmartRecommendation>> = _recommendations.asStateFlow()

    data class RecommendationsUiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val lastUpdated: Long = 0L
    )

    fun loadRecommendations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val apps = repository.getAnalysisResults().getOrThrow()
                val userPreferences = getUserSecurityPreferences()

                // ✅ FIXED: Use injected aiEngine
                val aiRecommendations = aiEngine.generatePersonalizedRecommendations(
                    apps = apps,
                    userPreferences = userPreferences,
                    context = context
                )

                _recommendations.value = aiRecommendations
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastUpdated = System.currentTimeMillis()
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load recommendations"
                )
            }
        }
    }

    fun refreshRecommendations() {
        loadRecommendations()
    }

    fun handleRecommendationAction(
        recommendation: SmartRecommendation,
        context: Context,
        onAppClick: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (recommendation.type) {
                "REVOKE_PERMISSION" -> {
                    val packageName = extractPackageNameFromRecommendation(recommendation)
                    if (packageName.isNotEmpty()) {
                        onAppClick(packageName)
                    }
                    Toast.makeText(context, "Review permissions for this app", Toast.LENGTH_SHORT).show()
                }

                "UNINSTALL_APP" -> {
                    val packageName = extractPackageNameFromRecommendation(recommendation)
                    if (packageName.isNotEmpty()) {
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.parse("package:$packageName")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open app settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                "TRUST_APP" -> {
                    val packageName = extractPackageNameFromRecommendation(recommendation)
                    if (packageName.isNotEmpty()) {
                        repository.markAppAsTrusted(packageName)
                        Toast.makeText(context, "App marked as trusted", Toast.LENGTH_SHORT).show()
                        refreshRecommendations()
                    }
                }

                else -> {
                    val packageName = extractPackageNameFromRecommendation(recommendation)
                    if (packageName.isNotEmpty()) {
                        onAppClick(packageName)
                    }
                }
            }
        }
    }

    private fun extractPackageNameFromRecommendation(recommendation: SmartRecommendation): String {
        return recommendation.message.substringAfter("package:", "")
            .substringBefore(" ")
            .ifEmpty {
                when {
                    recommendation.message.contains("com.") -> {
                        val regex = Regex("""(com\.[a-zA-Z0-9._]+)""")
                        regex.find(recommendation.message)?.value ?: ""
                    }
                    else -> ""
                }
            }
    }

    private suspend fun getUserSecurityPreferences(): UserSecurityPreferences {
        val sharedPrefs = context.getSharedPreferences("smart_permission_analyzer_settings", Context.MODE_PRIVATE)

        return UserSecurityPreferences(
            securityLevel = when {
                sharedPrefs.getBoolean("high_security_mode", false) -> SecurityLevel.HIGH
                sharedPrefs.getBoolean("balanced_mode", true) -> SecurityLevel.BALANCED
                else -> SecurityLevel.PERMISSIVE
            },
            allowDataCollection = sharedPrefs.getBoolean("data_collection", false),
            autoOptimize = sharedPrefs.getBoolean("auto_optimize", false),
            privacyFocused = sharedPrefs.getBoolean("privacy_focused", true)
        )
    }
}
