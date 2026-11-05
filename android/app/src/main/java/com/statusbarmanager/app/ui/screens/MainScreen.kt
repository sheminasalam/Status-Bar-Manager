package com.statusbarmanager.app.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.statusbarmanager.app.automation.AutomationProgress
import com.statusbarmanager.app.automation.AutomationStep
import com.statusbarmanager.app.automation.DeviceCompatibilityManager
import com.statusbarmanager.app.data.StatusBarBackupModel
import com.statusbarmanager.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isHideActive: Boolean,
    hasBackup: Boolean,
    backupModel: StatusBarBackupModel?,
    isAccessibilityGranted: Boolean,
    lastOperation: Triple<String, String, Long>,
    automationProgress: AutomationProgress,
    compatibilityManager: DeviceCompatibilityManager,
    onToggleMasterSwitch: (Boolean) -> Unit,
    onRestoreOriginal: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onViewBackupDetails: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isRunning = automationProgress.currentStep != AutomationStep.IDLE &&
            automationProgress.currentStep != AutomationStep.COMPLETED &&
            automationProgress.currentStep != AutomationStep.FAILED

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BluePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "StatusBar Manager",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Control the visibility of your Android status bar.",
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
            // Permission Alert Banner if missing
            if (!isAccessibilityGranted) {
                AccessibilityBanner(onRequestPermission = onRequestAccessibilityPermission)
            }

            // Prominent Master Switch Card
            MasterSwitchCard(
                isHideActive = isHideActive,
                isRunning = isRunning,
                isAccessibilityGranted = isAccessibilityGranted,
                onToggle = onToggleMasterSwitch
            )

            // Status Overview Grid (Normal / Hidden / Partially hidden)
            StatusOverviewCard(
                isHideActive = isHideActive,
                hasBackup = hasBackup,
                isAccessibilityGranted = isAccessibilityGranted,
                oemName = compatibilityManager.oemDisplayName
            )

            // Live Automation Progress Banner (when active or recently run)
            if (isRunning || automationProgress.currentStep != AutomationStep.IDLE) {
                AutomationStatusCard(progress = automationProgress)
            }

            // Backup & Restoration Card
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
                oemName = compatibilityManager.oemDisplayName,
                currentOem = compatibilityManager.currentOem.name
            )

            // Manual Assistance & Settings Quick Actions
            ManualAssistanceCard(
                onOpenSettings = onOpenSystemSettings,
                onRequestPermission = onRequestAccessibilityPermission
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
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = if (isHideActive) BlueAccent else DarkBorder,
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
                        text = if (isHideActive) {
                            "Clock, real-time speed, and status icons are currently hidden."
                        } else {
                            "Hides clock, network speed, Wi-Fi, 5G, and status icons across phone."
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
                        checkedTrackColor = BluePrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }

            if (isRunning) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = BlueAccent,
                    trackColor = DarkSurfaceElevated
                )
            }
        }
    }
}

@Composable
fun StatusOverviewCard(
    isHideActive: Boolean,
    hasBackup: Boolean,
    isAccessibilityGranted: Boolean,
    oemName: String
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
                    isHideActive -> "Hidden" to BlueAccent
                    hasBackup -> "Partially hidden" to WarningAmber
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
                    value = if (isAccessibilityGranted) "Active" else "Disabled",
                    color = if (isAccessibilityGranted) SuccessGreen else ErrorRed,
                    modifier = Modifier.weight(1f)
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun AutomationStatusCard(progress: AutomationProgress) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BlueAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BlueContainer.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (progress.currentStep) {
                        AutomationStep.COMPLETED -> Icons.Default.CheckCircle
                        AutomationStep.FAILED -> Icons.Default.Error
                        else -> Icons.Default.Sync
                    },
                    contentDescription = null,
                    tint = when (progress.currentStep) {
                        AutomationStep.COMPLETED -> SuccessGreen
                        AutomationStep.FAILED -> ErrorRed
                        else -> BlueAccent
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Current Operation",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = progress.message,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
            )

            if (progress.totalSteps > 0 && progress.stepIndex > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Step ${progress.stepIndex} of ${progress.totalSteps}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
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
                    text = "Original Settings Backup",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )

                if (hasBackup) {
                    TextButton(onClick = onViewDetails) {
                        Text("View Data", color = BlueAccent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (hasBackup) {
                    "Original state preserved. Restores your exact previous Time setting and individual icon toggles."
                } else {
                    "No active backup. A snapshot of your current settings will be created automatically before hiding."
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )

            Spacer(modifier = Modifier.height(16.dp))

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
fun CompatibilityCard(oemName: String, currentOem: String) {
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
                    text = "Device Compatibility",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = oemName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Automates: Settings → Notifications & status bar → Status bar",
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
                text = "StatusBar Manager requires Accessibility Service permission to navigate system settings on your behalf without requiring root.",
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
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Manual Assistance & Tools",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "If your device layout requires manual verification or custom icon toggles, you can open native Settings directly.",
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
                    Text("Open Settings", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = onRequestPermission,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Accessibility", color = TextPrimary)
                }
            }
        }
    }
}
