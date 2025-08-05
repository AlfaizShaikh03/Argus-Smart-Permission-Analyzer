package com.yourname.smartpermissionanalyzer.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalAppCard(
    app: AppEntity,
    onClick: () -> Unit,
    onTrustClick: () -> Unit,
    onFlagClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // ✅ FIXED: Perfect App Header Alignment
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Icon with proper sizing
                AppIconPlaceholder(
                    appName = app.appName,
                    isSystemApp = app.isSystemApp
                )

                // ✅ FIXED: Perfect App Info Alignment
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        app.appName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            lineHeight = 22.sp
                        ),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // ✅ FIXED: Perfect Category and Internet Access Alignment
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryChip(
                            category = app.appCategory.name,
                            isSystem = app.isSystemApp
                        )

                        if (app.hasInternetAccess) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Language,
                                    contentDescription = "Internet Access",
                                    modifier = Modifier
                                        .size(12.dp)
                                        .padding(4.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Risk Level Indicator
                RiskLevelChip(riskLevel = app.riskLevel)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Risk Analysis Summary
            RiskAnalysisSummary(app = app)

            Spacer(modifier = Modifier.height(18.dp))

            // ✅ FIXED: Perfect Action Buttons Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onTrustClick,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32).copy(alpha = 0.1f),
                        contentColor = Color(0xFF2E7D32)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        Icons.Outlined.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Trust",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = onFlagClick,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F).copy(alpha = 0.1f),
                        contentColor = Color(0xFFD32F2F)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        Icons.Outlined.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Flag",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Show less" else "Show more",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expanded Details
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                ExpandedAppDetails(app = app)
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: String,
    isSystem: Boolean
) {
    val (displayText, containerColor, contentColor) = when {
        isSystem -> Triple("System", MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.primary)
        category == "SOCIAL" -> Triple("Social", Color(0xFF9C27B0).copy(alpha = 0.15f), Color(0xFF9C27B0))
        category == "GAME" -> Triple("Game", Color(0xFF2196F3).copy(alpha = 0.15f), Color(0xFF2196F3))
        category == "FINANCE" -> Triple("Finance", Color(0xFF4CAF50).copy(alpha = 0.15f), Color(0xFF4CAF50))
        category == "COMMUNICATION" -> Triple("Communication", Color(0xFF607D8B).copy(alpha = 0.15f), Color(0xFF607D8B))
        category == "MEDIA" -> Triple("Media", Color(0xFFE91E63).copy(alpha = 0.15f), Color(0xFFE91E63))
        else -> Triple("App", MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), MaterialTheme.colorScheme.outline)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        modifier = Modifier.height(26.dp)
    ) {
        Text(
            displayText,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 16.sp
            ),
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Rest of the composables remain the same but with improved alignment...
@Composable
private fun AppIconPlaceholder(
    appName: String,
    isSystemApp: Boolean
) {
    Box(
        modifier = Modifier
            .size(52.dp) // Slightly larger for better proportion
            .clip(CircleShape)
            .background(
                if (isSystemApp) {
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                } else {
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        )
                    )
                }
            )
            .border(
                1.5.dp,
                if (isSystemApp) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSystemApp) {
            Icon(
                Icons.Filled.Android,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                appName.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// Other composables remain similar with minor alignment improvements...
@Composable
private fun RiskLevelChip(riskLevel: RiskLevelEntity) {
    val (text, color, icon) = when (riskLevel) {
        RiskLevelEntity.CRITICAL -> Triple("Critical", Color(0xFFD32F2F), Icons.Filled.Error)
        RiskLevelEntity.HIGH -> Triple("High", Color(0xFFD32F2F), Icons.Filled.Warning)
        RiskLevelEntity.MEDIUM -> Triple("Medium", Color(0xFFF57C00), Icons.Filled.Info)
        RiskLevelEntity.LOW -> Triple("Low", Color(0xFF2E7D32), Icons.Filled.CheckCircle)
        else -> Triple("Unknown", MaterialTheme.colorScheme.outline, Icons.Filled.HelpOutline)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Text(
                text,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun RiskAnalysisSummary(app: AppEntity) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Permission Analysis",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                "Risk: ${app.riskScore}/100",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Risk content with perfect spacing
        if (app.suspiciousPermissions.isNotEmpty()) {
            val topRisks = app.suspiciousPermissions.take(2)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                topRisks.forEach { permission ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFF57C00)
                        )
                        Text(
                            getPermissionDisplayName(permission),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (app.suspiciousPermissions.size > 2) {
                    Text(
                        "• ${app.suspiciousPermissions.size - 2} more permissions",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp)
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFF2E7D32)
                )
                Text(
                    "No concerning permissions detected",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
private fun ExpandedAppDetails(app: AppEntity) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DetailRow("Package", app.packageName)
        DetailRow("Version", "${app.versionName} (${app.versionCode})")
        DetailRow("Permissions", "${app.permissions.size} total • ${app.criticalPermissionCount} critical")

        if (app.riskFactors.isNotEmpty()) {
            Text(
                "Risk Factors:",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            app.riskFactors.take(3).forEach { factor ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant,
                                CircleShape
                            )
                            .offset(y = 8.dp)
                    )
                    Text(
                        factor,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(2f)
        )
    }
}

private fun getPermissionDisplayName(permission: String): String {
    return when {
        permission.contains("CAMERA", ignoreCase = true) -> "Camera access"
        permission.contains("LOCATION", ignoreCase = true) -> "Location tracking"
        permission.contains("CONTACTS", ignoreCase = true) -> "Contact access"
        permission.contains("SMS", ignoreCase = true) -> "SMS/messaging"
        permission.contains("MICROPHONE", ignoreCase = true) -> "Microphone access"
        permission.contains("STORAGE", ignoreCase = true) -> "File access"
        permission.contains("PHONE", ignoreCase = true) -> "Phone access"
        else -> permission.substringAfterLast(".").replace("_", " ").lowercase()
    }
}
