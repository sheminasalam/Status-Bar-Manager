package com.statusbarmanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.statusbarmanager.app.automation.DeviceCompatibilityManager
import com.statusbarmanager.app.automation.SettingsAutomationManager
import com.statusbarmanager.app.automation.TargetMode
import com.statusbarmanager.app.data.DataStoreRepository
import com.statusbarmanager.app.data.SettingsBackupManager
import com.statusbarmanager.app.ui.screens.BackupDetailsDialog
import com.statusbarmanager.app.ui.screens.MainScreen
import com.statusbarmanager.app.ui.screens.OnboardingDialog
import com.statusbarmanager.app.ui.theme.StatusBarManagerTheme

class MainActivity : ComponentActivity() {

    private lateinit var dataStoreRepository: DataStoreRepository
    private lateinit var backupManager: SettingsBackupManager
    private lateinit var automationManager: SettingsAutomationManager
    private val compatibilityManager = DeviceCompatibilityManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataStoreRepository = DataStoreRepository(applicationContext)
        backupManager = SettingsBackupManager(dataStoreRepository)
        automationManager = SettingsAutomationManager(applicationContext)

        setContent {
            StatusBarManagerTheme {
                val isHideActive by dataStoreRepository.isHideActiveFlow.collectAsState(initial = false)
                val hasBackup by dataStoreRepository.hasBackupFlow.collectAsState(initial = false)
                val backupModel by dataStoreRepository.backupModelFlow.collectAsState(initial = null)
                val lastOperation by dataStoreRepository.lastOperationFlow.collectAsState(
                    initial = Triple("IDLE", "Ready to configure", 0L)
                )

                var isAccessibilityGranted by remember {
                    mutableStateOf(automationManager.isAccessibilityPermissionGranted())
                }
                var isOverlayGranted by remember {
                    mutableStateOf(automationManager.canDrawOverlays())
                }

                var showOnboardingDialog by remember { mutableStateOf(false) }
                var showBackupDialog by remember { mutableStateOf(false) }

                val automationProgress by automationManager.progressState.collectAsState()

                DisposableEffect(Unit) {
                    isAccessibilityGranted = automationManager.isAccessibilityPermissionGranted()
                    isOverlayGranted = automationManager.canDrawOverlays()
                    onDispose { }
                }

                MainScreen(
                    isHideActive = isHideActive,
                    hasBackup = hasBackup,
                    backupModel = backupModel,
                    isAccessibilityGranted = isAccessibilityGranted,
                    isOverlayGranted = isOverlayGranted,
                    lastOperation = lastOperation,
                    automationProgress = automationProgress,
                    compatibilityManager = compatibilityManager,
                    onToggleMasterSwitch = { shouldHide ->
                        if (!isAccessibilityGranted) {
                            showOnboardingDialog = true
                            return@MainScreen
                        }

                        if (shouldHide) {
                            automationManager.triggerAutomation(TargetMode.HideAll)
                        } else {
                            automationManager.triggerAutomation(TargetMode.RestoreOriginal)
                        }
                    },
                    onRestoreOriginal = {
                        if (!isAccessibilityGranted) {
                            showOnboardingDialog = true
                            return@MainScreen
                        }
                        automationManager.triggerAutomation(TargetMode.RestoreOriginal)
                    },
                    onRequestAccessibilityPermission = {
                        automationManager.openAccessibilitySettings()
                    },
                    onRequestOverlayPermission = {
                        automationManager.openOverlaySettings()
                    },
                    onOpenSystemSettings = {
                        automationManager.openSystemSettings()
                    },
                    onViewBackupDetails = {
                        showBackupDialog = true
                    }
                )

                if (showOnboardingDialog) {
                    OnboardingDialog(
                        onDismiss = { showOnboardingDialog = false },
                        onOpenSettings = {
                            showOnboardingDialog = false
                            automationManager.openAccessibilitySettings()
                        }
                    )
                }

                if (showBackupDialog) {
                    BackupDetailsDialog(
                        backup = backupModel,
                        onDismiss = { showBackupDialog = false }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Permissions re-checked by DisposableEffect
    }
}
