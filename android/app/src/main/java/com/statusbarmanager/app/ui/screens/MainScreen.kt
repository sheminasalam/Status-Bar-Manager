package com.statusbarmanager.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.statusbarmanager.app.automation.*
import com.statusbarmanager.app.data.StatusBarBackupModel
import com.statusbarmanager.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isHideActive: Boolean,
    hasBackup: Boolean,
    backupModel: StatusBarBackupModel?,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    lastOperation: Triple<String, String, Long>,
    automationProgress: AutomationProgress,
    compatibilityManager: DeviceCompatibilityManager,
    engageMode: EngageMode,
    onSelectEngageMode: (EngageMode) -> Unit,
    onOpenWhitelist: () -> Unit,
    onToggleMasterSwitch: (Boolean) -> Unit,
    onRestoreOriginal: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onViewBackupDetails: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isRunning = automationProgress.currentStep != AutomationStep.IDLE &&
            automationProgress.currentStep != AutomationStep.COMPLETED &&
            automationProgress.currentStep != AutomationStep.PARTIAL &&
            automationProgress.currentStep != AutomationStep.FAILED

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BluePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.statusbarmanager.app.R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.statusbarmanager.app.R.string.app_subtitle),
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Alert Banner if Accessibility missing
            if (!isAccessibilityGranted) {
                AccessibilityBanner(onRequestPermission = onRequestAccessibilityPermission)
            }

            // Prominent Master Switch Card
            MasterSwitchCard(
                isHideActive = isHideActive,
                isRunning = isRunning,
                isAccessibilityGranted = isAccessibilityGranted,
                isPartial = backupModel?.isPartial ?: false,
                onToggle = onToggleMasterSwitch
            )

            // Live Comprehensive Automation Progress Panel (Objective 3)
            if (isRunning || automationProgress.currentStep != AutomationStep.IDLE) {
                LiveAutomationProgressPanel(
                    progress = automationProgress,
                    isRunning = isRunning
                )
            }

            // System Status Overview Grid
            StatusOverviewCard(
                isHideActive = isHideActive,
                hasBackup = hasBackup,
                isPartial = backupModel?.isPartial ?: false,
                isAccessibilityGranted = isAccessibilityGranted,
                isOverlayGranted = isOverlayGranted,
                headphoneStatus = automationProgress.headphoneStatus
            )

            // OBJECTIVE 2: Engage Button Behaviour Card
            EngageModeCard(
                currentMode = engageMode,
                onModeSelected = onSelectEngageMode,
                onManageWhitelist = onOpenWhitelist
            )

            // Floating Overlay Permission Card (Display over other apps)
            if (!isOverlayGranted) {
                OverlayPermissionCard(onRequestPermission = onRequestOverlayPermission)
            }

            // Backup & Restoration Card (Objective 4)
            BackupStatusCard(
                hasBackup = hasBackup,
                backup = backupModel,
                isHideActive = isHideActive,
                isRunning = isRunning,
                onRestore = onRestoreOriginal,
                onViewDetails = onViewBackupDetails
            )

            // Device Compatibility Card
            CompatibilityCard(
                oemName = compatibilityManager.oemDisplayName
            )

            // Manual Assistance & Settings Quick Actions
            ManualAssistanceCard(
                onOpenSettings = onOpenSystemSettings,
                onRequestAccessibility = onRequestAccessibilityPermission,
                onRequestOverlay = onRequestOverlayPermission
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MasterSwitchCard(
    isHideActive: Boolean,
    isRunning: Boolean,
    isAccessibilityGranted: Boolean,
    isPartial: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = when {
                    isPartial -> WarningAmber
                    isHideActive -> BlueAccent
                    else -> DarkBorder
                },
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hide Status Bar Information",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            isPartial -> "Clock, network speed & available icons hidden (Wired headphone icon is system-managed)."
                            isHideActive -> "Clock, real-time network speed, and all status icons are currently hidden."
                            else -> "Disables clock, network speed, VoLTE, SIM, Wi-Fi, battery and all available system status icons."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Switch(
                    checked = isHideActive,
                    onCheckedChange = { onToggle(it) },
                    enabled = !isRunning && isAccessibilityGranted,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = if (isPartial) WarningAmber else BluePrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }
        }
    }
}

/**
 * OBJECTIVE 3: Rich Live Progress Panel with horizontal progress bar, percentage,
 * operation checklist, and live icon counters.
 */
@Composable
fun LiveAutomationProgressPanel(
    progress: AutomationProgress,
    isRunning: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BlueAccent.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Title + Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (progress.currentStep) {
                            AutomationStep.COMPLETED -> Icons.Default.CheckCircle
                            AutomationStep.PARTIAL -> Icons.Default.Info
                            AutomationStep.FAILED -> Icons.Default.Error
                            else -> Icons.Default.Sync
                        },
                        contentDescription = null,
                        tint = when (progress.currentStep) {
                            AutomationStep.COMPLETED -> SuccessGreen
                            AutomationStep.PARTIAL -> WarningAmber
                            AutomationStep.FAILED -> ErrorRed
                            else -> BlueAccent
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = progress.operationTitle.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BlueAccent,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Text(
                    text = "${progress.percentage}%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = when (progress.currentStep) {
                            AutomationStep.COMPLETED -> SuccessGreen
                            AutomationStep.PARTIAL -> WarningAmber
                            AutomationStep.FAILED -> ErrorRed
                            else -> BlueAccent
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Determinate Progress Bar
            LinearProgressIndicator(
                progress = { progress.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = when (progress.currentStep) {
                    AutomationStep.COMPLETED -> SuccessGreen
                    AutomationStep.PARTIAL -> WarningAmber
                    AutomationStep.FAILED -> ErrorRed
                    else -> BlueAccent
                },
                trackColor = DarkSurfaceElevated
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Current Operation Text
            Text(
                text = progress.currentOperation,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )

            // Icon Statistics Counter (Live)
            if (progress.iconStats.discovered > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                IconStatisticsBox(stats = progress.iconStats)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Operation Checklist
            Text(
                text = "Process Checklist:",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                progress.checklist.forEach { item ->
                    ChecklistRow(item = item)
                }
            }
        }
    }
}

@Composable
fun IconStatisticsBox(stats: IconStats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = "STATUS ICON METRICS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn(label = "Discovered", count = stats.discovered, color = BlueAccent)
                StatColumn(label = "Disabled", count = stats.disabled, color = SuccessGreen)
                StatColumn(label = "Already Off", count = stats.alreadyDisabled, color = TextMuted)
                StatColumn(label = "Remaining", count = stats.remaining, color = if (stats.remaining > 0) WarningAmber else SuccessGreen)
            }
        }
    }
}

@Composable
fun StatColumn(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TextSecondary)
        )
    }
}

@Composable
fun ChecklistRow(item: ProcessChecklistItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, color, labelText) = when (item.status) {
            StepStatus.COMPLETED -> Triple(Icons.Default.CheckCircle, SuccessGreen, "Completed")
            StepStatus.IN_PROGRESS -> Triple(Icons.Default.Sync, BlueAccent, "In progress")
            StepStatus.PARTIAL -> Triple(Icons.Default.Info, WarningAmber, "Partial")
            StepStatus.FAILED -> Triple(Icons.Default.Cancel, ErrorRed, "Failed")
            StepStatus.SKIPPED -> Triple(Icons.Default.RemoveCircleOutline, TextMuted, "Skipped")
            StepStatus.PENDING -> Triple(Icons.Default.RadioButtonUnchecked, TextMuted, "Pending")
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = item.details ?: labelText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = color,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
fun StatusOverviewCard(
    isHideActive: Boolean,
    hasBackup: Boolean,
    isPartial: Boolean,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    headphoneStatus: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "System Status",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Badge
                val (statusText, statusColor) = when {
                    isPartial -> "Partially hidden" to WarningAmber
                    isHideActive -> "Hidden" to BlueAccent
                    hasBackup -> "Baseline active" to WarningAmber
                    else -> "Normal" to SuccessGreen
                }

                StatusPill(
                    label = "Status",
                    value = statusText,
                    color = statusColor,
                    modifier = Modifier.weight(1f)
                )

                // Accessibility Status Pill
                StatusPill(
                    label = "Accessibility",
                    value = if (isAccessibilityGranted) "Active" to SuccessGreen else "Disabled" to ErrorRed,
                    modifier = Modifier.weight(1f)
                )

                // Floating Overlay Pill
                StatusPill(
                    label = "Floating Overlay",
                    value = if (isOverlayGranted) "Enabled" to SuccessGreen else "Disabled" to TextMuted,
                    modifier = Modifier.weight(1f)
                )
            }

            // Wired Headphone Limitation Note (Objective 2)
            if (isPartial || headphoneStatus.contains("System-managed")) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Wired headphones: Controlled internally by SystemUI (no OEM setting exposed).",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    value: Pair<String, Color>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(value.second)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = value.first,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    StatusPill(label = label, value = value to color, modifier = modifier)
}

@Composable
fun OverlayPermissionCard(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BlueAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                tint = BlueAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Floating Live Progress Panel",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Displays live progress on top of Settings while automation runs.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Enable", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun BackupStatusCard(
    hasBackup: Boolean,
    backup: StatusBarBackupModel?,
    isHideActive: Boolean,
    isRunning: Boolean,
    onRestore: () -> Unit,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Original Settings Baseline",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )

                if (hasBackup) {
                    TextButton(onClick = onViewDetails) {
                        Text("View Baseline", color = BlueAccent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (hasBackup) {
                    "Exact original baseline saved (${backup?.originalStatusIcons?.size ?: 0} icons). Restores Time, speed, and each individual icon."
                } else {
                    "No baseline saved yet. A full snapshot of your current settings will be captured before applying Hide mode."
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onRestore,
                enabled = !isRunning && hasBackup,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceElevated,
                    contentColor = TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Restore Original Settings",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun CompatibilityCard(oemName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = BlueAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Device Compatibility & Target",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = oemName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Target route: Settings → Notifications & status bar → Status bar",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }
    }
}

@Composable
fun AccessibilityBanner(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Accessibility Permission Required",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "SysHush requires Accessibility Service permission to navigate system settings on your behalf without requiring root or ADB commands.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Text("Open Accessibility Settings", color = Color.White)
            }
        }
    }
}

@Composable
fun ManualAssistanceCard(
    onOpenSettings: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "System Tools & Navigation",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Quick shortcuts to verify your status bar layout manually or configure permissions.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Settings", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = onRequestAccessibility,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Accessibility", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = onRequestOverlay,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Overlay", color = TextPrimary)
                }
            }
        }
    }
}

/**
 * OBJECTIVE 2: Engage Button Behaviour selector card.
 * Select between:
 * - Status Bar Only
 * - Force Stop Apps Only
 * - Both (Status Bar + Apps)
 */
@Composable
fun EngageModeCard(
    currentMode: EngageMode,
    onModeSelected: (EngageMode) -> Unit,
    onManageWhitelist: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BluePrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Engage Button Behaviour",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Choose what actions execute when the master Engage switch or Home-Screen Widget is activated.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EngageModeRadioItem(
                    title = "Both (Status Bar + Force Stop Apps)",
                    description = "Hides status bar and force-stops eligible non-system apps in one smooth batch.",
                    isSelected = currentMode == EngageMode.BOTH,
                    onClick = { onModeSelected(EngageMode.BOTH) }
                )

                EngageModeRadioItem(
                    title = "Status Bar Only",
                    description = "Hides clock, network speed, and status icons. Apps are untouched.",
                    isSelected = currentMode == EngageMode.STATUS_BAR_ONLY,
                    onClick = { onModeSelected(EngageMode.STATUS_BAR_ONLY) }
                )

                EngageModeRadioItem(
                    title = "Force Stop Apps Only",
                    description = "Force-stops non-system apps via Settings. Status bar remains untouched.",
                    isSelected = currentMode == EngageMode.FORCE_STOP_APPS_ONLY,
                    onClick = { onModeSelected(EngageMode.FORCE_STOP_APPS_ONLY) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Button to open Protected Apps / Whitelist
            FilledTonalButton(
                onClick = onManageWhitelist,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = DarkSurfaceElevated,
                    contentColor = TextPrimary
                )
            ) {
                Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp), tint = SuccessEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Protected Apps / Whitelist", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun EngageModeRadioItem(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) BlueSurface else DarkSurfaceElevated,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BluePrimary) else androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = BluePrimary,
                    unselectedColor = TextMuted
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
            }
        }
    }
}

