package com.yourname.smartpermissionanalyzer.ui.recommendations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.smartpermissionanalyzer.domain.recommendations.RecommendationPriority
import com.yourname.smartpermissionanalyzer.domain.recommendations.SmartRecommendation
import com.yourname.smartpermissionanalyzer.presentation.viewmodel.RecommendationsViewModel
import com.yourname.smartpermissionanalyzer.ui.theme.ResponsiveDimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    onBackClick: () -> Unit,
    onAppClick: (String) -> Unit,
    viewModel: RecommendationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadRecommendations() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Recommendations", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshRecommendations() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingScreen()
            recommendations.isEmpty() -> EmptyRecommendationsScreen { viewModel.refreshRecommendations() }
            else -> RecommendationsList(
                list = recommendations,
                padding = padding,
                onCardAction = { rec ->
                    viewModel.handleRecommendationAction(rec, context, onAppClick)
                }
            )
        }
    }
}

/* ----------  List ---------- */

@Composable
private fun RecommendationsList(
    list: List<SmartRecommendation>,
    padding: PaddingValues,
    onCardAction: (SmartRecommendation) -> Unit
) {
    val crit = list.filter { it.priority == RecommendationPriority.CRITICAL }
    val high = list.filter { it.priority == RecommendationPriority.HIGH }
    val med  = list.filter { it.priority == RecommendationPriority.MEDIUM }
    val low  = list.filter { it.priority == RecommendationPriority.LOW }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RecommendationsSummaryCard(
                total = list.size,
                critical = crit.size,
                high = high.size
            )
        }

        section("🔥 Critical Priority", crit, Color(0xFFD32F2F), onCardAction)
        section("⚠️ High Priority", high, Color(0xFFFF5722), onCardAction)
        section("📋 Medium Priority", med, Color(0xFFFF9800), onCardAction)
        section("💡 Suggestions", low, Color(0xFF4CAF50), onCardAction)
    }
}

private fun LazyListScope.section(
    title: String,
    recs: List<SmartRecommendation>,
    color: Color,
    onCardAction: (SmartRecommendation) -> Unit
) {
    if (recs.isEmpty()) return
    item {
        RecommendationSectionHeader(
            title = title,
            count = recs.size,
            color = color
        )
    }
    items(recs) { rec ->
        RecommendationCard(
            recommendation = rec,
            priorityColor = color,
            onActionClick = { onCardAction(rec) }
        )
    }
}

/* ----------  Cards ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendationCard(
    recommendation: SmartRecommendation,
    priorityColor: Color,
    onActionClick: () -> Unit
) {
    val appName = extractAppNameForDisplay(recommendation)
    val hasApp = appName.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(priorityColor.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        recommendation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = priorityColor
                    )
                    if (hasApp) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Apps, null, Modifier.size(14.dp), MaterialTheme.colorScheme.primary)
                            Text(
                                "Target App: $appName",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = priorityColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        recommendation.priority.name,
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                recommendation.message,
                style = MaterialTheme.typography.bodyMedium
            )

            if (recommendation.actionable) {
                Spacer(Modifier.height(12.dp))
                val label = when {
                    hasApp -> "Review $appName"
                    recommendation.title.contains("Multiple", true) -> "Review Apps"
                    recommendation.title.contains("Permission", true) -> "Check Permissions"
                    else -> "Take Action"
                }
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(priorityColor),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontSize = ResponsiveDimensions.textSize())
                }
            }
        }
    }
}

/* ----------  Summary ---------- */

@Composable
private fun RecommendationsSummaryCard(
    total: Int,
    critical: Int,
    high: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "AI Security Recommendations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "$total personalized recommendations found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    Icons.Default.AutoAwesome,
                    null,
                    Modifier.size(32.dp),
                    MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (critical > 0 || high > 0) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (critical > 0) {
                        PriorityStatItem(
                            count = critical,
                            label = "Critical",
                            color = Color(0xFFD32F2F),
                            icon = Icons.Default.Error
                        )
                    }
                    if (high > 0) {
                        PriorityStatItem(
                            count = high,
                            label = "High",
                            color = Color(0xFFFF5722),
                            icon = Icons.Default.Warning
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationSectionHeader(
    title: String,
    count: Int,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            "$title ($count)",
            Modifier.padding(12.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun PriorityStatItem(
    count: Int,
    label: String,
    color: Color,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(24.dp), color)
        Spacer(Modifier.height(4.dp))
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

/* ----------  Empty / Loading ---------- */

@Composable
private fun LoadingScreen() = Box(
    Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Generating AI recommendations…")
    }
}

@Composable
private fun EmptyRecommendationsScreen(onRefresh: () -> Unit) = Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Icon(Icons.Default.AutoAwesome, null, Modifier.size(64.dp), MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(16.dp))
    Text(
        "No Recommendations",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Text(
        "Your apps are well-optimized! Run a new scan to get fresh recommendations.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = onRefresh) {
        Icon(Icons.Default.Refresh, null)
        Spacer(Modifier.width(8.dp))
        Text("Refresh Recommendations")
    }
}

/* ----------  Helpers ---------- */

private fun extractAppNameForDisplay(rec: SmartRecommendation): String = try {
    rec.title.split(": ", limit = 2).let {
        if (it.size == 2 && !it[0].contains("Multiple", true) && !it[0].contains("Apps", true))
            return it[0]
    }
    val patterns = listOf("App: ", "app: ", "(App: ")
    patterns.forEach { p ->
        val idx = rec.message.indexOf(p)
        if (idx != -1) {
            val start = idx + p.length
            val end = rec.message.indexOfAny(listOf(")", " ", ","), start)
                .takeIf { it != -1 } ?: rec.message.length
            return rec.message.substring(start, end).trim()
        }
    }
    ""
} catch (_: Exception) { "" }
