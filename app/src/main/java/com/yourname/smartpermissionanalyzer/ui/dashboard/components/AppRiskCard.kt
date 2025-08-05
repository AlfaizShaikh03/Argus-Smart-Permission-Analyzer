package com.yourname.smartpermissionanalyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRiskCard(
    app: AppEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RiskLevelChip(riskLevel = app.riskLevel)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(
                    icon = Icons.Default.Security,
                    label = "Risk Score",
                    value = "${app.riskScore}/100"
                )

                InfoItem(
                    icon = Icons.Default.Key,
                    label = "Permissions",
                    value = app.permissions.size.toString()
                )

                InfoItem(
                    icon = Icons.Default.Warning,
                    label = "Suspicious",
                    value = app.suspiciousPermissionCount.toString()
                )
            }
        }
    }
}

@Composable
private fun RiskLevelChip(riskLevel: RiskLevelEntity) {
    val (color, icon, text) = when (riskLevel) {
        RiskLevelEntity.CRITICAL -> Triple(Color(0xFFD32F2F), Icons.Filled.Error, "Critical")
        RiskLevelEntity.HIGH -> Triple(Color(0xFFD32F2F), Icons.Filled.Warning, "High")
        RiskLevelEntity.MEDIUM -> Triple(Color(0xFFF57C00), Icons.Filled.Info, "Medium")
        RiskLevelEntity.LOW -> Triple(Color(0xFF2E7D32), Icons.Filled.CheckCircle, "Low")
        RiskLevelEntity.MINIMAL -> Triple(Color(0xFF4CAF50), Icons.Filled.Done, "Minimal") // ✅ ADDED: MINIMAL branch
        RiskLevelEntity.UNKNOWN -> Triple(Color(0xFF757575), Icons.Filled.HelpOutline, "Unknown")
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
