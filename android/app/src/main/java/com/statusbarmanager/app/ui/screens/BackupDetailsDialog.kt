package com.statusbarmanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.statusbarmanager.app.data.StatusBarBackupModel
import com.statusbarmanager.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupDetailsDialog(
    backup: StatusBarBackupModel?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preserved Baseline",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                if (backup == null) {
                    Text(
                        text = "No backup currently saved in DataStore.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
                        .format(Date(backup.timestamp))

                    Text(
                        text = "Saved on: $dateStr",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                    Text(
                        text = "Device: ${backup.deviceManufacturer} ${backup.deviceModel} (Android ${backup.androidVersion})",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Configuration Snapshot",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BlueAccent
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Time Setting:", color = TextSecondary)
                            Text(backup.originalTimeSetting, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Network Speed:", color = TextSecondary)
                            Text(
                                if (backup.originalRealTimeNetworkSpeed) "Enabled" else "Disabled",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Wired Headphones:", color = TextSecondary)
                            Text(
                                if (backup.hasHeadphoneControl) {
                                    if (backup.originalHeadphoneSetting == true) "Visible" else "Hidden"
                                } else {
                                    "System-managed"
                                },
                                color = if (backup.hasHeadphoneControl) TextPrimary else WarningAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Preserved Status Icons (${backup.originalStatusIcons.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BlueAccent
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(backup.originalStatusIcons.entries.toList()) { (icon, wasEnabled) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(icon, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = if (wasEnabled) "ON" else "OFF",
                                    color = if (wasEnabled) SuccessGreen else TextMuted,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}
