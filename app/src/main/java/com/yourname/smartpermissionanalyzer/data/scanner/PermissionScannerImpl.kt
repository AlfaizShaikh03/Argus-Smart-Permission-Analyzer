package com.yourname.smartpermissionanalyzer.data.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.yourname.smartpermissionanalyzer.domain.analyzer.PermissionAnalyzer
import com.yourname.smartpermissionanalyzer.domain.repository.PermissionAnalyzerRepository
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.AppCategoryEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionScannerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PermissionAnalyzerRepository
) : PermissionAnalyzer {

    override suspend fun scanInstalledApps(): Result<List<AppEntity>> = withContext(Dispatchers.IO) {
        return@withContext try {
            println("PermissionScanner: Starting app scan...")
            val packageManager = context.packageManager

            // Get exclusion list with timeout
            val excludedPackages = try {
                val exclusionResult = repository.getExclusionList()
                exclusionResult.getOrElse { emptyList() }.toSet()
            } catch (e: Exception) {
                println("PermissionScanner: Warning - Could not get exclusion list: ${e.message}")
                emptySet()
            }

            println("PermissionScanner: Found ${excludedPackages.size} excluded packages")

            // Get installed apps with proper flags
            val installedApps = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getInstalledApplications(0)
                }
            } catch (e: Exception) {
                println("PermissionScanner: Error getting installed apps: ${e.message}")
                return@withContext Result.failure(e)
            }

            println("PermissionScanner: Found ${installedApps.size} total installed applications")

            // ✅ FIXED: Process only user-facing apps with comprehensive filtering
            val entities = mutableListOf<AppEntity>()
            var processedCount = 0
            var filteredOutCount = 0

            for (appInfo in installedApps) {
                try {
                    // Yield control every 10 apps to prevent ANR
                    if (processedCount % 10 == 0) {
                        yield() // Allow other coroutines to run
                    }

                    // Skip excluded apps during scan
                    if (excludedPackages.contains(appInfo.packageName)) {
                        filteredOutCount++
                        continue
                    }

                    // ✅ NEW: Comprehensive filtering for user-facing apps only
                    if (!isUserFacingApp(appInfo, packageManager)) {
                        filteredOutCount++
                        continue
                    }

                    val entity = createAppEntityOptimized(appInfo, packageManager)

                    // Only include apps with valid names and proper data
                    if (entity.appName.isNotBlank() && entity.appName != "Unknown App") {
                        entities.add(entity)
                        println("PermissionScanner: Added user app: ${entity.appName} (${entity.packageName})")
                    } else {
                        filteredOutCount++
                    }

                    processedCount++

                    // Progress logging every 25 apps
                    if (processedCount % 25 == 0) {
                        println("PermissionScanner: Processed $processedCount apps, filtered out $filteredOutCount...")
                    }

                    // Safety check to prevent memory issues
                    if (entities.size >= 500) {
                        println("PermissionScanner: Safety limit reached (500 apps)")
                        break
                    }

                } catch (e: Exception) {
                    println("PermissionScanner: Failed to process ${appInfo.packageName}: ${e.message}")
                    filteredOutCount++
                }
            }

            // Sort results efficiently
            val sortedEntities = entities.sortedByDescending { it.riskScore }

            // ✅ IMPROVED: More detailed logging
            val riskAppCount = sortedEntities.count {
                it.riskLevel == RiskLevelEntity.HIGH || it.riskLevel == RiskLevelEntity.CRITICAL
            }

            println("PermissionScanner: Scan complete!")
            println("PermissionScanner: - Total apps scanned: ${installedApps.size}")
            println("PermissionScanner: - Apps filtered out: $filteredOutCount")
            println("PermissionScanner: - User-facing apps found: ${sortedEntities.size}")
            println("PermissionScanner: - High/Critical risk apps: $riskAppCount")
            println("PermissionScanner: - Safe apps: ${sortedEntities.size - riskAppCount}")

            Result.success(sortedEntities)

        } catch (e: Exception) {
            println("PermissionScanner Critical Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ✅ NEW: Comprehensive filtering for user-facing apps
    private fun isUserFacingApp(appInfo: ApplicationInfo, packageManager: PackageManager): Boolean {
        val packageName = appInfo.packageName

        // ✅ Step 1: Filter out system internal apps first
        if (isSystemInternalApp(packageName)) {
            return false
        }

        // ✅ Step 2: Filter out system background services and utilities
        if (isSystemBackgroundApp(packageName)) {
            return false
        }

        // ✅ Step 3: Check if app has a launcher icon (user can open it)
        if (!hasLauncherActivity(packageName, packageManager)) {
            return false
        }

        // ✅ Step 4: Filter out accessibility and input method services
        if (isAccessibilityOrInputService(packageName)) {
            return false
        }

        // ✅ Step 5: Keep popular user apps regardless of system status
        if (isPopularUserApp(packageName)) {
            return true
        }

        // ✅ Step 6: For system apps, only keep essential user-facing ones
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        if (isSystemApp && !isEssentialUserSystemApp(packageName)) {
            return false
        }

        return true
    }

    // ✅ NEW: Detect system background apps and services
    private fun isSystemBackgroundApp(packageName: String): Boolean {
        val systemBackgroundPatterns = listOf(
            // Android System Components
            "com.android.shell",              // ADB Shell
            "com.android.systemui",           // System UI
            "com.android.providers.",         // System providers
            "com.android.server.",            // System servers
            "com.android.nfc",               // NFC Service
            "com.android.bluetooth",         // Bluetooth Service
            "com.android.cellbroadcastreceiver", // Emergency alerts
            "com.android.stk",               // SIM Toolkit
            "com.android.se",                // Secure Element
            "com.android.bips",              // Built-in Print Service
            "com.android.printspooler",      // Print Spooler
            "com.android.pacprocessor",      // PAC Processor
            "com.android.proxyhandler",      // Proxy Handler
            "com.android.inputdevices",      // Input Devices
            "com.android.location.fused",    // Fused Location
            "com.android.externalstorage",   // External Storage
            "com.android.mms.service",       // MMS Service
            "com.android.cts.",              // CTS Tests
            "com.android.test.",             // Test Apps

            // Device Management
            "com.android.managedprovisioning", // Device Management
            "com.android.apps.tag",          // NFC Tags
            "com.android.emergency",         // Emergency Info
            "com.android.setupwizard",       // Setup Wizard (user sees once)
            "com.android.provision",         // Device Provisioning

            // Background Services
            "com.qualcomm.",                 // Qualcomm services
            "com.qti.",                      // QTI services
            "vendor.qti.",                   // Vendor QTI
            "org.codeaurora.",              // Code Aurora services

            // OEM Background Services
            "com.miui.daemon",              // Xiaomi daemon
            "com.miui.securitycenter.receiver", // Xiaomi security receiver
            "com.xiaomi.finddevice",        // Xiaomi find device
            "com.samsung.android.game.gos", // Samsung Game Optimizer
            "com.samsung.android.spay.framework", // Samsung Pay Framework
            "com.huawei.android.hsf",       // Huawei service framework
            "com.oppo.safe.",               // Oppo safety services
            "com.oneplus.security.",        // OnePlus security

            // Accessibility and Input
            "com.android.inputmethod.",     // Input methods (but not keyboard apps users see)
            "com.google.android.inputmethod.latin", // But keep Gboard
            "com.android.captiveportallogin", // Captive Portal

            // Other Background
            "android.autoinstalls.",        // Auto installs
            "com.android.ons",             // Opportunistic Network Service
            "com.android.carrierconfig",    // Carrier Config
            "com.android.smspush",          // SMS Push
        )

        return systemBackgroundPatterns.any { pattern ->
            packageName.startsWith(pattern)
        }
    }

    // ✅ NEW: Check if app has launcher activity (user can open it)
    private fun hasLauncherActivity(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            launchIntent != null
        } catch (e: Exception) {
            false
        }
    }

    // ✅ NEW: Filter out accessibility and input services that users don't directly interact with
    private fun isAccessibilityOrInputService(packageName: String): Boolean {
        val servicePatterns = listOf(
            "com.android.talkback",          // Keep TalkBack (users interact with it)
            "com.google.android.marvin.talkback", // TalkBack variants
            "com.android.dreams.",           // Daydream/Screen savers
            "com.android.wallpaper.",        // Live wallpapers (but users see these)
            "com.android.inputmethod.latin", // But NOT Gboard (users use this)
        )

        // Special case: Don't filter out popular keyboards and accessibility apps
        val keepTheseServices = listOf(
            "com.google.android.inputmethod.latin", // Gboard
            "com.swiftkey.swiftkey",        // SwiftKey
            "com.touchtype.swiftkey",       // SwiftKey variants
            "com.google.android.marvin.talkback", // TalkBack
            "com.android.talkback",         // TalkBack
        )

        if (keepTheseServices.any { packageName.startsWith(it) }) {
            return false // Keep these
        }

        return servicePatterns.any { pattern ->
            packageName.startsWith(pattern)
        }
    }

    // ✅ NEW: Popular user apps that should always be included
    private fun isPopularUserApp(packageName: String): Boolean {
        val popularApps = listOf(
            // Social Media
            "com.facebook.katana",          // Facebook
            "com.instagram.android",        // Instagram
            "com.twitter.android",          // Twitter
            "com.snapchat.android",         // Snapchat
            "com.linkedin.android",         // LinkedIn
            "com.pinterest",                // Pinterest
            "com.tiktok",                   // TikTok
            "com.reddit.frontpage",         // Reddit

            // Communication
            "com.whatsapp",                 // WhatsApp
            "org.telegram.messenger",       // Telegram
            "com.viber.voip",               // Viber
            "com.skype.raider",             // Skype
            "us.zoom.videomeetings",        // Zoom
            "com.discord",                  // Discord

            // Google Apps (user-facing)
            "com.google.android.youtube",   // YouTube
            "com.chrome.beta",              // Chrome Beta
            "com.chrome.canary",            // Chrome Canary
            "com.google.android.apps.maps", // Google Maps
            "com.google.android.gm",        // Gmail
            "com.google.android.apps.drive", // Google Drive
            "com.google.android.apps.docs", // Google Docs
            "com.google.android.keep",      // Google Keep
            "com.google.android.calendar",  // Google Calendar

            // Entertainment
            "com.netflix.mediaclient",      // Netflix
            "com.amazon.avod.thirdpartyclient", // Prime Video
            "com.disney.disneyplus",        // Disney+
            "com.spotify.music",            // Spotify
            "com.amazon.mp3",               // Amazon Music
            "com.google.android.music",     // YouTube Music

            // Shopping & Finance
            "com.amazon.mShop.android.shopping", // Amazon
            "com.ebay.mobile",              // eBay
            "com.paypal.android.p2pmobile", // PayPal
            "com.square.cash",              // Cash App
            "com.coinbase.android",         // Coinbase

            // Gaming
            "com.king.candycrushsaga",      // Candy Crush
            "com.supercell.clashofclans",   // Clash of Clans
            "com.mojang.minecraftpe",       // Minecraft
            "com.roblox.client",            // Roblox

            // Productivity
            "com.microsoft.office.outlook", // Outlook
            "com.microsoft.teams",          // Microsoft Teams
            "com.dropbox.android",          // Dropbox
            "com.evernote",                 // Evernote
            "com.todoist",                  // Todoist

            // Utilities (user-facing)
            "com.adobe.reader",             // Adobe Reader
            "com.winzip.android",           // WinZip
            "com.cleanmaster.mguard",       // Clean Master
        )

        return popularApps.any { popular ->
            packageName.startsWith(popular) || packageName.contains(popular)
        }
    }

    // ✅ NEW: Essential system apps that users directly interact with
    private fun isEssentialUserSystemApp(packageName: String): Boolean {
        val essentialSystemApps = listOf(
            // Core Android Apps Users Need
            "com.android.chrome",           // Chrome (system version)
            "com.android.vending",          // Google Play Store
            "com.google.android.gms",       // Google Play Services (users see some UI)
            "com.android.settings",         // Settings
            "com.android.contacts",         // Contacts
            "com.android.dialer",           // Phone
            "com.android.mms",              // Messages/SMS
            "com.android.email",            // Email
            "com.android.calendar",         // Calendar
            "com.android.calculator2",      // Calculator
            "com.android.camera2",          // Camera
            "com.android.gallery3d",        // Gallery
            "com.android.music",            // Music Player
            "com.android.documentsui",      // Files
            "com.android.deskclock",        // Clock/Alarms
            "com.android.soundrecorder",    // Sound Recorder

            // Google Core Apps
            "com.google.android.googlequicksearchbox", // Google App
            "com.google.android.youtube",   // YouTube
            "com.google.android.gm",        // Gmail
            "com.google.android.apps.maps", // Google Maps
            "com.google.android.inputmethod.latin", // Gboard

            // Essential Launchers
            "com.android.launcher3",        // Default Launcher
            "com.google.android.launcher",  // Google Launcher
            "com.google.android.apps.nexuslauncher", // Pixel Launcher
        )

        return essentialSystemApps.any { essential ->
            packageName.startsWith(essential)
        }
    }

    override suspend fun analyzeAppPermissions(packageName: String): Result<AppEntity> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Check if app is excluded
            val isExcludedResult = repository.isExcluded(packageName)
            if (isExcludedResult.getOrElse { false }) {
                return@withContext Result.failure(Exception("App is excluded from analysis"))
            }

            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val entity = createAppEntityOptimized(appInfo, packageManager)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun analyzePermissionRisk(permissions: List<String>): Result<Int> {
        return try {
            val score = calculateRiskScore(permissions)
            Result.success(score)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isSystemInternalApp(packageName: String): Boolean {
        // Keep the original method for basic system internal filtering
        val internalPatterns = listOf(
            "com.android.internal",
            "com.android.server",
            "android.autoinstalls",
            "com.android.theme",
            "com.android.overlay",
            "com.android.providers.downloads.ui",
            "com.android.packageinstaller",
            "com.android.backupconfirm",
            "com.android.keychain",
            "com.android.proxyhandler",
            "com.android.managedprovisioning",
        )

        return internalPatterns.any { pattern ->
            packageName.startsWith(pattern)
        }
    }

    // Keep all your existing helper methods unchanged...
    private fun createAppEntityOptimized(appInfo: ApplicationInfo, packageManager: PackageManager): AppEntity {
        val permissions = getPermissionsSafely(appInfo.packageName, packageManager)
        val appName = getAppNameSafely(appInfo, packageManager)
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val category = determineAppCategory(appName, appInfo.packageName)
        val riskScore = calculateRiskScore(permissions)
        val riskLevel = determineRiskLevel(riskScore)
        val suspiciousPermissions = findSuspiciousPermissions(permissions)

        return AppEntity(
            appName = appName,
            packageName = appInfo.packageName,
            permissions = permissions,
            detailedPermissions = permissions,
            riskLevel = riskLevel,
            riskScore = riskScore,
            appCategory = category,
            suspiciousPermissions = suspiciousPermissions,
            suspiciousPermissionCount = suspiciousPermissions.size,
            criticalPermissionCount = countCriticalPermissions(permissions),
            installTime = getInstallTimeSafely(appInfo.packageName, packageManager),
            lastUpdateTime = getLastUpdateTimeSafely(appInfo.packageName, packageManager),
            lastUpdate = getLastUpdateTimeSafely(appInfo.packageName, packageManager),
            lastScannedTime = System.currentTimeMillis(),
            lastScan = System.currentTimeMillis(),
            isSystemApp = isSystemApp,
            targetSdkVersion = appInfo.targetSdkVersion,
            versionName = getVersionNameSafely(appInfo.packageName, packageManager),
            versionCode = getVersionCodeSafely(appInfo.packageName, packageManager),
            minSdkVersion = 0,
            permissionDensity = calculatePermissionDensity(permissions),
            riskFactors = generateRiskFactors(permissions, category),
            permissionChanges = emptyList(),
            appSize = getAppSizeSafely(appInfo),
            lastUsedTime = 0L,
            isEnabled = appInfo.enabled,
            hasInternetAccess = permissions.contains("android.permission.INTERNET"),
            signatureHash = getSignatureHashSafely(appInfo.packageName, packageManager),
            trustScore = calculateTrustScore(permissions, isSystemApp, category)
        )
    }

    private fun getPermissionsSafely(packageName: String, packageManager: PackageManager): List<String> {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
            packageInfo.requestedPermissions?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getAppNameSafely(appInfo: ApplicationInfo, packageManager: PackageManager): String {
        return try {
            val label = appInfo.loadLabel(packageManager).toString()
            if (label.isNotBlank() && label != appInfo.packageName) {
                label
            } else {
                appInfo.packageName.split(".").lastOrNull()
                    ?.replace("_", " ")
                    ?.split(" ")
                    ?.joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                    ?: "Unknown App"
            }
        } catch (e: Exception) {
            appInfo.packageName.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: "Unknown App"
        }
    }

    private fun getInstallTimeSafely(packageName: String, packageManager: PackageManager): Long {
        return try {
            packageManager.getPackageInfo(packageName, 0).firstInstallTime
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun getLastUpdateTimeSafely(packageName: String, packageManager: PackageManager): Long {
        return try {
            packageManager.getPackageInfo(packageName, 0).lastUpdateTime
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun getVersionNameSafely(packageName: String, packageManager: PackageManager): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    private fun getVersionCodeSafely(packageName: String, packageManager: PackageManager): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    private fun getAppSizeSafely(appInfo: ApplicationInfo): Long {
        return try {
            val sourceDir = appInfo.sourceDir
            java.io.File(sourceDir).length()
        } catch (e: Exception) {
            0L
        }
    }

    private fun getSignatureHashSafely(packageName: String, packageManager: PackageManager): String {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
            }
            signatures?.firstOrNull()?.toCharsString()?.take(16) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun determineAppCategory(appName: String, packageName: String): AppCategoryEntity {
        val name = appName.lowercase()
        val pkg = packageName.lowercase()

        return when {
            pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("messenger") -> AppCategoryEntity.COMMUNICATION
            pkg.contains("facebook") || pkg.contains("instagram") || pkg.contains("twitter") -> AppCategoryEntity.SOCIAL
            pkg.contains("game") || name.contains("game") -> AppCategoryEntity.GAME
            pkg.contains("camera") || pkg.contains("photo") -> AppCategoryEntity.PHOTOGRAPHY
            pkg.contains("music") || pkg.contains("spotify") -> AppCategoryEntity.MUSIC_AND_AUDIO
            pkg.contains("chrome") || pkg.contains("browser") -> AppCategoryEntity.COMMUNICATION
            pkg.contains("bank") || pkg.contains("pay") -> AppCategoryEntity.FINANCE
            pkg.contains("shop") || name.contains("shop") -> AppCategoryEntity.SHOPPING
            pkg.contains("map") || pkg.contains("navigation") -> AppCategoryEntity.MAPS_AND_NAVIGATION
            pkg.contains("weather") -> AppCategoryEntity.WEATHER
            pkg.contains("health") || pkg.contains("fitness") -> AppCategoryEntity.HEALTH
            pkg.contains("news") -> AppCategoryEntity.NEWS
            pkg.contains("office") || pkg.contains("document") -> AppCategoryEntity.PRODUCTIVITY
            pkg.contains("tool") || pkg.contains("util") -> AppCategoryEntity.TOOLS
            pkg.startsWith("com.android") -> AppCategoryEntity.SYSTEM
            else -> AppCategoryEntity.UNKNOWN
        }
    }

    private fun calculateRiskScore(permissions: List<String>): Int {
        if (permissions.isEmpty()) return 10

        var score = 0

        val criticalPermissions = mapOf(
            "android.permission.CAMERA" to 15,
            "android.permission.RECORD_AUDIO" to 15,
            "android.permission.ACCESS_FINE_LOCATION" to 18,
            "android.permission.ACCESS_COARSE_LOCATION" to 12,
            "android.permission.READ_CONTACTS" to 20,
            "android.permission.WRITE_CONTACTS" to 18,
            "android.permission.READ_SMS" to 25,
            "android.permission.SEND_SMS" to 22,
            "android.permission.CALL_PHONE" to 15,
            "android.permission.READ_CALL_LOG" to 20,
            "android.permission.WRITE_CALL_LOG" to 18,
            "android.permission.READ_PHONE_STATE" to 12
        )

        permissions.forEach { permission ->
            score += criticalPermissions[permission] ?: when {
                permission.contains("READ_") -> 8
                permission.contains("WRITE_") -> 10
                permission.contains("ACCESS_") -> 7
                permission.contains("INTERNET") -> 5
                permission.contains("NETWORK") -> 3
                else -> 2
            }
        }

        val hasCamera = permissions.contains("android.permission.CAMERA")
        val hasMicrophone = permissions.contains("android.permission.RECORD_AUDIO")
        val hasInternet = permissions.contains("android.permission.INTERNET")
        val hasLocation = permissions.any { it.contains("LOCATION") }

        if (hasCamera && hasMicrophone && hasInternet) score += 30
        if (hasLocation && hasInternet) score += 20

        return minOf(100, maxOf(10, score))
    }

    private fun determineRiskLevel(score: Int): RiskLevelEntity {
        return when {
            score >= 85 -> RiskLevelEntity.CRITICAL
            score >= 70 -> RiskLevelEntity.HIGH
            score >= 50 -> RiskLevelEntity.MEDIUM
            score >= 30 -> RiskLevelEntity.LOW
            else -> RiskLevelEntity.MINIMAL
        }
    }

    private fun findSuspiciousPermissions(permissions: List<String>): List<String> {
        val suspiciousPatterns = listOf(
            "CAMERA", "RECORD_AUDIO", "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION",
            "READ_CONTACTS", "WRITE_CONTACTS", "READ_SMS", "SEND_SMS",
            "READ_CALL_LOG", "WRITE_CALL_LOG", "CALL_PHONE", "READ_PHONE_STATE"
        )

        return permissions.filter { permission ->
            suspiciousPatterns.any { pattern ->
                permission.contains(pattern, ignoreCase = true)
            }
        }
    }

    private fun countCriticalPermissions(permissions: List<String>): Int {
        val criticalPermissions = setOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_SMS",
            "android.permission.SEND_SMS"
        )
        return permissions.count { it in criticalPermissions }
    }

    private fun calculatePermissionDensity(permissions: List<String>): Float {
        return if (permissions.isEmpty()) 0f else minOf(1f, permissions.size.toFloat() / 25f)
    }

    private fun generateRiskFactors(permissions: List<String>, category: AppCategoryEntity): List<String> {
        val factors = mutableListOf<String>()

        if (permissions.contains("android.permission.CAMERA")) {
            factors.add("Can access device camera")
        }
        if (permissions.contains("android.permission.RECORD_AUDIO")) {
            factors.add("Can record audio")
        }
        if (permissions.any { it.contains("LOCATION") }) {
            factors.add("Can track your location")
        }
        if (permissions.contains("android.permission.READ_CONTACTS")) {
            factors.add("Can read your contacts")
        }
        if (permissions.any { it.contains("SMS") }) {
            factors.add("Can access SMS messages")
        }

        return factors
    }

    private fun calculateTrustScore(permissions: List<String>, isSystemApp: Boolean, category: AppCategoryEntity): Float {
        var trustScore = 0.5f

        if (isSystemApp) trustScore += 0.3f
        if (permissions.size <= 10) trustScore += 0.1f
        if (category != AppCategoryEntity.UNKNOWN) trustScore += 0.1f

        return minOf(1.0f, trustScore)
    }
}
