package com.statusbarmanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.statusbarmanager.app.automation.AppItem
import com.statusbarmanager.app.ui.theme.*

/**
 * OBJECTIVE 1.2: Protected Apps / Whitelist Management Screen.
 * - Search installed applications
 * - Filter between All, User Installed, and System/Protected
 * - Add/remove applications to/from local DataStore whitelist
 * - Clear counts: Total eligible, whitelisted, system protected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(
    apps: List<AppItem>,
    whitelist: Set<String>,
    onToggleWhitelist: (packageName: String) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Eligible") } // "Eligible", "Whitelisted", "System / Protected"

    val filteredApps = remember(apps, searchQuery, selectedFilter, whitelist) {
        apps.filter { app ->
            val matchesSearch = app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            if (!matchesSearch) return@filter false

            when (selectedFilter) {
                "Eligible" -> !app.isSystemApp && !app.isProtectedRole && !whitelist.contains(app.packageName)
                "Whitelisted" -> whitelist.contains(app.packageName)
                "System / Protected" -> app.isSystemApp || app.isProtectedRole
                else -> true
            }
        }
    }

    val totalEligible = apps.count { !it.isSystemApp && !it.isProtectedRole && !whitelist.contains(it.packageName) }
    val totalWhitelisted = whitelist.size
    val totalProtected = apps.count { it.isSystemApp || it.isProtectedRole }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Protected Apps / Whitelist",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "$totalEligible eligible for force stop • $totalWhitelisted whitelisted",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("Search installed applications...", color = TextMuted) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluePrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                )
            )

            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTabItem(
                    title = "Eligible ($totalEligible)",
                    isSelected = selectedFilter == "Eligible",
                    onClick = { selectedFilter = "Eligible" }
                )
                FilterTabItem(
                    title = "Whitelisted ($totalWhitelisted)",
                    isSelected = selectedFilter == "Whitelisted",
                    onClick = { selectedFilter = "Whitelisted" }
                )
                FilterTabItem(
                    title = "Protected ($totalProtected)",
                    isSelected = selectedFilter == "System / Protected",
                    onClick = { selectedFilter = "System / Protected" }
                )
            }

            // Notice Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = BlueAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "System components, launchers, keyboards, and whitelisted apps are strictly excluded from Force Stop batches.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )
                }
            }

            // App List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isWhitelisted = whitelist.contains(app.packageName)
                    AppItemRow(
                        app = app,
                        isWhitelisted = isWhitelisted,
                        onToggle = { onToggleWhitelist(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) BluePrimary else DarkSurface,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.height(34.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else TextSecondary
                )
            )
        }
    }
}

@Composable
fun AppItemRow(
    app: AppItem,
    isWhitelisted: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon Placeholder / Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            app.isProtectedRole -> PurpleSurface
                            app.isSystemApp -> Color(0xFF1E293B)
                            isWhitelisted -> EmeraldSurface
                            else -> DarkSurfaceElevated
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        app.isProtectedRole -> Icons.Default.Security
                        app.isSystemApp -> Icons.Default.Settings
                        isWhitelisted -> Icons.Default.VerifiedUser
                        else -> Icons.Default.Apps
                    },
                    contentDescription = null,
                    tint = when {
                        app.isProtectedRole -> PurpleText
                        app.isSystemApp -> TextMuted
                        isWhitelisted -> SuccessEmerald
                        else -> BlueAccent
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )

                if (app.protectionReason != null) {
                    Text(
                        text = "Protected: ${app.protectionReason}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PurpleText,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!app.isSystemApp && !app.isProtectedRole) {
                FilledTonalButton(
                    onClick = onToggle,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isWhitelisted) EmeraldSurface else DarkSurfaceElevated,
                        contentColor = if (isWhitelisted) SuccessEmerald else TextSecondary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isWhitelisted) "Whitelisted" else "Protect",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkSurfaceElevated
                ) {
                    Text(
                        text = if (app.isProtectedRole) "SYSTEM ROLE" else "SYSTEM APP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
