package com.yourname.smartpermissionanalyzer.ui.notifications

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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.smartpermissionanalyzer.domain.entities.NotificationItem
import com.yourname.smartpermissionanalyzer.domain.entities.NotificationPriority
import com.yourname.smartpermissionanalyzer.domain.entities.NotificationType
import com.yourname.smartpermissionanalyzer.presentation.viewmodel.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    onAppDetailsClick: (String) -> Unit = { },
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val filteredNotifications by viewModel.filteredNotifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val showOnlyUnread by viewModel.showOnlyUnread.collectAsState()
    val context = LocalContext.current

    // ✅ NEW: Advanced filtering states
    var selectedPriority by remember { mutableStateOf<NotificationPriority?>(null) }
    var showBulkActions by remember { mutableStateOf(false) }
    var selectedNotifications by remember { mutableStateOf(setOf<String>()) }
    var sortOrder by remember { mutableStateOf(SortOrder.NEWEST_FIRST) }

    // ✅ NEW: Notification statistics
    val notificationStats = remember(filteredNotifications) {
        calculateNotificationStats(filteredNotifications)
    }

    LaunchedEffect(filteredNotifications, unreadCount) {
        android.util.Log.d("NotificationScreen", "Notifications: ${filteredNotifications.size}, Unread: $unreadCount")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Security Notifications",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "$unreadCount unread • ${filteredNotifications.size} total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // ✅ ENHANCED: Bulk actions toggle
                    if (filteredNotifications.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                showBulkActions = !showBulkActions
                                if (!showBulkActions) selectedNotifications = emptySet()
                            }
                        ) {
                            Icon(
                                if (showBulkActions) Icons.Default.CheckBoxOutlineBlank else Icons.Default.Checklist,
                                contentDescription = "Bulk actions",
                                tint = if (showBulkActions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // ✅ ENHANCED: Mark all as read with animation
                    if (unreadCount > 0) {
                        IconButton(
                            onClick = {
                                android.util.Log.d("NotificationScreen", "Mark all as read clicked")
                                viewModel.markAllAsRead()
                            }
                        ) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ) {
                                Text(
                                    unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Icon(Icons.Outlined.DoneAll, contentDescription = "Mark all as read")
                        }
                    }

                    // ✅ ENHANCED: Advanced filter menu
                    var showFilterMenu by remember { mutableStateOf(false) }

                    Box {
                        IconButton(
                            onClick = { showFilterMenu = true }
                        ) {
                            Icon(
                                if (selectedFilter != null || showOnlyUnread || selectedPriority != null)
                                    Icons.Default.FilterAlt
                                else
                                    Icons.Outlined.FilterAlt,
                                contentDescription = "Filter options",
                                tint = if (selectedFilter != null || showOnlyUnread || selectedPriority != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Show only unread") },
                                onClick = {
                                    viewModel.toggleUnreadFilter()
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        if (showOnlyUnread) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null
                                    )
                                }
                            )

                            Divider()

                            DropdownMenuItem(
                                text = { Text("Sort by newest") },
                                onClick = {
                                    sortOrder = SortOrder.NEWEST_FIRST
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        if (sortOrder == SortOrder.NEWEST_FIRST) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Sort by priority") },
                                onClick = {
                                    sortOrder = SortOrder.BY_PRIORITY
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        if (sortOrder == SortOrder.BY_PRIORITY) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null
                                    )
                                }
                            )

                            Divider()

                            DropdownMenuItem(
                                text = { Text("Clear all filters") },
                                onClick = {
                                    viewModel.setFilter(null)
                                    selectedPriority = null
                                    if (showOnlyUnread) viewModel.toggleUnreadFilter()
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ClearAll, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        },
        // ✅ NEW: Floating Action Button for bulk actions
        floatingActionButton = {
            if (showBulkActions && selectedNotifications.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        selectedNotifications.forEach { notificationId ->
                            viewModel.deleteNotification(notificationId)
                        }
                        selectedNotifications = emptySet()
                    },
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete selected",
                        tint = Color.White
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ✅ NEW: Notification Statistics Card
            NotificationStatsCard(
                stats = notificationStats,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ✅ ENHANCED: Advanced Filter Row
            AdvancedFilterRow(
                selectedFilter = selectedFilter,
                selectedPriority = selectedPriority,
                sortOrder = sortOrder,
                onFilterSelected = {
                    android.util.Log.d("NotificationScreen", "Filter selected: $it")
                    viewModel.setFilter(it)
                },
                onPrioritySelected = { selectedPriority = it },
                onSortOrderChanged = { sortOrder = it }
            )

            // ✅ ENHANCED: Bulk Actions Bar
            AnimatedVisibility(
                visible = showBulkActions,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                BulkActionsBar(
                    selectedCount = selectedNotifications.size,
                    totalCount = filteredNotifications.size,
                    onSelectAll = {
                        selectedNotifications = if (selectedNotifications.size == filteredNotifications.size) {
                            emptySet()
                        } else {
                            filteredNotifications.map { it.id }.toSet()
                        }
                    },
                    onMarkSelectedAsRead = {
                        selectedNotifications.forEach { notificationId ->
                            viewModel.markAsRead(notificationId)
                        }
                        selectedNotifications = emptySet()
                    },
                    onDeleteSelected = {
                        selectedNotifications.forEach { notificationId ->
                            viewModel.deleteNotification(notificationId)
                        }
                        selectedNotifications = emptySet()
                    }
                )
            }

            if (filteredNotifications.isEmpty()) {
                EmptyNotificationsCard(
                    hasFilter = selectedFilter != null || showOnlyUnread || selectedPriority != null,
                    onClearFilter = {
                        android.util.Log.d("NotificationScreen", "Clear filter clicked")
                        viewModel.setFilter(null)
                        selectedPriority = null
                        if (showOnlyUnread) viewModel.toggleUnreadFilter()
                    }
                )
            } else {
                // ✅ ENHANCED: Sorted and filtered notifications
                val sortedNotifications = remember(filteredNotifications, sortOrder, selectedPriority) {
                    var filtered = filteredNotifications

                    // Filter by priority
                    if (selectedPriority != null) {
                        filtered = filtered.filter { it.priority == selectedPriority }
                    }

                    // Sort notifications
                    when (sortOrder) {
                        SortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.timestamp }
                        SortOrder.OLDEST_FIRST -> filtered.sortedBy { it.timestamp }
                        SortOrder.BY_PRIORITY -> filtered.sortedByDescending { it.priority.ordinal }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedNotifications, key = { it.id }) { notif ->
                        EnhancedNotificationCard(
                            notification = notif,
                            isSelected = notif.id in selectedNotifications,
                            showBulkActions = showBulkActions,
                            onSelectionChanged = { selected ->
                                selectedNotifications = if (selected) {
                                    selectedNotifications + notif.id
                                } else {
                                    selectedNotifications - notif.id
                                }
                            },
                            onMarkAsRead = {
                                android.util.Log.d("NotificationScreen", "Mark as read clicked for: ${notif.id}")
                                viewModel.markAsRead(notif.id)
                            },
                            onDelete = {
                                android.util.Log.d("NotificationScreen", "Delete clicked for: ${notif.id}")
                                viewModel.deleteNotification(notif.id)
                            },
                            onTakeAction = {
                                android.util.Log.d("NotificationScreen", "Take action clicked for: ${notif.id}")
                                handleNotificationTakeAction(
                                    notification = notif,
                                    context = context,
                                    viewModel = viewModel,
                                    onAppDetailsClick = onAppDetailsClick
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ✅ NEW: Notification Statistics Card
@Composable
private fun NotificationStatsCard(
    stats: NotificationStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.Error,
                label = "Critical",
                value = stats.criticalCount,
                color = Color(0xFFD32F2F)
            )

            StatItem(
                icon = Icons.Default.Warning,
                label = "High",
                value = stats.highCount,
                color = Color(0xFFFF5722)
            )

            StatItem(
                icon = Icons.Default.Info,
                label = "Medium",
                value = stats.mediumCount,
                color = Color(0xFFFF9800)
            )

            StatItem(
                icon = Icons.Default.CheckCircle,
                label = "Low",
                value = stats.lowCount,
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            value.toString(),
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

// ✅ ENHANCED: Advanced Filter Row with Priority and Sort
@Composable
private fun AdvancedFilterRow(
    selectedFilter: NotificationType?,
    selectedPriority: NotificationPriority?,
    sortOrder: SortOrder,
    onFilterSelected: (NotificationType?) -> Unit,
    onPrioritySelected: (NotificationPriority?) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Type filters
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    onClick = { onFilterSelected(null) },
                    label = { Text("All") },
                    selected = selectedFilter == null
                )
            }

            items(NotificationType.values()) { type ->
                FilterChip(
                    onClick = { onFilterSelected(type) },
                    label = {
                        Text(
                            type.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() }
                        )
                    },
                    selected = selectedFilter == type,
                    leadingIcon = {
                        Icon(
                            getNotificationIcon(type),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Priority filters
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    onClick = { onPrioritySelected(null) },
                    label = { Text("All Priorities") },
                    selected = selectedPriority == null
                )
            }

            items(NotificationPriority.values()) { priority ->
                FilterChip(
                    onClick = { onPrioritySelected(priority) },
                    label = {
                        Text(
                            priority.name.lowercase()
                                .replaceFirstChar { it.uppercase() }
                        )
                    },
                    selected = selectedPriority == priority,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = getPriorityColor(priority).copy(alpha = 0.2f),
                        selectedLabelColor = getPriorityColor(priority)
                    )
                )
            }
        }
    }
}

// ✅ NEW: Bulk Actions Bar
@Composable
private fun BulkActionsBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onMarkSelectedAsRead: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedCount == totalCount && totalCount > 0,
                    onCheckedChange = { onSelectAll() },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "$selectedCount selected",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedCount > 0) {
                    OutlinedButton(
                        onClick = onMarkSelectedAsRead,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Read", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onDeleteSelected,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ✅ ENHANCED: Notification Card with Selection Support
@Composable
private fun EnhancedNotificationCard(
    notification: NotificationItem,
    isSelected: Boolean,
    showBulkActions: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    onTakeAction: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    val isSmallScreen = screenWidth < 400
    val buttonHeight = if (isSmallScreen) 36.dp else 44.dp
    val buttonPadding = if (isSmallScreen) 6.dp else 8.dp
    val iconSize = if (isSmallScreen) 14.dp else 16.dp
    val buttonTextSize = if (isSmallScreen) 11.sp else 14.sp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(12.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                !notification.isRead -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 2.dp else 4.dp
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header with selection checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ✅ Selection checkbox for bulk actions
                    if (showBulkActions) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = onSelectionChanged,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // ✅ Priority indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                getPriorityColor(notification.priority),
                                CircleShape
                            )
                    )

                    Icon(
                        getNotificationIcon(notification.type),
                        contentDescription = null,
                        tint = getPriorityColor(notification.priority),
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (notification.isRead)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ✅ Priority badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = getPriorityColor(notification.priority).copy(alpha = 0.1f)
                    ) {
                        Text(
                            notification.priority.name.lowercase().replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = getPriorityColor(notification.priority),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Message
            Text(
                notification.message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (notification.isRead)
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // ✅ Enhanced timestamp with relative and absolute time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatTimestamp(notification.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Text(
                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(notification.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // Action buttons (same as before, but with enhanced styling)
            if (!notification.isRead || notification.actionRequired) {
                Spacer(Modifier.height(12.dp))

                when {
                    notification.isRead && notification.actionRequired -> {
                        Button(
                            onClick = {
                                android.util.Log.d("NotificationCard", "Take Action clicked - read notification")
                                onTakeAction()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(buttonHeight),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = buttonPadding, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(iconSize))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Take Action",
                                fontSize = buttonTextSize,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    !notification.isRead && notification.actionRequired -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    android.util.Log.d("NotificationCard", "Mark as Read clicked - unread with action")
                                    onMarkAsRead()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(buttonHeight),
                                contentPadding = PaddingValues(horizontal = buttonPadding, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, null, Modifier.size(iconSize))
                                if (!isSmallScreen) {
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Read",
                                        fontSize = buttonTextSize,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    android.util.Log.d("NotificationCard", "Delete clicked - unread with action")
                                    onDelete()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(buttonHeight),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = buttonPadding, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Close, null, Modifier.size(iconSize))
                                if (!isSmallScreen) {
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Dismiss",
                                        fontSize = buttonTextSize,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                android.util.Log.d("NotificationCard", "Take Action clicked - unread with action")
                                onTakeAction()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(buttonHeight),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = buttonPadding, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(iconSize))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Take Action",
                                fontSize = buttonTextSize,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    !notification.isRead -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    android.util.Log.d("NotificationCard", "Mark as Read clicked - unread no action")
                                    onMarkAsRead()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(buttonHeight),
                                contentPadding = PaddingValues(horizontal = buttonPadding, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, null, Modifier.size(iconSize))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (isSmallScreen) "Read" else "Mark as Read",
                                    fontSize = buttonTextSize,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    android.util.Log.d("NotificationCard", "Delete clicked - unread no action")
                                    onDelete()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(buttonHeight),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = buttonPadding, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Close, null, Modifier.size(iconSize))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Dismiss",
                                    fontSize = buttonTextSize,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ✅ Your existing helper functions remain the same
private fun handleNotificationTakeAction(
    notification: NotificationItem,
    context: android.content.Context,
    viewModel: NotificationsViewModel,
    onAppDetailsClick: (String) -> Unit
) {
    android.util.Log.d("NotificationScreen", "Handling take action for: ${notification.id}")

    val packageName = extractPackageNameFromNotification(notification)
    val appName = extractAppNameFromNotification(notification)

    when (notification.type) {
        NotificationType.THREAT_DETECTED, NotificationType.SECURITY_ALERT -> {
            if (packageName.isNotEmpty()) {
                android.widget.Toast.makeText(
                    context,
                    "📱 Opening security analysis for $appName",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                onAppDetailsClick(packageName)
                viewModel.markAsRead(notification.id)
            } else {
                android.widget.Toast.makeText(
                    context,
                    "🔍 Review high-risk apps in main dashboard",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                viewModel.markAsRead(notification.id)
            }
        }

        NotificationType.APP_INSTALLED -> {
            if (packageName.isNotEmpty()) {
                android.widget.Toast.makeText(
                    context,
                    "📱 Reviewing new app: $appName",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                onAppDetailsClick(packageName)
                viewModel.markAsRead(notification.id)
            } else {
                android.widget.Toast.makeText(
                    context,
                    "📱 Check new apps in main dashboard",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                viewModel.markAsRead(notification.id)
            }
        }

        NotificationType.PERMISSION_CHANGED -> {
            if (packageName.isNotEmpty()) {
                android.widget.Toast.makeText(
                    context,
                    "🔒 Reviewing permissions for $appName",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                onAppDetailsClick(packageName)
                viewModel.markAsRead(notification.id)
            } else {
                android.widget.Toast.makeText(
                    context,
                    "🔒 Review app permissions in main dashboard",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                viewModel.markAsRead(notification.id)
            }
        }

        NotificationType.SCAN_COMPLETED -> {
            android.widget.Toast.makeText(
                context,
                "📊 Navigate back to see scan results",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            viewModel.markAsRead(notification.id)
        }

        NotificationType.SYSTEM_UPDATE -> {
            android.widget.Toast.makeText(
                context,
                "⚙️ Opening system settings...",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            try {
                val intent = android.content.Intent("android.settings.SYSTEM_UPDATE_SETTINGS")
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    val fallbackIntent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                    fallbackIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(fallbackIntent)
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    context,
                    "Unable to open system settings",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            viewModel.markAsRead(notification.id)
        }
    }
}

private fun extractPackageNameFromNotification(notification: NotificationItem): String {
    return try {
        val message = notification.message.lowercase()

        val appMappings = mapOf(
            "adobe scan" to "com.adobe.scan.android",
            "tiktok" to "com.zhiliaoapp.musically",
            "instagram" to "com.instagram.android",
            "whatsapp" to "com.whatsapp",
            "facebook" to "com.facebook.katana",
            "telegram" to "org.telegram.messenger",
            "chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "youtube" to "com.google.android.youtube",
            "spotify" to "com.spotify.music",
            "netflix" to "com.netflix.mediaclient"
        )

        for ((appName, packageName) in appMappings) {
            if (message.contains(appName)) {
                return packageName
            }
        }

        ""
    } catch (e: Exception) {
        ""
    }
}

private fun extractAppNameFromNotification(notification: NotificationItem): String {
    return try {
        val message = notification.message

        val patterns = listOf(
            Regex("""([A-Z][a-zA-Z\s]+) has"""),
            Regex("""([A-Z][a-zA-Z\s]+) requests"""),
            Regex("""([A-Z][a-zA-Z\s]+) can access""")
        )

        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }
        }

        ""
    } catch (e: Exception) {
        ""
    }
}

@Composable
private fun EmptyNotificationsCard(
    hasFilter: Boolean,
    onClearFilter: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.NotificationsOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (hasFilter) "No Matching Notifications" else "No Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (hasFilter)
                    "Try adjusting your filters to see more notifications."
                else
                    "You're all caught up! No new security alerts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (hasFilter) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onClearFilter) {
                    Text("Clear Filters")
                }
            }
        }
    }
}

// ✅ NEW: Data classes and helper functions
data class NotificationStats(
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int
)

enum class SortOrder {
    NEWEST_FIRST, OLDEST_FIRST, BY_PRIORITY
}

private fun calculateNotificationStats(notifications: List<NotificationItem>): NotificationStats {
    return NotificationStats(
        criticalCount = notifications.count { it.priority == NotificationPriority.CRITICAL },
        highCount = notifications.count { it.priority == NotificationPriority.HIGH },
        mediumCount = notifications.count { it.priority == NotificationPriority.MEDIUM },
        lowCount = notifications.count { it.priority == NotificationPriority.LOW }
    )
}

// Helper functions (unchanged)
private fun getNotificationIcon(type: NotificationType): ImageVector = when (type) {
    NotificationType.SECURITY_ALERT -> Icons.Default.Warning
    NotificationType.SCAN_COMPLETED -> Icons.Default.CheckCircle
    NotificationType.APP_INSTALLED -> Icons.Default.GetApp
    NotificationType.PERMISSION_CHANGED -> Icons.Default.Security
    NotificationType.THREAT_DETECTED -> Icons.Default.Error
    NotificationType.SYSTEM_UPDATE -> Icons.Default.SystemUpdate
}

private fun getPriorityColor(priority: NotificationPriority): Color = when (priority) {
    NotificationPriority.LOW -> Color(0xFF4CAF50)
    NotificationPriority.MEDIUM -> Color(0xFFF57C00)
    NotificationPriority.HIGH -> Color(0xFFFF5722)
    NotificationPriority.CRITICAL -> Color(0xFFD32F2F)
}

private fun formatTimestamp(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(ts))
    }
}
