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
import com.statusbarmanager.app.ui.screens.MainScreen
import com.statusbarmanager.app.ui.theme.StatusBarManagerTheme
import kotlinx.coroutines.launch

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
                val coroutineScope = rememberCoroutineScope()

                // State from DataStore and Accessibility Service
                val isHideActive by dataStoreRepository.isHideActiveFlow.collectAsState(initial = false)
                val hasBackup by dataStoreRepository.hasBackupFlow.collectAsState(initial = false)
                val backupModel by dataStoreRepository.backupModelFlow.collectAsState(initial = null)
                val lastOperation by dataStoreRepository.lastOperationFlow.collectAsState(
                    initial = Triple("IDLE", "Ready to configure", 0L)
                )

                // Track Accessibility Permission
                var isAccessibilityGranted by remember {
                    mutableStateOf(automationManager.isAccessibilityPermissionGranted())
                }

                // Automation Progress from service
                val automationProgress by automationManager.progressState.collectAsState()

                // Update accessibility state whenever activity resumes
                DisposableEffect(Unit) {
                    isAccessibilityGranted = automationManager.isAccessibilityPermissionGranted()
                    onDispose { }
                }

                MainScreen(
                    isHideActive = isHideActive,
                    hasBackup = hasBackup,
                    backupModel = backupModel,
                    isAccessibilityGranted = isAccessibilityGranted,
                    lastOperation = lastOperation,
                    automationProgress = automationProgress,
                    compatibilityManager = compatibilityManager,
                    onToggleMasterSwitch = { shouldHide ->
                        if (!isAccessibilityGranted) {
                            automationManager.openAccessibilitySettings()
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
                            automationManager.openAccessibilitySettings()
                            return@MainScreen
                        }
                        automationManager.triggerAutomation(TargetMode.RestoreOriginal)
                    },
                    onRequestAccessibilityPermission = {
                        automationManager.openAccessibilitySettings()
                    },
                    onOpenSystemSettings = {
                        automationManager.openSystemSettings()
                    },
                    onViewBackupDetails = {
                        // View backup details action
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recheck accessibility permission on returning from system settings
    }
}
