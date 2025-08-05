package com.yourname.smartpermissionanalyzer.core.analyzer

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.ContextCompat
import com.yourname.smartpermissionanalyzer.domain.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.coroutines.coroutineContext

@Singleton
class PermissionAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val packageManager = context.packageManager

    /* ========================================================================================
     * MAIN SCANNING FUNCTIONALITY - Production Ready
     * ======================================================================================== */

    suspend fun scanInstalledApps(): List<AppEntity> = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val allApps = mutableListOf<AppEntity>()

            // Get launcher apps (user-installed apps)
            val launcherApps = getLauncherApps()

            // Process apps with proper error handling
            launcherApps.forEachIndexed { index, appInfo ->
                if (!coroutineContext.isActive) return@withContext allApps

                try {
                    val analyzedApp = analyzeApp(appInfo)
                    allApps.add(analyzedApp)

                    // Yield periodically for smooth UI updates
                    if (index % 5 == 0) yield()

                } catch (e: SecurityException) {
                    // Handle permission denied gracefully
                    val fallbackApp = createFallbackAppEntity(appInfo, "Permission denied")
                    allApps.add(fallbackApp)
                } catch (e: Exception) {
                    // Handle other errors
                    val fallbackApp = createFallbackAppEntity(appInfo, "Analysis failed")
                    allApps.add(fallbackApp)
                }
            }

            val processingTime = System.currentTimeMillis() - startTime
            println("Permission analysis completed in ${processingTime}ms for ${allApps.size} apps")

            // Sort by risk score (highest first) and return
            allApps.sortedByDescending { it.riskScore }

        } catch (e: Exception) {
            println("Fatal error during app scanning: ${e.message}")
            emptyList()
        }
    }

    /* ========================================================================================
     * APP DISCOVERY - Comprehensive
     * ======================================================================================== */

    private fun getLauncherApps(): List<ApplicationInfo> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return try {
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }

            val appInfos = mutableListOf<ApplicationInfo>()

            resolveInfos.forEach { resolveInfo ->
                try {
                    val appInfo = packageManager.getApplicationInfo(
                        resolveInfo.activityInfo.packageName,
                        PackageManager.GET_META_DATA
                    )

                    // Filter out system launcher and basic Android components
                    if (!isBasicSystemApp(appInfo.packageName)) {
                        appInfos.add(appInfo)
                    }

                } catch (e: Exception) {
                    // Skip apps we can't access
                }
            }

            appInfos
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun isBasicSystemApp(packageName: String): Boolean {
        val systemPackages = setOf(
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "android",
            "com.android.systemui"
        )
        return systemPackages.contains(packageName)
    }

    /* ========================================================================================
     * COMPREHENSIVE APP ANALYSIS
     * ======================================================================================== */

    private fun analyzeApp(appInfo: ApplicationInfo): AppEntity {
        val packageName = appInfo.packageName
        val appName = getAppName(appInfo)
        val packageInfo = getPackageInfo(packageName)

        // Core analysis
        val permissions = getAppPermissions(packageInfo)
        val category = determineAppCategory(appInfo, packageName, appName)
        val riskAnalysis = calculateComprehensiveRiskScore(permissions, category, appInfo)
        val suspiciousPermissions = identifySuspiciousPermissions(permissions, category, appName)
        val timingInfo = getAppTimingInfo(packageInfo)
        val appMetadata = getAppMetadata(packageInfo, appInfo)

        return AppEntity(
            appName = appName,
            packageName = packageName,
            appIconPath = packageName, // Using package name as icon identifier
            permissions = permissions,
            detailedPermissions = permissions.map { getFriendlyPermissionDescription(it) },
            riskLevel = riskAnalysis.riskLevel,
            riskScore = riskAnalysis.score,
            appCategory = category,
            suspiciousPermissions = suspiciousPermissions,
            suspiciousPermissionCount = suspiciousPermissions.size,
            criticalPermissionCount = permissions.count { isHighRiskPermission(it) },
            installTime = timingInfo.installTime,
            lastUpdateTime = timingInfo.lastUpdateTime,
            lastUpdate = timingInfo.lastUpdateTime,
            lastScannedTime = System.currentTimeMillis(),
            lastScan = System.currentTimeMillis(),
            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            targetSdkVersion = appMetadata.targetSdkVersion,
            versionName = appMetadata.versionName,
            versionCode = appMetadata.versionCode,
            minSdkVersion = appMetadata.minSdkVersion,
            permissionDensity = calculatePermissionDensity(permissions, category),
            riskFactors = riskAnalysis.riskFactors,
            permissionChanges = emptyList(), // Could be enhanced with historical tracking
            appSize = getAppSize(packageInfo),
            lastUsedTime = getLastUsedTime(packageName),
            isEnabled = appInfo.enabled,
            hasInternetAccess = permissions.any { it.contains("INTERNET", ignoreCase = true) },
            signatureHash = getAppSignatureHash(packageInfo),
            trustScore = calculateInitialTrustScore(riskAnalysis.score, category, appInfo)
        )
    }

    /* ========================================================================================
     * ADVANCED RISK ANALYSIS SYSTEM
     * ======================================================================================== */

    private data class RiskAnalysis(
        val score: Int,
        val riskLevel: RiskLevelEntity,
        val riskFactors: List<String>
    )

    private fun calculateComprehensiveRiskScore(
        permissions: List<String>,
        category: AppCategoryEntity,
        appInfo: ApplicationInfo
    ): RiskAnalysis {
        var score = 0
        val riskFactors = mutableListOf<String>()

        // Base score from permission count
        score += (permissions.size * 1.5).toInt()

        // High-risk permission analysis
        permissions.forEach { permission ->
            val permissionScore = when {
                permission.contains("READ_SMS", ignoreCase = true) -> {
                    riskFactors.add("Can read text messages")
                    35
                }
                permission.contains("SEND_SMS", ignoreCase = true) -> {
                    riskFactors.add("Can send text messages")
                    30
                }
                permission.contains("FINE_LOCATION", ignoreCase = true) -> {
                    riskFactors.add("Tracks precise location")
                    25
                }
                permission.contains("RECORD_AUDIO", ignoreCase = true) -> {
                    riskFactors.add("Can record audio/conversations")
                    25
                }
                permission.contains("CAMERA", ignoreCase = true) -> {
                    riskFactors.add("Can take photos and videos")
                    20
                }
                permission.contains("READ_CONTACTS", ignoreCase = true) -> {
                    riskFactors.add("Can access contact list")
                    20
                }
                permission.contains("CALL_PHONE", ignoreCase = true) -> {
                    riskFactors.add("Can make phone calls")
                    18
                }
                permission.contains("READ_CALL_LOG", ignoreCase = true) -> {
                    riskFactors.add("Can access call history")
                    18
                }
                permission.contains("WRITE_EXTERNAL_STORAGE", ignoreCase = true) -> {
                    riskFactors.add("Can modify files on device")
                    15
                }
                permission.contains("READ_EXTERNAL_STORAGE", ignoreCase = true) -> {
                    riskFactors.add("Can access files on device")
                    10
                }
                permission.contains("GET_ACCOUNTS", ignoreCase = true) -> {
                    riskFactors.add("Can access account information")
                    12
                }
                permission.contains("SYSTEM_ALERT_WINDOW", ignoreCase = true) -> {
                    riskFactors.add("Can display over other apps")
                    15
                }
                else -> 1
            }
            score += permissionScore
        }

        // Category-based adjustments
        when (category) {
            AppCategoryEntity.SYSTEM -> {
                score = maxOf(0, score - 30)
                riskFactors.add("System app - inherently trusted")
            }
            AppCategoryEntity.GAME -> {
                if (permissions.any { it.contains("SMS", ignoreCase = true) }) {
                    score += 20
                    riskFactors.add("Game with SMS access is suspicious")
                }
            }
            AppCategoryEntity.FINANCE -> {
                // Finance apps should be more secure
                if (score > 40) {
                    riskFactors.add("Finance app with extensive permissions")
                }
            }
            else -> { /* No specific adjustments */ }
        }

        // System app bonus
        if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
            score = maxOf(0, score - 25)
        }

        // Dangerous permission combinations
        val hasCameraAndLocation = permissions.any { it.contains("CAMERA", ignoreCase = true) } &&
                permissions.any { it.contains("LOCATION", ignoreCase = true) }
        val hasAudioAndContacts = permissions.any { it.contains("RECORD_AUDIO", ignoreCase = true) } &&
                permissions.any { it.contains("READ_CONTACTS", ignoreCase = true) }

        if (hasCameraAndLocation && hasAudioAndContacts) {
            score += 25
            riskFactors.add("🚨 CRITICAL: Full surveillance capability detected")
        } else if (hasCameraAndLocation) {
            score += 15
            riskFactors.add("⚠️ Can track location and capture images")
        } else if (hasAudioAndContacts) {
            score += 15
            riskFactors.add("⚠️ Can record audio and access contacts")
        }

        val finalScore = minOf(100, maxOf(0, score))
        val riskLevel = when {
            finalScore >= 85 -> RiskLevelEntity.CRITICAL
            finalScore >= 65 -> RiskLevelEntity.HIGH
            finalScore >= 35 -> RiskLevelEntity.MEDIUM
            finalScore >= 15 -> RiskLevelEntity.LOW
            else -> RiskLevelEntity.LOW
        }

        return RiskAnalysis(finalScore, riskLevel, riskFactors.take(5)) // Limit to top 5 factors
    }

    /* ========================================================================================
     * SUSPICIOUS PERMISSION DETECTION
     * ======================================================================================== */

    private fun identifySuspiciousPermissions(
        permissions: List<String>,
        category: AppCategoryEntity,
        appName: String
    ): List<String> {
        val suspicious = mutableListOf<String>()

        // Category-specific suspicious patterns
        when (category) {
            AppCategoryEntity.GAME -> {
                permissions.forEach { permission ->
                    when {
                        permission.contains("SMS", ignoreCase = true) ->
                            suspicious.add("SMS access unusual for games")
                        permission.contains("CALL_PHONE", ignoreCase = true) ->
                            suspicious.add("Phone access unnecessary for games")
                        permission.contains("READ_CONTACTS", ignoreCase = true) ->
                            suspicious.add("Contact access suspicious for games")
                    }
                }
            }
            AppCategoryEntity.UTILITY -> {
                if (permissions.size > 20) {
                    suspicious.add("Utility app requesting excessive permissions")
                }
            }
            AppCategoryEntity.SOCIAL -> {
                // Social apps might legitimately need many permissions
                if (permissions.any { it.contains("CALL_PHONE", ignoreCase = true) }) {
                    suspicious.add("Direct calling capability")
                }
            }
            else -> { /* Category-neutral checks below */ }
        }

        // Universal suspicious patterns
        val hasInternet = permissions.any { it.contains("INTERNET", ignoreCase = true) }
        val hasLocation = permissions.any { it.contains("LOCATION", ignoreCase = true) }
        val hasContacts = permissions.any { it.contains("READ_CONTACTS", ignoreCase = true) }

        if (hasInternet && hasLocation && hasContacts) {
            suspicious.add("Can upload location and contact data")
        }

        // Check for premium SMS potential
        if (permissions.any { it.contains("SEND_SMS", ignoreCase = true) } &&
            permissions.any { it.contains("INTERNET", ignoreCase = true) }) {
            suspicious.add("Potential premium SMS fraud capability")
        }

        return suspicious.distinct()
    }

    /* ========================================================================================
     * UTILITY METHODS - Enhanced
     * ======================================================================================== */

    private fun getAppName(appInfo: ApplicationInfo): String {
        return try {
            val label = appInfo.loadLabel(packageManager).toString()
            if (label.isNotBlank() && label != appInfo.packageName) {
                label
            } else {
                // Fallback: Clean up package name
                appInfo.packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
            }
        } catch (e: Exception) {
            appInfo.packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
    }

    private fun getPackageInfo(packageName: String): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(
                        PackageManager.GET_PERMISSIONS.toLong() or
                                PackageManager.GET_META_DATA.toLong()
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getAppPermissions(packageInfo: PackageInfo?): List<String> {
        return try {
            packageInfo?.requestedPermissions?.toList()?.filter { it.isNotBlank() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun determineAppCategory(
        appInfo: ApplicationInfo,
        packageName: String,
        appName: String
    ): AppCategoryEntity {
        val packageLower = packageName.lowercase()
        val nameLower = appName.lowercase()

        // System app check first
        if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
            return AppCategoryEntity.SYSTEM
        }

        // Detailed category detection
        return when {
            isGameApp(packageLower, nameLower) -> AppCategoryEntity.GAME
            isSocialApp(packageLower, nameLower) -> AppCategoryEntity.SOCIAL
            isFinanceApp(packageLower, nameLower) -> AppCategoryEntity.FINANCE
            isCommunicationApp(packageLower, nameLower) -> AppCategoryEntity.COMMUNICATION
            isMediaApp(packageLower, nameLower) -> AppCategoryEntity.MEDIA
            isProductivityApp(packageLower, nameLower) -> AppCategoryEntity.PRODUCTIVITY
            isHealthApp(packageLower, nameLower) -> AppCategoryEntity.HEALTH
            isEducationApp(packageLower, nameLower) -> AppCategoryEntity.EDUCATION
            isTravelApp(packageLower, nameLower) -> AppCategoryEntity.TRAVEL
            isShoppingApp(packageLower, nameLower) -> AppCategoryEntity.SHOPPING
            isNewsApp(packageLower, nameLower) -> AppCategoryEntity.NEWS
            isUtilityApp(packageLower, nameLower) -> AppCategoryEntity.UTILITY
            else -> AppCategoryEntity.UNKNOWN
        }
    }

    // Category detection methods
    private fun isGameApp(pkg: String, name: String): Boolean =
        listOf("game", "play", "puzzle", "arcade", "casino", "card", "board", "racing", "action", "adventure")
            .any { pkg.contains(it) || name.contains(it) }

    private fun isSocialApp(pkg: String, name: String): Boolean =
        listOf("social", "facebook", "instagram", "twitter", "snapchat", "tiktok", "whatsapp", "telegram")
            .any { pkg.contains(it) || name.contains(it) }

    private fun isFinanceApp(pkg: String, name: String): Boolean =
        listOf("bank", "pay", "wallet", "finance", "money", "credit", "visa", "mastercard", "paypal")
            .any { pkg.contains(it) || name.contains(it) }

    private fun isCommunicationApp(pkg: String, name: String): Boolean =
        listOf("message", "sms", "chat", "call", "phone", "mail", "email", "messenger")
            .any { pkg.contains(it) || name.contains(it) }

    private fun isMediaApp(pkg: String, name: String): Boolean =
        listOf("camera", "photo", "gallery", "music", "video", "player", "media", "youtube", "netflix")
            .any { pkg.contains(it) || name.contains(it) }

    private fun isProductivityApp(pkg: String, name: String): Boolean =
        listOf("office", "document", "pdf", "note", "excel", "word", "powerpoint", "sheets", "docs")
            .any { pkg.contains(it) || name.contains(it) }

    private fun isHealthApp(pkg: String, name: String): Boolean =
        listOf("health", "fitness", "medical", "workout", "step", "heart", "diet", "nutrition")
            .any { pkg.contains(it) || name.contains(it) }

    private fun isEducationApp(pkg: String, name: String): Boolean =
        listOf("education", "learn", "study", "school", "university", "course", "lesson", "tutorial")
            .any { pkg.contains(it) || name.contains(it) }

    private fun isTravelApp(pkg: String, name: String): Boolean =
        listOf("travel", "map", "navigation", "uber", "taxi", "hotel", "flight", "trip", "booking")
            .any { pkg.contains(it) || name.contains(it) }

    private fun isShoppingApp(pkg: String, name: String): Boolean =
        listOf("shop", "store", "market", "buy", "amazon", "ebay", "shopping", "ecommerce", "retail")
            .any { pkg.contains(it) || name.contains(it) }

    private fun isNewsApp(pkg: String, name: String): Boolean =
        listOf("news", "magazine", "newspaper", "article", "times", "post", "journal", "press")
            .any { pkg.contains(it) || name.contains(it) }

    private fun isUtilityApp(pkg: String, name: String): Boolean =
        listOf("flashlight", "torch", "calculator", "utility", "tool", "cleaner", "battery", "wifi")
            .any { pkg.contains(it) || name.contains(it) }

    private fun getFriendlyPermissionDescription(permission: String): String {
        return when {
            permission.contains("CAMERA") -> "📷 Camera - Take photos and record videos"
            permission.contains("FINE_LOCATION") -> "📍 Precise Location - GPS tracking capability"
            permission.contains("COARSE_LOCATION") -> "📍 Approximate Location - Network-based location"
            permission.contains("READ_CONTACTS") -> "👥 Read Contacts - Access contact list"
            permission.contains("READ_SMS") -> "💬 Read SMS - Access text messages"
            permission.contains("SEND_SMS") -> "💬 Send SMS - Send text messages"
            permission.contains("RECORD_AUDIO") -> "🎤 Microphone - Record audio and conversations"
            permission.contains("CALL_PHONE") -> "📞 Phone Calls - Make phone calls directly"
            permission.contains("READ_CALL_LOG") -> "📞 Call History - Access call logs"
            permission.contains("WRITE_EXTERNAL_STORAGE") -> "💾 Write Storage - Modify files on device"
            permission.contains("READ_EXTERNAL_STORAGE") -> "📁 Read Storage - Access files on device"
            permission.contains("INTERNET") -> "🌐 Internet - Network access capability"
            permission.contains("ACCESS_NETWORK_STATE") -> "📶 Network State - Check connection status"
            permission.contains("GET_ACCOUNTS") -> "👤 Accounts - Access account information"
            permission.contains("SYSTEM_ALERT_WINDOW") -> "⚠️ Overlay - Display over other apps"
            permission.contains("WAKE_LOCK") -> "⚡ Keep Awake - Prevent device sleep"
            else -> "🔐 ${permission.substringAfterLast(".").replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}"
        }
    }

    private fun isHighRiskPermission(permission: String): Boolean {
        return listOf(
            "CAMERA", "RECORD_AUDIO", "FINE_LOCATION", "READ_SMS", "SEND_SMS",
            "READ_CONTACTS", "CALL_PHONE", "READ_CALL_LOG", "SYSTEM_ALERT_WINDOW"
        ).any { permission.contains(it, ignoreCase = true) }
    }

    private data class TimingInfo(val installTime: Long, val lastUpdateTime: Long)

    private fun getAppTimingInfo(packageInfo: PackageInfo?): TimingInfo {
        return try {
            TimingInfo(
                installTime = packageInfo?.firstInstallTime ?: System.currentTimeMillis(),
                lastUpdateTime = packageInfo?.lastUpdateTime ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            TimingInfo(System.currentTimeMillis(), System.currentTimeMillis())
        }
    }

    private data class AppMetadata(
        val targetSdkVersion: Int,
        val versionName: String,
        val versionCode: Int,
        val minSdkVersion: Int
    )

    private fun getAppMetadata(packageInfo: PackageInfo?, appInfo: ApplicationInfo): AppMetadata {
        return try {
            AppMetadata(
                targetSdkVersion = appInfo.targetSdkVersion,
                versionName = packageInfo?.versionName ?: "Unknown",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo?.longVersionCode?.toInt() ?: 0
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo?.versionCode ?: 0
                },
                minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appInfo.minSdkVersion
                } else {
                    0
                }
            )
        } catch (e: Exception) {
            AppMetadata(0, "Unknown", 0, 0)
        }
    }

    private fun calculatePermissionDensity(permissions: List<String>, category: AppCategoryEntity): Float {
        val expectedPermissions = when (category) {
            AppCategoryEntity.GAME -> 8
            AppCategoryEntity.SOCIAL -> 12
            AppCategoryEntity.FINANCE -> 6
            AppCategoryEntity.COMMUNICATION -> 10
            AppCategoryEntity.MEDIA -> 15
            AppCategoryEntity.PRODUCTIVITY -> 8
            AppCategoryEntity.SYSTEM -> 20
            else -> 6
        }

        return if (expectedPermissions > 0) {
            permissions.size.toFloat() / expectedPermissions
        } else {
            1.0f
        }
    }

    private fun getAppSize(packageInfo: PackageInfo?): Long {
        // In a real implementation, you could get app size from PackageStats
        return (50..500).random() * 1024 * 1024L // Random size between 50MB-500MB for demo
    }

    private fun getLastUsedTime(packageName: String): Long {
        // Would require PACKAGE_USAGE_STATS permission and UsageStatsManager
        return System.currentTimeMillis() - ((1..168).random() * 60 * 60 * 1000L) // Random within last week
    }

    private fun getAppSignatureHash(packageInfo: PackageInfo?): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo?.signingInfo?.apkContentsSigners?.firstOrNull()?.hashCode()?.toString() ?: ""
            } else {
                @Suppress("DEPRECATION")
                packageInfo?.signatures?.firstOrNull()?.hashCode()?.toString() ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun calculateInitialTrustScore(riskScore: Int, category: AppCategoryEntity, appInfo: ApplicationInfo): Float {
        var trustScore = 100f - (riskScore * 0.8f)

        // System apps get trust bonus
        if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
            trustScore += 20f
        }

        // Category-based trust adjustments
        when (category) {
            AppCategoryEntity.FINANCE -> trustScore += 10f
            AppCategoryEntity.HEALTH -> trustScore += 5f
            AppCategoryEntity.EDUCATION -> trustScore += 5f
            else -> { /* No adjustment */ }
        }

        return (trustScore / 100f).coerceIn(0f, 1f)
    }

    private fun createFallbackAppEntity(appInfo: ApplicationInfo, reason: String = "Unknown"): AppEntity {
        return AppEntity(
            appName = getAppName(appInfo),
            packageName = appInfo.packageName,
            appIconPath = appInfo.packageName,
            permissions = emptyList(),
            detailedPermissions = listOf("⚠️ Analysis incomplete: $reason"),
            riskLevel = RiskLevelEntity.UNKNOWN,
            riskScore = 0,
            appCategory = AppCategoryEntity.UNKNOWN,
            suspiciousPermissions = emptyList(),
            suspiciousPermissionCount = 0,
            criticalPermissionCount = 0,
            installTime = System.currentTimeMillis(),
            lastUpdateTime = System.currentTimeMillis(),
            lastUpdate = System.currentTimeMillis(),
            lastScannedTime = System.currentTimeMillis(),
            lastScan = System.currentTimeMillis(),
            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            targetSdkVersion = appInfo.targetSdkVersion,
            versionName = "Unknown",
            versionCode = 0,
            minSdkVersion = 0,
            permissionDensity = 0f,
            riskFactors = listOf("Analysis incomplete"),
            permissionChanges = emptyList(),
            appSize = 0L,
            lastUsedTime = 0L,
            isEnabled = appInfo.enabled,
            hasInternetAccess = false,
            signatureHash = "",
            trustScore = 0.5f
        )
    }

    /* ========================================================================================
     * PUBLIC API METHODS
     * ======================================================================================== */

    suspend fun analyzeAllInstalledApps(): List<AppEntity> = scanInstalledApps()

    suspend fun analyzeSpecificApp(packageName: String): AppEntity? = withContext(Dispatchers.IO) {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            analyzeApp(appInfo)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun refreshAppAnalysis(packageName: String): AppEntity? = analyzeSpecificApp(packageName)

    fun getAppIconDrawable(packageName: String): Drawable {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            appInfo.loadIcon(packageManager)
        } catch (e: Exception) {
            ContextCompat.getDrawable(context, android.R.drawable.ic_menu_help)
                ?: ContextCompat.getDrawable(context, android.R.drawable.ic_dialog_info)!!
        }
    }
}
