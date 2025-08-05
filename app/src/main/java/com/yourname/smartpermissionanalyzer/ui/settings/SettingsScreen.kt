package com.yourname.smartpermissionanalyzer.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.smartpermissionanalyzer.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    // ✅ FIXED: Accept SettingsViewModel as parameter to avoid creating new instances
    viewModel: SettingsViewModel? = null
) {
    val context = LocalContext.current

    // ✅ FIXED: Use provided ViewModel or create one only if none provided
    // This prevents creating multiple ViewModel instances
    val settingsViewModel = viewModel ?: hiltViewModel<SettingsViewModel>()
    val uiState by settingsViewModel.uiState.collectAsState()

    // ✅ DEBUG: Log to track ViewModel creation
    android.util.Log.d("SettingsScreen", "Using SettingsViewModel: $settingsViewModel")

    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    // ✅ NEW: Advanced settings state
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // ✅ FIXED: Prevent any side effects that might trigger notification refresh
    // Remove any LaunchedEffect that might be causing the issue

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Security Settings Section
            item {
                SettingsSectionCard(
                    title = "Security Settings",
                    icon = Icons.Default.Security
                ) {
                    SettingsToggleItem(
                        title = "Real-time Scanning",
                        subtitle = "Monitor app permissions continuously",
                        checked = uiState.realTimeScanning,
                        onCheckedChange = { settingsViewModel.toggleRealTimeScanning() }
                    )

                    SettingsToggleItem(
                        title = "Security Notifications",
                        subtitle = "Get alerts for suspicious app behavior",
                        checked = uiState.securityNotifications,
                        onCheckedChange = { settingsViewModel.toggleSecurityNotifications() }
                    )

                    SettingsToggleItem(
                        title = "Auto-Updates",
                        subtitle = "Automatically update threat database",
                        checked = uiState.autoUpdates,
                        onCheckedChange = { settingsViewModel.toggleAutoUpdates() }
                    )
                }
            }

            // Privacy Settings Section
            item {
                SettingsSectionCard(
                    title = "Privacy Settings",
                    icon = Icons.Default.PrivacyTip
                ) {
                    SettingsToggleItem(
                        title = "Data Collection",
                        subtitle = "Allow anonymous usage statistics",
                        checked = uiState.dataCollection,
                        onCheckedChange = { settingsViewModel.toggleDataCollection() }
                    )

                    SettingsToggleItem(
                        title = "Usage Analytics",
                        subtitle = "Help improve the app",
                        checked = uiState.usageAnalytics,
                        onCheckedChange = { settingsViewModel.toggleUsageAnalytics() }
                    )

                    SettingsClickableItem(
                        title = "Privacy Policy",
                        subtitle = "View our privacy policy",
                        icon = Icons.Default.Policy
                    ) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/privacy"))
                        context.startActivity(intent)
                    }
                }
            }

            // ✅ NEW: Advanced Settings Section
            item {
                AdvancedSettingsCard(
                    expanded = showAdvancedSettings,
                    onExpandedChange = { showAdvancedSettings = it },
                    uiState = uiState,
                    viewModel = settingsViewModel,
                    onResetClick = { showResetConfirmDialog = true }
                )
            }

            // General Settings Section
            item {
                SettingsSectionCard(
                    title = "General",
                    icon = Icons.Default.Settings
                ) {
                    SettingsClickableItem(
                        title = "About",
                        subtitle = "App version and information",
                        icon = Icons.Default.Info
                    ) {
                        showAboutDialog = true
                    }

                    SettingsClickableItem(
                        title = "Help & Support",
                        subtitle = "Get help using the app",
                        icon = Icons.Default.Help
                    ) {
                        showHelpDialog = true
                    }

                    SettingsClickableItem(
                        title = "Export All Data",
                        subtitle = "Export complete security analysis",
                        icon = Icons.Default.Download
                    ) {
                        settingsViewModel.exportAllData(context)
                    }

                    SettingsClickableItem(
                        title = "Rate This App",
                        subtitle = "Rate us on Google Play Store",
                        icon = Icons.Default.Star
                    ) {
                        val intent = Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=${context.packageName}"))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val webIntent = Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                            context.startActivity(webIntent)
                        }
                    }

                    SettingsClickableItem(
                        title = "Share App",
                        subtitle = "Share with friends and family",
                        icon = Icons.Default.Share
                    ) {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT,
                                "Check out Argus - the best app security scanner! " +
                                        "https://play.google.com/store/apps/details?id=${context.packageName}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share App"))
                    }
                }
            }
        }

        // ✅ FIXED: Removed LaunchedEffect for messages that might cause side effects
        // Show success/error messages only as UI state changes, not as side effects
        uiState.message?.let { message ->
            // Only update UI state, don't trigger any side effects
            android.util.Log.d("SettingsScreen", "Message: $message")
        }
    }

    // About Dialog
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        HelpDialog(
            onDismiss = { showHelpDialog = false }
        )
    }

    // ✅ NEW: Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        ResetConfirmationDialog(
            onDismiss = { showResetConfirmDialog = false },
            onConfirm = {
                settingsViewModel.resetToDefaults()
                showResetConfirmDialog = false
            }
        )
    }
}

// ✅ NEW: Advanced Settings Card with expandable content
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedSettingsCard(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    uiState: com.yourname.smartpermissionanalyzer.presentation.viewmodel.SettingsUiState,
    viewModel: SettingsViewModel,
    onResetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Advanced Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ✅ Expandable content with animation
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    // Scan Frequency Selector
                    ScanFrequencySelector(
                        currentFrequency = uiState.scanFrequency,
                        onFrequencyChange = { viewModel.setScanFrequency(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Notification Priority Selector
                    NotificationPrioritySelector(
                        currentPriority = uiState.notificationPriority,
                        onPriorityChange = { viewModel.setNotificationPriority(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Reset to Defaults Button
                    OutlinedButton(
                        onClick = onResetClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Reset All Settings",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ✅ FIXED: Scan Frequency Selector with "Off" option
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanFrequencySelector(
    currentFrequency: Int,
    onFrequencyChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // ✅ FIXED: Added "Off" option as first choice
    val frequencies = listOf(
        0 to "Off (No automatic scanning)", // ✅ NEW: Off option
        15 to "15 minutes (High Protection)",
        30 to "30 minutes (Default)",
        60 to "1 hour (Balanced)",
        120 to "2 hours (Battery Saver)",
        240 to "4 hours (Light Protection)"
    )

    Column {
        Text(
            "Scan Frequency",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            "How often to check for security threats automatically",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = frequencies.find { it.first == currentFrequency }?.second
                    ?: if (currentFrequency == 0) "Off" else "$currentFrequency minutes",
                onValueChange = { },
                readOnly = true,
                label = { Text("Frequency") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                // ✅ FIXED: Color coding for "Off" option
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = if (currentFrequency == 0)
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                frequencies.forEach { (minutes, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                label,
                                // ✅ FIXED: Color coding for "Off" option
                                color = if (minutes == 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onFrequencyChange(minutes)
                            expanded = false
                        },
                        leadingIcon = {
                            when {
                                minutes == currentFrequency -> {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint = if (minutes == 0) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary
                                    )
                                }
                                minutes == 0 -> {
                                    Icon(
                                        Icons.Default.Block,
                                        null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        // ✅ NEW: Warning message for "Off" option
        if (currentFrequency == 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Automatic scanning is disabled. Manual scans only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ✅ FIXED: Notification Priority Selector (Fixed destructuring and type errors)
@Composable
private fun NotificationPrioritySelector(
    currentPriority: String,
    onPriorityChange: (String) -> Unit
) {
    // ✅ FIXED: Proper data structure - List of Triple instead of nested Pairs
    data class PriorityOption(
        val value: String,
        val label: String,
        val description: String
    )

    val priorities = listOf(
        PriorityOption("HIGH", "High Priority", "Alerts appear immediately with sound/vibration"),
        PriorityOption("MEDIUM", "Medium Priority", "Standard notifications, less intrusive"),
        PriorityOption("LOW", "Low Priority", "Silent notifications, minimal disruption")
    )

    Column {
        Text(
            "Alert Priority",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            "How aggressively to show security alerts",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        priorities.forEach { priorityOption ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (currentPriority == priorityOption.value),
                        onClick = { onPriorityChange(priorityOption.value) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (currentPriority == priorityOption.value),
                    onClick = { onPriorityChange(priorityOption.value) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        priorityOption.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        priorityOption.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ✅ NEW: Reset Confirmation Dialog
@Composable
private fun ResetConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    "Reset All Settings?",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text("This will reset all settings to their default values:")
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Stop real-time scanning")
                Text("• Clear all preferences")
                Text("• Reset scan frequency to 30 minutes")
                Text("• Set notification priority to HIGH")
                Text("• Disable data collection and analytics")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This action cannot be undone.",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reset All")
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
private fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "About Argus",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("Version: 1.0.0")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Argus is a comprehensive security tool that helps you:")
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Analyze app permissions and security risks")
                Text("• Get AI-powered security recommendations")
                Text("• Monitor suspicious app behavior")
                Text("• Export detailed security reports")
                Text("• Optimize your device's security posture")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Developed with ❤️ for your privacy and security.By Alfaiz Shaikh")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun HelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Help & Support",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("How to use Argus:", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                Text("📱 Dashboard:")
                Text("• Tap 'Start Security Scan' to analyze your apps")
                Text("• Use search to find specific apps")
                Text("• Filter apps by risk level")
                Text("• Trust/Flag apps to train the AI")
                Spacer(modifier = Modifier.height(8.dp))

                Text("🔍 App Details:")
                Text("• Tap any app to see detailed analysis")
                Text("• Review permissions and risk factors")
                Text("• Export or share individual app reports")
                Spacer(modifier = Modifier.height(8.dp))

                Text("📊 Export Reports:")
                Text("• Choose CSV for spreadsheet analysis")
                Text("• Choose Text for detailed readable reports")
                Spacer(modifier = Modifier.height(8.dp))

                Text("⚠️ Risk Levels:")
                Text("• Critical: Immediate attention required")
                Text("• High: Review and monitor closely")
                Text("• Medium: Periodic review recommended")
                Text("• Low: Generally safe to use")
                Text("• Safe: Minimal security concerns")
                Spacer(modifier = Modifier.height(8.dp))

                Text("⚙️ Advanced Settings:")
                Text("• Set scan frequency including 'Off' option")
                Text("• Configure notification priorities")
                Text("• Reset all settings to defaults")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!")
            }
        }
    )
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
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
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
