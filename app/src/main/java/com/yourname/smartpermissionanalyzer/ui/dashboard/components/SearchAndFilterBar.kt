package com.yourname.smartpermissionanalyzer.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    selectedRiskLevel: String?,
    onSearchChange: (String) -> Unit,
    onRiskFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit
) {
    var showFilterMenu by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filter:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Risk Level Filter
                FilterChip(
                    selected = selectedRiskLevel == "CRITICAL",
                    onClick = {
                        onRiskFilterChange(if (selectedRiskLevel == "CRITICAL") null else "CRITICAL")
                    },
                    label = { Text("Critical") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.error,
                        selectedLabelColor = MaterialTheme.colorScheme.onError
                    )
                )

                FilterChip(
                    selected = selectedRiskLevel == "HIGH",
                    onClick = {
                        onRiskFilterChange(if (selectedRiskLevel == "HIGH") null else "HIGH")
                    },
                    label = { Text("High") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                )

                FilterChip(
                    selected = selectedRiskLevel == "MEDIUM",
                    onClick = {
                        onRiskFilterChange(if (selectedRiskLevel == "MEDIUM") null else "MEDIUM")
                    },
                    label = { Text("Medium") }
                )

                FilterChip(
                    selected = selectedRiskLevel == "LOW",
                    onClick = {
                        onRiskFilterChange(if (selectedRiskLevel == "LOW") null else "LOW")
                    },
                    label = { Text("Low") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )

                // Clear filters button
                if (searchQuery.isNotEmpty() || selectedRiskLevel != null) {
                    TextButton(onClick = onClearFilters) {
                        Text("Clear All")
                    }
                }
            }
        }
    }
}
