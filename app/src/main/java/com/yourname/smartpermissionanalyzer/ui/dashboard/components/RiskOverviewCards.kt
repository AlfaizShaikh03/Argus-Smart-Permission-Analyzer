package com.yourname.smartpermissionanalyzer.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourname.smartpermissionanalyzer.data.models.AppPermissionInfo
import com.yourname.smartpermissionanalyzer.data.models.RiskLevel

@Composable
fun RiskOverviewCards(analysisResults: List<AppPermissionInfo>) {
    val riskCounts = analysisResults.groupBy { it.riskLevel }.mapValues { it.value.size }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(
            RiskLevel.CRITICAL, RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.LOW
        ).forEach { risk ->
            Card(
                Modifier.weight(1f).padding(4.dp),
                colors = CardDefaults.cardColors(risk.color.copy(alpha = 0.10f))
            ) {
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text("${riskCounts[risk] ?: 0}", style = MaterialTheme.typography.headlineSmall, color = risk.color)
                    Text(risk.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
