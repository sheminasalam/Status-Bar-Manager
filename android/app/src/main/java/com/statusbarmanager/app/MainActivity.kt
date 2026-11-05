package com.statusbarmanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.statusbarmanager.app.automation.*
import com.statusbarmanager.app.data.DataStoreRepository
import com.statusbarmanager.app.data.SettingsBackupManager
import com.statusbarmanager.app.ui.screens.*
import com.statusbarmanager.app.ui.theme.StatusBarManagerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var overlayAccessGranted by mutableStateOf(false)
    private var accessibilityAccessGranted by mutableStateOf(false)

    private lateinit var dataStoreRepository: DataStoreRepository
    private lateinit var backupManager: SettingsBackupManager
    private lateinit var automationManager: SettingsAutomationManager
    private lateinit var coordinator: OperationCoordinator
    private lateinit var applicationManager: ApplicationManager
    private val compatibilityManager = DeviceCompatibilityManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataStoreRepository = DataStoreRepository(applicationContext)
        backupManager = SettingsBackupManager(dataStoreRepository)
        automationManager = SettingsAutomationManager(applicationContext)
        coordinator = OperationCoordinator(applicationContext, dataStoreRepository, backupManager)
        applicationManager = ApplicationManager(applicationContext)
        overlayAccessGranted = automationManager.canDrawOverlays()
        accessibilityAccessGranted = automationManager.isAccessibilityPermissionGranted()
        if (intent.getBooleanExtra("prompt_overlay", false) && !overlayAccessGranted) {
            automationManager.openOverlaySettings()
        }

        setContent {
            StatusBarManagerTheme {
                val coroutineScope = rememberCoroutineScope()

                val isHideActive by dataStoreRepository.isHideActiveFlow.collectAsState(initial = false)
                val hasBackup by dataStoreRepository.hasBackupFlow.collectAsState(initial = false)
                val backupModel by dataStoreRepository.backupModelFlow.collectAsState(initial = null)
                val engageMode by dataStoreRepository.engageModeFlow.collectAsState(initial = EngageMode.BOTH)
                val activeEngagementMode by dataStoreRepository.activeEngagementModeFlow.collectAsState(initial = null)
                val whitelist by dataStoreRepository.whitelistFlow.collectAsState(initial = emptySet())
                val lastOperation by dataStoreRepository.lastOperationFlow.collectAsState(
                    initial = Triple("IDLE", "Ready to configure", 0L)
                )

                val isAccessibilityGranted = accessibilityAccessGranted
                val isOverlayGranted = overlayAccessGranted

                var showOnboardingDialog by remember {
                    mutableStateOf(intent.getBooleanExtra("prompt_accessibility", false))
                }
                var showBackupDialog by remember { mutableStateOf(false) }
                var showWhitelistScreen by remember { mutableStateOf(false) }

                val automationProgress by automationManager.progressState.collectAsState()

                DisposableEffect(Unit) {
                    // Permission states refresh from onResume after returning from Android Settings.
                    if (intent.getBooleanExtra("prompt_accessibility", false)) {
                        showOnboardingDialog = true
                    }
                    onDispose { }
                }

                if (showWhitelistScreen) {
                    val allApps = remember(whitelist) {
                        applicationManager.getInstalledApplications(whitelist)
                    }
                    WhitelistScreen(
                        apps = allApps,
                        whitelist = whitelist,
                        onToggleWhitelist = { pkg ->
                            coroutineScope.launch {
                                if (whitelist.contains(pkg)) {
                                    dataStoreRepository.removeFromWhitelist(pkg)
                                } else {
                                    dataStoreRepository.addToWhitelist(pkg)
                                }
                            }
                        },
                        onBack = { showWhitelistScreen = false }
                    )
                } else {
                    MainScreen(
                        isHideActive = isHideActive,
                        hasBackup = hasBackup,
                        backupModel = backupModel,
                        isAccessibilityGranted = isAccessibilityGranted,
                        isOverlayGranted = isOverlayGranted,
                        lastOperation = lastOperation,
                        automationProgress = automationProgress,
                        compatibilityManager = compatibilityManager,
                        engageMode = engageMode,
                        activeEngagementMode = activeEngagementMode,
                        onSelectEngageMode = { mode ->
                            coroutineScope.launch {
                                dataStoreRepository.setEngageMode(mode)
                            }
                        },
                        onOpenWhitelist = {
                            showWhitelistScreen = true
                        },
                        onToggleMasterSwitch = { shouldEngage ->
                            if (!isAccessibilityGranted) {
                                showOnboardingDialog = true
                                return@MainScreen
                            }

                            if (shouldEngage) {
                                if (!automationManager.canDrawOverlays()) {
                                    automationManager.openOverlaySettings()
                                    return@MainScreen
                                }
                                coroutineScope.launch { coordinator.engage() }
                            } else {
                                coroutineScope.launch { coordinator.disengage() }
                            }
                        },
                        onRestoreOriginal = {
                            if (!isAccessibilityGranted) {
                                showOnboardingDialog = true
                                return@MainScreen
                            }
                            if (!automationManager.canDrawOverlays()) {
                                automationManager.openOverlaySettings()
                                return@MainScreen
                            }
                            coroutineScope.launch { coordinator.disengage() }
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
                }

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
        if (::applicationManager.isInitialized) {
            overlayAccessGranted = automationManager.canDrawOverlays()
            accessibilityAccessGranted = automationManager.isAccessibilityPermissionGranted()
        }
    }
}
