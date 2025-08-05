package com.yourname.smartpermissionanalyzer.ui.details

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.smartpermissionanalyzer.presentation.viewmodel.AppDetailsViewModel
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    packageName: String,
    onBackClick: () -> Unit,
    onTrustApp: () -> Unit,
    onFlagAsRisky: () -> Unit,
    viewModel: AppDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showRemoveDialog by remember { mutableStateOf(false) }

    // ✅ FIXED: Separate processing states for each button
    var isProcessingTrust by remember { mutableStateOf(false) }
    var isProcessingFlag by remember { mutableStateOf(false) }

    LaunchedEffect(packageName) {
        viewModel.loadAppDetails(packageName)
    }

    // ✅ FIXED: Listen for completion signals instead of message detection
    LaunchedEffect(Unit) {
        viewModel.trustActionCompleted.collect {
            isProcessingTrust = false
            viewModel.clearActionMessage()
            onTrustApp() // Navigate back
        }
    }

    LaunchedEffect(Unit) {
        viewModel.flagActionCompleted.collect {
            isProcessingFlag = false
            viewModel.clearActionMessage()
            onFlagAsRisky() // Navigate back
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.appEntity?.appName ?: "App Details",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val shareIntent = viewModel.getShareIntent()
                            if (shareIntent != null) {
                                context.startActivity(shareIntent)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, "Share Report")
                    }

                    IconButton(onClick = { viewModel.exportAppReport() }) {
                        Icon(Icons.Default.Download, "Export Report")
                    }
                }
            )
        }
    ) { paddingValues ->

        when {
            uiState.isLoading -> {
                LoadingScreen()
            }
            uiState.appEntity != null -> {
                AppDetailsContent(
                    app = uiState.appEntity!!,
                    uiState = uiState,
                    onTrustClick = {
                        // ✅ FIXED: Prevent multiple clicks and start processing
                        if (!isProcessingTrust && !isProcessingFlag) {
                            isProcessingTrust = true
                            viewModel.markAppAsTrusted(packageName)
                        }
                    },
                    onFlagClick = {
                        // ✅ FIXED: Prevent multiple clicks and start processing
                        if (!isProcessingTrust && !isProcessingFlag) {
                            isProcessingFlag = true
                            viewModel.flagAppAsRisky(packageName)
                        }
                    },
                    onRemoveClick = { showRemoveDialog = true },
                    isProcessingTrust = isProcessingTrust,
                    isProcessingFlag = isProcessingFlag,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                ErrorScreen(
                    error = uiState.error ?: "App not found",
                    onRetry = { viewModel.loadAppDetails(packageName) }
                )
            }
        }

        if (showRemoveDialog) {
            RemoveConfirmationDialog(
                appName = uiState.appEntity?.appName ?: "this app",
                onConfirm = {
                    viewModel.removeAppFromAnalysis()
                    showRemoveDialog = false
                    onBackClick()
                },
                onDismiss = { showRemoveDialog = false }
            )
        }
    }
}

@Composable
private fun AppDetailsContent(
    app: com.yourname.smartpermissionanalyzer.domain.entities.AppEntity,
    uiState: com.yourname.smartpermissionanalyzer.presentation.viewmodel.AppDetailsUiState,
    onTrustClick: () -> Unit,
    onFlagClick: () -> Unit,
    onRemoveClick: () -> Unit,
    isProcessingTrust: Boolean = false, // ✅ NEW: Separate trust processing state
    isProcessingFlag: Boolean = false,  // ✅ NEW: Separate flag processing state
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            AppHeaderCard(
                app = app,
                onTrustClick = onTrustClick,
                onFlagClick = onFlagClick,
                isProcessingTrust = isProcessingTrust,
                isProcessingFlag = isProcessingFlag
            )
        }

        item {
            SecurityAnalysisCard(
                app = app,
                privacyScore = uiState.privacyScore
            )
        }

        // Real Permission Categories
        item {
            PermissionCategoriesCard(
                permissions = app.permissions,
                suspiciousPermissions = app.suspiciousPermissions
            )
        }

        // Real App Usage & Installation Info
        item {
            AppUsageAndInstallationCard(app = app)
        }

        // Privacy Impact Analysis
        item {
            PrivacyImpactAnalysisCard(
                permissions = app.permissions,
                suspiciousPermissions = app.suspiciousPermissions
            )
        }

        if (uiState.recommendations.isNotEmpty()) {
            item {
                RecommendationsCard(
                    recommendations = uiState.recommendations
                )
            }
        }

        // Security Best Practices
        item {
            SecurityBestPracticesCard(app = app)
        }

        item {
            EnhancedPermissionsCard(
                permissions = app.permissions,
                suspiciousPermissions = app.suspiciousPermissions,
                onPermissionExplain = { permission ->
                    getPermissionExplanation(permission)
                }
            )
        }

        item {
            AppInfoCard(app = app)
        }

        uiState.optimizationResult?.let { optimization ->
            item {
                OptimizationCard(optimization = optimization)
            }
        }

        item {
            DangerZoneCard(
                appName = app.appName,
                onRemoveClick = onRemoveClick
            )
        }
    }
}

@Composable
private fun AppHeaderCard(
    app: com.yourname.smartpermissionanalyzer.domain.entities.AppEntity,
    onTrustClick: () -> Unit,
    onFlagClick: () -> Unit,
    isProcessingTrust: Boolean = false, // ✅ NEW: Individual trust processing state
    isProcessingFlag: Boolean = false   // ✅ NEW: Individual flag processing state
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.appName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (app.isSystemApp) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF2196F3).copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Android,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color(0xFF2196F3)
                                    )
                                    Text(
                                        "System App",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2196F3),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                app.appCategory.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                RiskScoreCircle(
                    score = app.riskScore,
                    level = app.riskLevel
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ✅ FIXED: Trust button with individual processing state
                Button(
                    onClick = onTrustClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessingTrust && !isProcessingFlag, // Disable if either is processing
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    if (isProcessingTrust) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Processing...",
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            Icons.Default.ThumbUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Trust This App",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // ✅ FIXED: Flag button with individual processing state
                Button(
                    onClick = onFlagClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessingTrust && !isProcessingFlag, // Disable if either is processing
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5722)
                    )
                ) {
                    if (isProcessingFlag) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Processing...",
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Flag as Risky",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskScoreCircle(
    score: Int,
    level: RiskLevelEntity
) {
    val color = when (level) {
        RiskLevelEntity.CRITICAL -> Color(0xFFD32F2F)
        RiskLevelEntity.HIGH -> Color(0xFFFF5722)
        RiskLevelEntity.MEDIUM -> Color(0xFFFF9800)
        RiskLevelEntity.LOW -> Color(0xFF4CAF50)
        RiskLevelEntity.MINIMAL -> Color(0xFF2E7D32)
        RiskLevelEntity.UNKNOWN -> Color(0xFF757575)
    }

    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = 6.dp,
            trackColor = color.copy(alpha = 0.2f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                score.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                level.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
private fun SecurityAnalysisCard(
    app: com.yourname.smartpermissionanalyzer.domain.entities.AppEntity,
    privacyScore: com.yourname.smartpermissionanalyzer.domain.privacy.PrivacyScore?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Security Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SecurityMetric(
                    label = "Risk Score",
                    value = "${app.riskScore}/100",
                    color = when {
                        app.riskScore >= 80 -> Color(0xFFD32F2F)
                        app.riskScore >= 60 -> Color(0xFFFF5722)
                        app.riskScore >= 40 -> Color(0xFFFF9800)
                        else -> Color(0xFF4CAF50)
                    }
                )

                SecurityMetric(
                    label = "Trust Score",
                    value = "${(app.trustScore * 100).toInt()}%",
                    color = Color(0xFF4CAF50)
                )

                SecurityMetric(
                    label = "Risk Level",
                    value = app.riskLevel.name,
                    color = when (app.riskLevel) {
                        RiskLevelEntity.CRITICAL -> Color(0xFFD32F2F)
                        RiskLevelEntity.HIGH -> Color(0xFFFF5722)
                        RiskLevelEntity.MEDIUM -> Color(0xFFFF9800)
                        RiskLevelEntity.LOW -> Color(0xFF4CAF50)
                        RiskLevelEntity.MINIMAL -> Color(0xFF2E7D32)
                        RiskLevelEntity.UNKNOWN -> Color(0xFF757575)
                    }
                )
            }
        }
    }
}

@Composable
private fun SecurityMetric(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Real Permission Categories Card
@Composable
private fun PermissionCategoriesCard(
    permissions: List<String>,
    suspiciousPermissions: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Permission Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${permissions.size} total permissions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group permissions by category
            val categorizedPermissions = permissions.groupBy { getPermissionCategory(it) }

            categorizedPermissions.forEach { (category, perms) ->
                val suspiciousInCategory = perms.count { it in suspiciousPermissions }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (suspiciousInCategory > 0)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    getPermissionCategoryIcon(category),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (suspiciousInCategory > 0) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    category,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "${perms.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (suspiciousInCategory > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFF5722).copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            "$suspiciousInCategory risky",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFF5722),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(perms.take(5)) { permission ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (permission in suspiciousPermissions)
                                        Color(0xFFFF5722).copy(alpha = 0.1f)
                                    else
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ) {
                                    Text(
                                        permission.substringAfterLast(".").replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (permission in suspiciousPermissions) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (perms.size > 5) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            "+${perms.size - 5}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Real App Usage & Installation Info Card
@Composable
private fun AppUsageAndInstallationCard(
    app: com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "App Installation & Usage",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Real app information and usage data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Installation Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InstallationStatCard(
                    icon = Icons.Default.Download,
                    label = "Installed",
                    value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(app.installTime)),
                    color = Color(0xFF2196F3)
                )

                InstallationStatCard(
                    icon = Icons.Default.Update,
                    label = "Last Update",
                    value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(app.lastUpdateTime)),
                    color = Color(0xFF4CAF50)
                )

                InstallationStatCard(
                    icon = Icons.Default.Storage,
                    label = "App Size",
                    value = "${String.format("%.1f", app.appSize / (1024.0 * 1024.0))} MB",
                    color = Color(0xFFFF9800)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Version Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Version Information",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Version Name:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            app.versionName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Target SDK:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "API ${app.targetSdkVersion}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Status:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (app.isEnabled) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFFF5722).copy(alpha = 0.1f)
                        ) {
                            Text(
                                if (app.isEnabled) "Enabled" else "Disabled",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (app.isEnabled) Color(0xFF4CAF50) else Color(0xFFFF5722),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallationStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = Modifier.width(110.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Privacy Impact Analysis Card
@Composable
private fun PrivacyImpactAnalysisCard(
    permissions: List<String>,
    suspiciousPermissions: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Privacy Impact Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "What this app can access on your device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    Icons.Default.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy Areas Analysis
            val privacyAreas = mapOf(
                "Personal Data" to listOf("CONTACTS", "CALENDAR", "CALL_LOG"),
                "Media & Camera" to listOf("CAMERA", "RECORD_AUDIO"),
                "Location" to listOf("ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION"),
                "Communication" to listOf("SMS", "SEND_SMS", "READ_SMS"),
                "Storage" to listOf("READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE"),
                "Device Info" to listOf("READ_PHONE_STATE", "DEVICE_ID")
            )

            privacyAreas.forEach { (area, areaPermissions) ->
                val hasAccess = permissions.any { permission ->
                    areaPermissions.any { areaPerm -> permission.contains(areaPerm) }
                }
                val isRisky = suspiciousPermissions.any { permission ->
                    areaPermissions.any { areaPerm -> permission.contains(areaPerm) }
                }

                PrivacyImpactItem(
                    area = area,
                    hasAccess = hasAccess,
                    isRisky = isRisky,
                    icon = getPrivacyAreaIcon(area)
                )
            }
        }
    }
}

@Composable
private fun PrivacyImpactItem(
    area: String,
    hasAccess: Boolean,
    isRisky: Boolean,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isRisky) Color(0xFFFF5722) else if (hasAccess) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                area,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (hasAccess) FontWeight.Medium else FontWeight.Normal
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = when {
                isRisky -> Color(0xFFFF5722).copy(alpha = 0.1f)
                hasAccess -> Color(0xFFFF9800).copy(alpha = 0.1f)
                else -> Color(0xFF4CAF50).copy(alpha = 0.1f)
            }
        ) {
            Text(
                when {
                    isRisky -> "High Risk"
                    hasAccess -> "Has Access"
                    else -> "No Access"
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isRisky -> Color(0xFFFF5722)
                    hasAccess -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                },
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Security Best Practices Card
@Composable
private fun SecurityBestPracticesCard(
    app: com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Security Best Practices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val recommendations = generateSecurityRecommendations(app)

            recommendations.forEach { recommendation ->
                SecurityRecommendationItem(
                    title = recommendation.title,
                    description = recommendation.description,
                    priority = recommendation.priority,
                    icon = recommendation.icon
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SecurityRecommendationItem(
    title: String,
    description: String,
    priority: RecommendationPriority,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (priority) {
                RecommendationPriority.HIGH -> Color(0xFFFF5722).copy(alpha = 0.1f)
                RecommendationPriority.MEDIUM -> Color(0xFFFF9800).copy(alpha = 0.1f)
                RecommendationPriority.LOW -> Color(0xFF4CAF50).copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = when (priority) {
                    RecommendationPriority.HIGH -> Color(0xFFFF5722)
                    RecommendationPriority.MEDIUM -> Color(0xFFFF9800)
                    RecommendationPriority.LOW -> Color(0xFF4CAF50)
                }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (priority) {
                    RecommendationPriority.HIGH -> Color(0xFFFF5722).copy(alpha = 0.2f)
                    RecommendationPriority.MEDIUM -> Color(0xFFFF9800).copy(alpha = 0.2f)
                    RecommendationPriority.LOW -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                }
            ) {
                Text(
                    priority.name.lowercase().replaceFirstChar { it.uppercase() },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (priority) {
                        RecommendationPriority.HIGH -> Color(0xFFFF5722)
                        RecommendationPriority.MEDIUM -> Color(0xFFFF9800)
                        RecommendationPriority.LOW -> Color(0xFF4CAF50)
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RecommendationsCard(
    recommendations: List<com.yourname.smartpermissionanalyzer.domain.recommendations.SmartRecommendation>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "AI Security Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            recommendations.take(3).forEach { recommendation ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        recommendation.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedPermissionsCard(
    permissions: List<String>,
    suspiciousPermissions: List<String>,
    onPermissionExplain: (String) -> String
) {
    var showAllPermissions by remember { mutableStateOf(false) }
    val displayPermissions = if (showAllPermissions) permissions else permissions.take(8)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "All Permissions (${permissions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (permissions.size > 8) {
                    TextButton(
                        onClick = { showAllPermissions = !showAllPermissions }
                    ) {
                        Text(
                            if (showAllPermissions) "Show Less" else "Show All",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            if (showAllPermissions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            displayPermissions.forEach { permission ->
                val isSuspicious = permission in suspiciousPermissions

                PermissionItemWithIcon(
                    permission = permission,
                    isSuspicious = isSuspicious,
                    explanation = onPermissionExplain(permission)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!showAllPermissions && permissions.size > 8) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "... and ${permissions.size - 8} more permissions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PermissionItemWithIcon(
    permission: String,
    isSuspicious: Boolean,
    explanation: String
) {
    val (icon, displayName, color) = getPermissionIconAndName(permission, isSuspicious)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSuspicious) FontWeight.Medium else FontWeight.Normal
            )

            if (explanation.isNotBlank()) {
                Text(
                    explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isSuspicious) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFF5722).copy(alpha = 0.1f)
            ) {
                Text(
                    "Risky",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF5722),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AppInfoCard(
    app: com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Technical Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow("Version", app.versionName)
            InfoRow("Package", app.packageName)
            InfoRow("Category", app.appCategory.name.replace("_", " "))
            InfoRow("System App", if (app.isSystemApp) "Yes" else "No")
            InfoRow("Target SDK", "API ${app.targetSdkVersion}")
            InfoRow("App Size", "${String.format("%.1f", app.appSize / (1024.0 * 1024.0))} MB")
            InfoRow("Installed", SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(app.installTime)))
            InfoRow("Updated", SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(app.lastUpdateTime)))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun OptimizationCard(
    optimization: com.yourname.smartpermissionanalyzer.domain.optimization.OptimizationResult
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Optimization Suggestions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Potential risk reduction: ${optimization.totalPotentialRiskReduction} points"
            )
        }
    }
}

@Composable
private fun DangerZoneCard(
    appName: String,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Danger Zone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD32F2F)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Remove $appName from security analysis. This does not uninstall the app from your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD32F2F).copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRemoveClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remove from Analysis")
            }
        }
    }
}

@Composable
private fun RemoveConfirmationDialog(
    appName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Remove App from Analysis",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )
        },
        text = {
            Column {
                Text("Are you sure you want to remove $appName from analysis?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This will not uninstall the app from your device, only remove it from security monitoring.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                )
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Loading app details...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorScreen(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Error",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

// Helper Functions and Data Classes

// Security recommendation data class
data class SecurityRecommendation(
    val title: String,
    val description: String,
    val priority: RecommendationPriority,
    val icon: ImageVector
)

enum class RecommendationPriority {
    HIGH, MEDIUM, LOW
}

// Helper function to generate security recommendations based on app data
private fun generateSecurityRecommendations(app: com.yourname.smartpermissionanalyzer.domain.entities.AppEntity): List<SecurityRecommendation> {
    val recommendations = mutableListOf<SecurityRecommendation>()

    when (app.riskLevel) {
        RiskLevelEntity.CRITICAL -> {
            recommendations.add(
                SecurityRecommendation(
                    title = "Consider Uninstalling",
                    description = "This app poses a critical security risk to your device",
                    priority = RecommendationPriority.HIGH,
                    icon = Icons.Default.Delete
                )
            )
        }
        RiskLevelEntity.HIGH -> {
            recommendations.add(
                SecurityRecommendation(
                    title = "Review Permissions",
                    description = "Check and revoke unnecessary permissions in Android settings",
                    priority = RecommendationPriority.HIGH,
                    icon = Icons.Default.Security
                )
            )
        }
        RiskLevelEntity.MEDIUM -> {
            recommendations.add(
                SecurityRecommendation(
                    title = "Monitor Closely",
                    description = "Keep an eye on this app's behavior and updates",
                    priority = RecommendationPriority.MEDIUM,
                    icon = Icons.Default.Visibility
                )
            )
        }
        else -> {
            recommendations.add(
                SecurityRecommendation(
                    title = "Keep Updated",
                    description = "Ensure the app stays up to date for security patches",
                    priority = RecommendationPriority.LOW,
                    icon = Icons.Default.Update
                )
            )
        }
    }

    // Add permission-specific recommendations
    if (app.permissions.any { it.contains("CAMERA") }) {
        recommendations.add(
            SecurityRecommendation(
                title = "Camera Privacy",
                description = "Only grant camera access when actively using photo features",
                priority = if (app.permissions.any { it in app.suspiciousPermissions }) RecommendationPriority.HIGH else RecommendationPriority.MEDIUM,
                icon = Icons.Default.PhotoCamera
            )
        )
    }

    if (app.permissions.any { it.contains("LOCATION") }) {
        recommendations.add(
            SecurityRecommendation(
                title = "Location Settings",
                description = "Use 'While using app' instead of 'Always' for location access",
                priority = RecommendationPriority.MEDIUM,
                icon = Icons.Default.LocationOn
            )
        )
    }

    return recommendations
}

private fun getPermissionCategory(permission: String): String {
    return when {
        permission.contains("CAMERA") || permission.contains("RECORD_AUDIO") -> "Media & Camera"
        permission.contains("LOCATION") -> "Location Services"
        permission.contains("CONTACTS") || permission.contains("CALENDAR") -> "Personal Information"
        permission.contains("SMS") || permission.contains("CALL") -> "Communication"
        permission.contains("STORAGE") || permission.contains("WRITE") || permission.contains("READ") -> "Storage & Files"
        permission.contains("INTERNET") || permission.contains("NETWORK") -> "Network & Internet"
        permission.contains("BLUETOOTH") || permission.contains("NFC") -> "Device Connectivity"
        else -> "System & Other"
    }
}

private fun getPermissionCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Media & Camera" -> Icons.Default.PhotoCamera
        "Location Services" -> Icons.Default.LocationOn
        "Personal Information" -> Icons.Default.Contacts
        "Communication" -> Icons.Default.Sms
        "Storage & Files" -> Icons.Default.Storage
        "Network & Internet" -> Icons.Default.Wifi
        "Device Connectivity" -> Icons.Default.Bluetooth
        else -> Icons.Default.Security
    }
}

private fun getPrivacyAreaIcon(area: String): ImageVector {
    return when (area) {
        "Personal Data" -> Icons.Default.Contacts
        "Media & Camera" -> Icons.Default.PhotoCamera
        "Location" -> Icons.Default.LocationOn
        "Communication" -> Icons.Default.Sms
        "Storage" -> Icons.Default.Storage
        "Device Info" -> Icons.Default.PhoneAndroid
        else -> Icons.Default.Security
    }
}

private fun getPermissionIconAndName(permission: String, isSuspicious: Boolean): Triple<ImageVector, String, Color> {
    val cleanName = permission.substringAfterLast(".").replace("_", " ").lowercase()
        .replaceFirstChar { it.uppercase() }

    return when {
        permission.contains("CAMERA", ignoreCase = true) -> Triple(
            Icons.Default.PhotoCamera,
            "Camera Access",
            if (isSuspicious) Color(0xFFFF5722) else Color(0xFF2196F3)
        )

        permission.contains("RECORD_AUDIO", ignoreCase = true) -> Triple(
            Icons.Default.Mic,
            "Microphone Access",
            if (isSuspicious) Color(0xFFFF5722) else Color(0xFF2196F3)
        )

        permission.contains("LOCATION", ignoreCase = true) -> Triple(
            Icons.Default.LocationOn,
            "Location Access",
            if (isSuspicious) Color(0xFFFF5722) else Color(0xFF4CAF50)
        )

        permission.contains("CONTACTS", ignoreCase = true) -> Triple(
            Icons.Default.Contacts,
            "Contacts Access",
            if (isSuspicious) Color(0xFFFF5722) else Color(0xFF9C27B0)
        )

        permission.contains("SMS", ignoreCase = true) -> Triple(
            Icons.Default.Sms,
            "SMS Messages",
            if (isSuspicious) Color(0xFFD32F2F) else Color(0xFF2196F3)
        )

        permission.contains("CALL", ignoreCase = true) -> Triple(
            Icons.Default.Call,
            "Phone Calls",
            if (isSuspicious) Color(0xFFFF5722) else Color(0xFF4CAF50)
        )

        permission.contains("STORAGE", ignoreCase = true) -> Triple(
            Icons.Default.Storage,
            "Storage Access",
            if (isSuspicious) Color(0xFFFF9800) else Color(0xFF607D8B)
        )

        permission.contains("INTERNET", ignoreCase = true) -> Triple(
            Icons.Default.Wifi,
            "Internet Access",
            Color(0xFF2196F3)
        )

        permission.contains("NETWORK", ignoreCase = true) -> Triple(
            Icons.Default.NetworkCheck,
            "Network State",
            Color(0xFF607D8B)
        )

        permission.contains("BLUETOOTH", ignoreCase = true) -> Triple(
            Icons.Default.Bluetooth,
            "Bluetooth Access",
            Color(0xFF2196F3)
        )

        permission.contains("VIBRATE", ignoreCase = true) -> Triple(
            Icons.Default.Vibration,
            "Vibration Control",
            Color(0xFF9C27B0)
        )

        permission.contains("WAKE_LOCK", ignoreCase = true) -> Triple(
            Icons.Default.ScreenLockRotation,
            "Keep Device Awake",
            if (isSuspicious) Color(0xFFFF9800) else Color(0xFF607D8B)
        )

        permission.contains("CALENDAR", ignoreCase = true) -> Triple(
            Icons.Default.CalendarToday,
            "Calendar Access",
            if (isSuspicious) Color(0xFFFF5722) else Color(0xFF4CAF50)
        )

        permission.contains("SYSTEM_ALERT", ignoreCase = true) -> Triple(
            Icons.Default.Window,
            "System Overlay",
            Color(0xFFFF9800)
        )

        permission.contains("ADMIN", ignoreCase = true) -> Triple(
            Icons.Default.AdminPanelSettings,
            "Device Admin",
            Color(0xFFD32F2F)
        )

        permission.contains("WRITE", ignoreCase = true) -> Triple(
            Icons.Default.Edit,
            cleanName,
            if (isSuspicious) Color(0xFFFF9800) else Color(0xFF607D8B)
        )

        permission.contains("READ", ignoreCase = true) -> Triple(
            Icons.Default.ReadMore,
            cleanName,
            if (isSuspicious) Color(0xFFFF9800) else Color(0xFF607D8B)
        )

        else -> Triple(
            Icons.Default.Security,
            cleanName,
            if (isSuspicious) Color(0xFFFF5722) else Color(0xFF607D8B)
        )
    }
}

private fun getPermissionExplanation(permission: String): String {
    return when {
        permission.contains("CAMERA") -> "Allows the app to take pictures and record videos using the device camera"
        permission.contains("RECORD_AUDIO") -> "Allows the app to record audio using the device microphone"
        permission.contains("ACCESS_FINE_LOCATION") -> "Allows the app to access precise location information using GPS"
        permission.contains("ACCESS_COARSE_LOCATION") -> "Allows the app to access approximate location information"
        permission.contains("READ_CONTACTS") -> "Allows the app to read your contact list and personal information"
        permission.contains("WRITE_CONTACTS") -> "Allows the app to modify your contact list"
        permission.contains("READ_SMS") -> "Allows the app to read your text messages"
        permission.contains("SEND_SMS") -> "Allows the app to send text messages"
        permission.contains("READ_CALL_LOG") -> "Allows the app to read your call history"
        permission.contains("WRITE_CALL_LOG") -> "Allows the app to modify your call history"
        permission.contains("CALL_PHONE") -> "Allows the app to make phone calls"
        permission.contains("READ_PHONE_STATE") -> "Allows the app to access phone information and status"
        permission.contains("READ_EXTERNAL_STORAGE") -> "Allows the app to read files from external storage"
        permission.contains("WRITE_EXTERNAL_STORAGE") -> "Allows the app to write files to external storage"
        permission.contains("INTERNET") -> "Allows the app to access the internet and send data"
        permission.contains("ACCESS_NETWORK_STATE") -> "Allows the app to view network connection information"
        permission.contains("WAKE_LOCK") -> "Allows the app to prevent the device from sleeping"
        permission.contains("VIBRATE") -> "Allows the app to control device vibration"
        permission.contains("BLUETOOTH") -> "Allows the app to connect to Bluetooth devices"
        permission.contains("NFC") -> "Allows the app to use Near Field Communication"
        else -> "Standard Android permission: ${permission.substringAfterLast(".")}"
    }
}
