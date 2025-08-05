package com.yourname.smartpermissionanalyzer.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.smartpermissionanalyzer.data.models.AppPermissionInfo
import com.yourname.smartpermissionanalyzer.data.models.RiskLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedAppRiskCard(
    app: AppPermissionInfo,
    onClick: () -> Unit,
    onTrustClick: () -> Unit,
    onFlagClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (app.riskLevel) {
                RiskLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                RiskLevel.HIGH -> Color(0xFFFFF3E0)
                RiskLevel.MEDIUM -> Color(0xFFFFFDE7)
                RiskLevel.LOW -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon Placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${app.permissions.size} permissions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RiskBadge(riskLevel = app.riskLevel)
            }

            if (app.suspiciousPermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${app.suspiciousPermissions.size} suspicious permissions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onTrustClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Trust")
                }

                OutlinedButton(
                    onClick = onFlagClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Flag, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Flag")
                }
            }
        }
    }
}

@Composable
private fun RiskBadge(riskLevel: RiskLevel) {
    val (color, text) = when (riskLevel) {
        RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error to "CRITICAL"
        RiskLevel.HIGH -> Color(0xFFFF9800) to "HIGH"
        RiskLevel.MEDIUM -> Color(0xFFFFC107) to "MEDIUM"
        RiskLevel.LOW -> Color(0xFF4CAF50) to "LOW"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// Extension function for RiskLevel.displayName
val RiskLevel.displayName: String
    get() = when (this) {
        RiskLevel.CRITICAL -> "Critical Risk"
        RiskLevel.HIGH -> "High Risk"
        RiskLevel.MEDIUM -> "Medium Risk"
        RiskLevel.LOW -> "Low Risk"
    }
