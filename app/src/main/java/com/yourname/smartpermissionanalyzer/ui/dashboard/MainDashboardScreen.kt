package com.yourname.smartpermissionanalyzer.ui.dashboard

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.smartpermissionanalyzer.presentation.viewmodel.DashboardViewModel
import com.yourname.smartpermissionanalyzer.presentation.viewmodel.NotificationsViewModel
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    onAppDetailsClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onRecommendationsClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
    notificationsViewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    val filteredResults by viewModel.filteredResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedRiskFilter by viewModel.selectedRiskFilter.collectAsState()
    val context = LocalContext.current

    // Get real notification count
    val unreadNotificationCount by notificationsViewModel.unreadCount.collectAsState()

    // Show export dialog
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        notificationsViewModel.refreshNotifications()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Header Card
        item {
            SecurityHeaderCard(
                totalApps = uiState.totalAppsScanned,
                riskApps = uiState.highRiskApps,
                notificationCount = unreadNotificationCount,
                isScanning = uiState.isLoading,
                onScanClick = { viewModel.startScan() },
                onNotificationsClick = onNotificationsClick,
                onSettingsClick = onSettingsClick,
                onRecommendationsClick = onRecommendationsClick
            )
        }

        // Search Bar
        item {
            SearchBar(
                query = searchQuery,
                onQueryChange = { query: String -> viewModel.searchApps(query) },
                placeholder = "Search apps by name..."
            )
        }

        // Risk Level Filter
        item {
            RiskLevelFilterRow(
                selectedFilter = selectedRiskFilter,
                onFilterSelected = { filter -> viewModel.setRiskFilter(filter) }
            )
        }

        // Quick Actions Row - Responsive for all devices
        item {
            ResponsiveQuickActionsRow(
                onFullScanClick = { viewModel.startScan() },
                onExportClick = { showExportDialog = true }
            )
        }

        // Loading/Empty/Content
        when {
            uiState.isLoading && filteredResults.isEmpty() -> {
                item {
                    LoadingState()
                }
            }
            filteredResults.isEmpty() && !uiState.isLoading -> {
                item {
                    EmptyState(
                        hasScannedBefore = uiState.totalAppsScanned > 0,
                        onScanClick = { viewModel.startScan() }
                    )
                }
            }
            else -> {
                // Section Header
                item {
                    ResponsiveText(
                        text = "Security Analysis (${filteredResults.size} apps)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(
                    items = filteredResults,
                    key = { app: AppEntity -> app.packageName }
                ) { app: AppEntity ->
                    ResponsiveAppCard(
                        app = app,
                        onClick = { onAppDetailsClick(app.packageName) },
                        onTrustClick = { viewModel.markAppAsTrusted(app.packageName) },
                        onFlagClick = { viewModel.flagAppAsRisky(app.packageName) },
                        context = context,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Error Display
        uiState.error?.let { error ->
            item {
                ErrorCard(
                    error = error,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }
    }

    // Enhanced Export Options Dialog with PDF
    if (showExportDialog) {
        ResponsiveExportOptionsDialog(
            onDismiss = { showExportDialog = false },
            onCsvExport = {
                viewModel.exportAnalysisDataAsCsv()
                showExportDialog = false
            },
            onTextExport = {
                viewModel.exportAnalysisDataAsText()
                showExportDialog = false
            },
            onPdfExport = {
                viewModel.exportAnalysisDataAsPdf()
                showExportDialog = false
            }
        )
    }
}

// ✅ NEW: Fully Responsive Quick Actions Row
@Composable
private fun ResponsiveQuickActionsRow(
    onFullScanClick: () -> Unit,
    onExportClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val density = LocalDensity.current

    // Device type detection
    val isTablet = screenWidth >= 600.dp
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val isCompact = screenWidth < 360.dp

    // Responsive dimensions
    val buttonHeight = when {
        isTablet -> 72.dp
        isCompact -> 52.dp
        else -> 64.dp
    }

    val iconSize = when {
        isTablet -> 28.dp
        isCompact -> 18.dp
        else -> 24.dp
    }

    val fontSize = when {
        isTablet -> 16.sp
        isCompact -> 12.sp
        else -> 14.sp
    }

    val horizontalPadding = when {
        isTablet -> 24.dp
        else -> 16.dp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (isTablet) 24.dp else 16.dp,
                vertical = if (isTablet) 20.dp else 16.dp
            )
        ) {
            ResponsiveText(
                text = "Quick Actions",
                style = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(if (isTablet) 16.dp else 12.dp))

            if (isLandscape && !isTablet && screenWidth > 500.dp) {
                // Landscape layout for phones
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QuickActionButton(
                        onClick = onFullScanClick,
                        icon = Icons.Default.Security,
                        text = "Security Scan",
                        iconSize = iconSize,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionButton(
                        onClick = onExportClick,
                        icon = Icons.Default.Download,
                        text = "Export Report",
                        iconSize = iconSize,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f),
                        isSecondary = true
                    )
                }
            } else {
                // Portrait layout or tablet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    horizontalArrangement = Arrangement.spacedBy(if (isTablet) 20.dp else 12.dp)
                ) {
                    QuickActionButton(
                        onClick = onFullScanClick,
                        icon = Icons.Default.Security,
                        text = if (isCompact) "Scan" else "Security Scan",
                        iconSize = iconSize,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionButton(
                        onClick = onExportClick,
                        icon = Icons.Default.Download,
                        text = if (isCompact) "Export" else "Export Report",
                        iconSize = iconSize,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f),
                        isSecondary = true
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    iconSize: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp.dp >= 600.dp

    ElevatedButton(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(if (isTablet) 18.dp else 14.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(
            horizontal = if (isTablet) 16.dp else 12.dp,
            vertical = if (isTablet) 12.dp else 8.dp
        )
    ) {
        if (isTablet) {
            // Tablet layout - horizontal
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Phone layout - vertical
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text,
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    hasScannedBefore: Boolean,
    onScanClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp.dp >= 600.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 48.dp else 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (hasScannedBefore) Icons.Default.Refresh else Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(if (isTablet) 64.dp else 48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))

            ResponsiveText(
                text = if (hasScannedBefore) "Ready for New Scan" else "Welcome to Argus",
                style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            ResponsiveText(
                text = if (hasScannedBefore)
                    "Tap 'Security Scan' to analyze your apps again"
                else
                    "Start your first security scan to analyze your apps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))

            Button(
                onClick = onScanClick,
                modifier = Modifier.then(
                    if (isTablet) Modifier.fillMaxWidth(0.6f) else Modifier.fillMaxWidth()
                )
            ) {
                Icon(
                    if (hasScannedBefore) Icons.Default.Refresh else Icons.Default.Security,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (hasScannedBefore) "Scan Again" else "Start Scan")
            }
        }
    }
}

@Composable
private fun SecurityHeaderCard(
    totalApps: Int,
    riskApps: Int,
    notificationCount: Int,
    isScanning: Boolean,
    onScanClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecommendationsClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp.dp >= 600.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 32.dp else 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ResponsiveText(
                        text = "Argus",
                        style = if (isTablet) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    val statusText = when {
                        isScanning -> "Analyzing your apps..."
                        riskApps == 0 && totalApps > 0 -> "All apps are secure"
                        riskApps <= 2 && totalApps > 0 -> "Mostly secure"
                        totalApps > 0 -> "$riskApps apps need attention"
                        else -> "Tap 'Security Scan' to begin"
                    }

                    ResponsiveText(
                        text = statusText,
                        style = if (isTablet) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 8.dp)
                ) {
                    Box {
                        IconButton(
                            onClick = onNotificationsClick,
                            modifier = Modifier
                                .size(if (isTablet) 56.dp else 48.dp)
                                .background(
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(if (isTablet) 28.dp else 24.dp)
                            )
                        }

                        if (notificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(if (isTablet) 24.dp else 20.dp)
                                    .offset(x = if (isTablet) 40.dp else 34.dp, y = (-2).dp)
                                    .background(
                                        Color.Red,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (notificationCount > 99) "99+" else "$notificationCount",
                                    color = Color.White,
                                    fontSize = if (isTablet) 12.sp else 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onRecommendationsClick,
                        modifier = Modifier
                            .size(if (isTablet) 56.dp else 48.dp)
                            .background(
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI Recommendations",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(if (isTablet) 28.dp else 24.dp)
                        )
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(if (isTablet) 56.dp else 48.dp)
                            .background(
                                Color(0xFF2196F3).copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(if (isTablet) 28.dp else 24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SecurityStatItem(
                    value = totalApps.toString(),
                    label = "Total Apps",
                    icon = Icons.Default.Apps,
                    color = MaterialTheme.colorScheme.primary,
                    isTablet = isTablet
                )

                SecurityStatItem(
                    value = (totalApps - riskApps).toString(),
                    label = "Secure",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50),
                    isTablet = isTablet
                )

                SecurityStatItem(
                    value = riskApps.toString(),
                    label = "At Risk",
                    icon = Icons.Default.Warning,
                    color = if (riskApps > 0) Color(0xFFFF5722) else Color(0xFF4CAF50),
                    isTablet = isTablet
                )
            }

            Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))

            Button(
                onClick = onScanClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isScanning,
                contentPadding = PaddingValues(vertical = if (isTablet) 16.dp else 12.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(if (isTablet) 20.dp else 16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ResponsiveText(text = "Scanning...")
                } else {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ResponsiveText(
                        text = if (totalApps > 0) "Scan Again" else "Start Security Scan",
                        fontSize = if (isTablet) 16.sp else 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityStatItem(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    isTablet: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(if (isTablet) 28.dp else 20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        ResponsiveText(
            text = value,
            style = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        ResponsiveText(
            text = label,
            style = if (isTablet) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResponsiveAppCard(
    app: AppEntity,
    onClick: () -> Unit,
    onTrustClick: () -> Unit,
    onFlagClick: () -> Unit,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp.dp >= 600.dp
    val isCompact = configuration.screenWidthDp.dp < 360.dp

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 24.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
                ) {
                    AppIcon(
                        packageName = app.packageName,
                        context = context,
                        modifier = Modifier.size(if (isTablet) 56.dp else 48.dp)
                    )

                    Column {
                        ResponsiveText(
                            text = app.appName,
                            style = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        ResponsiveText(
                            text = "${app.permissions.size} permissions • ${app.appCategory.name.replace("_", " ")}",
                            style = if (isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                RiskScoreBadge(
                    score = app.riskScore,
                    level = app.riskLevel,
                    isTablet = isTablet
                )
            }

            Spacer(modifier = Modifier.height(if (isTablet) 16.dp else 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PermissionInfo(
                    icon = Icons.Default.Key,
                    value = "${app.permissions.size}",
                    label = "Permissions",
                    isTablet = isTablet
                )

                PermissionInfo(
                    icon = Icons.Default.Warning,
                    value = "${app.suspiciousPermissionCount}",
                    label = "Suspicious",
                    isTablet = isTablet
                )

                PermissionInfo(
                    icon = Icons.Default.Security,
                    value = "${app.criticalPermissionCount}",
                    label = "Critical",
                    isTablet = isTablet
                )
            }

            Spacer(modifier = Modifier.height(if (isTablet) 16.dp else 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 8.dp)
            ) {
                OutlinedButton(
                    onClick = onTrustClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF4CAF50)
                    ),
                    contentPadding = PaddingValues(vertical = if (isTablet) 12.dp else 8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(if (isTablet) 20.dp else 16.dp)
                    )
                    Spacer(modifier = Modifier.width(if (isCompact) 2.dp else 4.dp))
                    ResponsiveText(
                        text = "Trust",
                        fontSize = if (isTablet) 14.sp else if (isCompact) 12.sp else 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onFlagClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF5722)
                    ),
                    contentPadding = PaddingValues(vertical = if (isTablet) 12.dp else 8.dp)
                ) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(if (isTablet) 20.dp else 16.dp)
                    )
                    Spacer(modifier = Modifier.width(if (isCompact) 2.dp else 4.dp))
                    ResponsiveText(
                        text = "Flag",
                        fontSize = if (isTablet) 14.sp else if (isCompact) 12.sp else 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AppIcon(
    packageName: String,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    val packageManager = context.packageManager
    var appIcon by remember(packageName) { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(packageName) {
        appIcon = try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            applicationInfo.loadIcon(packageManager)
        } catch (e: Exception) {
            null
        }
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp.dp >= 600.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isTablet) 16.dp else 12.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (appIcon != null) {
            val size = if (isTablet) 56 else 48
            val bitmap = appIcon!!.toBitmap(width = size, height = size)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "App Icon",
                modifier = Modifier.size((size * 0.75).dp)
            )
        } else {
            Icon(
                Icons.Default.Apps,
                contentDescription = "Default App Icon",
                modifier = Modifier.size(if (isTablet) 32.dp else 24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp.dp >= 600.dp

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { ResponsiveText(text = placeholder) },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.size(if (isTablet) 28.dp else 24.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        modifier = Modifier.size(if (isTablet) 28.dp else 24.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(if (isTablet) 16.dp else 12.dp),
        textStyle = LocalTextStyle.current.copy(
            fontSize = if (isTablet) 16.sp else 14.sp
        )
    )
}

@Composable
private fun RiskLevelFilterRow(
    selectedFilter: RiskLevelEntity?,
    onFilterSelected: (RiskLevelEntity?) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp.dp >= 600.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 12.dp)
        ) {
            ResponsiveText(
                text = "Filter by Risk Level",
                style = if (isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(if (isTablet) 12.dp else 8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { onFilterSelected(null) },
                        label = { ResponsiveText(text = "All") }
                    )
                }

                item {
                    FilterChip(
                        selected = selectedFilter == RiskLevelEntity.CRITICAL,
                        onClick = { onFilterSelected(RiskLevelEntity.CRITICAL) },
                        label = { ResponsiveText(text = "Critical") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD32F2F).copy(alpha = 0.2f)
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedFilter == RiskLevelEntity.HIGH,
                        onClick = { onFilterSelected(RiskLevelEntity.HIGH) },
                        label = { ResponsiveText(text = "High Risk") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF5722).copy(alpha = 0.2f)
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedFilter == RiskLevelEntity.MEDIUM,
                        onClick = { onFilterSelected(RiskLevelEntity.MEDIUM) },
                        label = { ResponsiveText(text = "Medium") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF9800).copy(alpha = 0.2f)
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedFilter == RiskLevelEntity.LOW,
                        onClick = { onFilterSelected(RiskLevelEntity.LOW) },
                        label = { ResponsiveText(text = "Low Risk") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedFilter == RiskLevelEntity.MINIMAL,
                        onClick = { onFilterSelected(RiskLevelEntity.MINIMAL) },
                        label = { ResponsiveText(text = "Safe") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2E7D32).copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    }
}

// ✅ NEW: Responsive Export Options Dialog
@Composable
private fun ResponsiveExportOptionsDialog(
    onDismiss: () -> Unit,
    onCsvExport: () -> Unit,
    onTextExport: () -> Unit,
    onPdfExport: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp.dp >= 600.dp

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            ResponsiveText(
                text = "Export Security Report",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (isTablet) 20.sp else 18.sp
            )
        },
        text = {
            Column {
                ResponsiveText(
                    text = "Choose the format for your security analysis report:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 20.dp))

                // CSV Export Button
                ElevatedButton(
                    onClick = onCsvExport,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = if (isTablet) 16.dp else 12.dp
                    )
                ) {
                    Icon(
                        Icons.Default.TableChart,
                        contentDescription = null,
                        modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        ResponsiveText(
                            text = "Export as CSV",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (isTablet) 16.sp else 14.sp
                        )
                        ResponsiveText(
                            text = "Spreadsheet format for analysis",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Text Export Button
                ElevatedButton(
                    onClick = onTextExport,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    contentPadding = PaddingValues(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = if (isTablet) 16.dp else 12.dp
                    )
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        ResponsiveText(
                            text = "Export as Text Report",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (isTablet) 16.sp else 14.sp
                        )
                        ResponsiveText(
                            text = "Human-readable detailed report",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // PDF Export Button
                ElevatedButton(
                    onClick = onPdfExport,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    contentPadding = PaddingValues(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = if (isTablet) 16.dp else 12.dp
                    )
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        ResponsiveText(
                            text = "Export as PDF",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (isTablet) 16.sp else 14.sp
                        )
                        ResponsiveText(
                            text = "Professional multi-page report",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                ResponsiveText(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RiskScoreBadge(
    score: Int,
    level: RiskLevelEntity,
    isTablet: Boolean = false
) {
    val (color, text) = when (level) {
        RiskLevelEntity.CRITICAL -> Color(0xFFD32F2F) to "Critical"
        RiskLevelEntity.HIGH -> Color(0xFFFF5722) to "High"
        RiskLevelEntity.MEDIUM -> Color(0xFFFF9800) to "Medium"
        RiskLevelEntity.LOW -> Color(0xFF4CAF50) to "Low"
        RiskLevelEntity.MINIMAL -> Color(0xFF2E7D32) to "Safe"
        RiskLevelEntity.UNKNOWN -> Color(0xFF757575) to "Unknown"
    }

    Surface(
        shape = RoundedCornerShape(if (isTablet) 20.dp else 16.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isTablet) 16.dp else 12.dp,
                vertical = if (isTablet) 8.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ResponsiveText(
                text = "$score",
                style = if (isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            ResponsiveText(
                text = text,
                style = if (isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
private fun PermissionInfo(
    icon: ImageVector,
    value: String,
    label: String,
    isTablet: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(if (isTablet) 20.dp else 16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ResponsiveText(
            text = value,
            style = if (isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        ResponsiveText(
            text = label,
            style = if (isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadingState() {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp.dp >= 600.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(if (isTablet) 48.dp else 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(if (isTablet) 56.dp else 48.dp),
                strokeWidth = if (isTablet) 4.dp else 3.dp
            )
            Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))
            ResponsiveText(
                text = "Analyzing your apps...",
                style = if (isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp.dp >= 600.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 24.dp else 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ResponsiveText(
                text = error,
                style = if (isTablet) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(if (isTablet) 28.dp else 24.dp)
                )
            }
        }
    }
}

// ✅ NEW: Responsive Text Composable
@Composable
private fun ResponsiveText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp.dp >= 600.dp

    val responsiveFontSize = if (fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) {
        fontSize
    } else {
        if (isTablet) (style.fontSize.value * 1.2f).sp else style.fontSize
    }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = responsiveFontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = style
    )
}
