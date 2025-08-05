package com.yourname.smartpermissionanalyzer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.smartpermissionanalyzer.ui.dashboard.MainDashboardScreen
import com.yourname.smartpermissionanalyzer.ui.details.AppDetailsScreen
import com.yourname.smartpermissionanalyzer.ui.notifications.NotificationScreen
import com.yourname.smartpermissionanalyzer.ui.settings.SettingsScreen
import com.yourname.smartpermissionanalyzer.ui.recommendations.RecommendationsScreen
import com.yourname.smartpermissionanalyzer.ui.theme.SmartPermissionAnalyzerTheme
import com.yourname.smartpermissionanalyzer.presentation.viewmodel.SettingsViewModel
import com.yourname.smartpermissionanalyzer.presentation.viewmodel.NotificationsViewModel
import com.yourname.smartpermissionanalyzer.presentation.viewmodel.DashboardViewModel
import com.yourname.smartpermissionanalyzer.services.BackgroundScanService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Permission launcher for notifications
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start background service if settings allow
            startBackgroundServiceIfEnabled()
        } else {
            // Permission denied - show explanation or continue without notifications
            // Background service will still work, but notifications won't show
            startBackgroundServiceIfEnabled()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge with proper system bar handling
        enableEdgeToEdge()

        // Configure window for proper status bar handling
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize app and request permissions
        initializeApp()

        setContent {
            SmartPermissionAnalyzerTheme {
                // Handle status bar colors based on theme
                HandleSystemBars()

                // Surface with proper insets handling
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmartPermissionAnalyzerApp()
                }
            }
        }
    }

    // Handle system bars (status bar, navigation bar) colors
    @Composable
    private fun HandleSystemBars() {
        val isDarkTheme = isSystemInDarkTheme()
        val statusBarColor = MaterialTheme.colorScheme.background
        val navigationBarColor = MaterialTheme.colorScheme.background

        LaunchedEffect(isDarkTheme, statusBarColor, navigationBarColor) {
            // Set status bar color
            window.statusBarColor = statusBarColor.toArgb()

            // Set navigation bar color
            window.navigationBarColor = navigationBarColor.toArgb()

            // Configure status bar content color based on theme
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    // Initialize app with permissions and background service
    private fun initializeApp() {
        // Request notification permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    startBackgroundServiceIfEnabled()
                }
                PackageManager.PERMISSION_DENIED -> {
                    // Request permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 and below - no permission needed
            startBackgroundServiceIfEnabled()
        }
    }

    // Start background service if real-time scanning is enabled
    private fun startBackgroundServiceIfEnabled() {
        try {
            // Check SharedPreferences to see if real-time scanning is enabled
            val sharedPrefs = getSharedPreferences("smart_permission_analyzer_settings", Context.MODE_PRIVATE)
            val realTimeScanning = sharedPrefs.getBoolean("real_time_scanning", true) // Default true

            if (realTimeScanning) {
                // Start the background scanning service
                BackgroundScanService.startService(this)
            }
        } catch (e: Exception) {
            // Handle any errors gracefully
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if we should restart service when app comes to foreground
        startBackgroundServiceIfEnabled()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't stop the service here - it should continue running in background
        // Only stop if user explicitly disables it in settings
    }
}

@Composable
fun SmartPermissionAnalyzerApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Create shared ViewModel instances at the top level
    // This ensures the same ViewModel instance is used across all screens
    val notificationsViewModel: NotificationsViewModel = hiltViewModel()
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    // Handle system insets properly
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars) // Handle status bar
            .windowInsetsPadding(WindowInsets.navigationBars) // Handle navigation bar
            .windowInsetsPadding(WindowInsets.displayCutout) // Handle camera cutout/notch
    ) {
        NavHost(
            navController = navController,
            startDestination = "main_dashboard"
        ) {
            // Main Dashboard
            composable("main_dashboard") {
                MainDashboardScreen(
                    onAppDetailsClick = { packageName: String ->
                        navController.navigate("app_details/$packageName")
                    },
                    onSettingsClick = {
                        navController.navigate("settings")
                    },
                    onNotificationsClick = {
                        navController.navigate("notifications")
                    },
                    onRecommendationsClick = {
                        navController.navigate("recommendations")
                    },
                    // Pass the shared ViewModel instances
                    viewModel = dashboardViewModel,
                    notificationsViewModel = notificationsViewModel
                )
            }

            // App Details
            composable(
                "app_details/{packageName}",
                arguments = listOf(navArgument("packageName") { type = NavType.StringType })
            ) { backStackEntry ->
                val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                AppDetailsScreen(
                    packageName = packageName,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onTrustApp = {
                        // ✅ FIXED: Properly refresh dashboard data when returning
                        dashboardViewModel.refreshData()
                    },
                    onFlagAsRisky = {
                        // ✅ FIXED: Properly refresh dashboard data when returning
                        dashboardViewModel.refreshData()
                    }
                )
            }

            // AI Recommendations
            composable("recommendations") {
                RecommendationsScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onAppClick = { packageName: String ->
                        navController.navigate("app_details/$packageName")
                    }
                )
            }

            // Notifications
            composable("notifications") {
                NotificationScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onAppDetailsClick = { packageName: String ->
                        navController.navigate("app_details/$packageName")
                    },
                    // Pass the shared ViewModel instance
                    viewModel = notificationsViewModel
                )
            }

            // Settings
            composable("settings") {
                SettingsScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    // Pass the shared ViewModel instance
                    viewModel = settingsViewModel
                )
            }
        }
    }
}
