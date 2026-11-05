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

        setContent {
            StatusBarManagerTheme {
                val coroutineScope = rememberCoroutineScope()

                val isHideActive by dataStoreRepository.isHideActiveFlow.collectAsState(initial = false)
                val hasBackup by dataStoreRepository.hasBackupFlow.collectAsState(initial = false)
                val backupModel by dataStoreRepository.backupModelFlow.collectAsState(initial = null)
                val engageMode by dataStoreRepository.engageModeFlow.collectAsState(initial = EngageMode.BOTH)
                val whitelist by dataStoreRepository.whitelistFlow.collectAsState(initial = emptySet())
                val lastOperation by dataStoreRepository.lastOperationFlow.collectAsState(
                    initial = Triple("IDLE", "Ready to configure", 0L)
                )

                var isAccessibilityGranted by remember {
                    mutableStateOf(automationManager.isAccessibilityPermissionGranted())
                }
                var isOverlayGranted by remember {
                    mutableStateOf(automationManager.canDrawOverlays())
                }

                var showOnboardingDialog by remember {
                    mutableStateOf(intent.getBooleanExtra("prompt_accessibility", false))
                }
                var showBackupDialog by remember { mutableStateOf(false) }
                var showWhitelistScreen by remember { mutableStateOf(false) }
                var showForceStopConfirmDialog by remember {
                    mutableStateOf(intent.getBooleanExtra("confirm_force_stop", false))
                }

                val automationProgress by automationManager.progressState.collectAsState()

                DisposableEffect(Unit) {
                    isAccessibilityGranted = automationManager.isAccessibilityPermissionGranted()
                    isOverlayGranted = automationManager.canDrawOverlays()
                    if (intent.getBooleanExtra("confirm_force_stop", false)) {
                        showForceStopConfirmDialog = true
                    }
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
                                // If mode includes force-stopping apps, show safety confirmation dialog first
                                if (engageMode == EngageMode.FORCE_STOP_APPS_ONLY || engageMode == EngageMode.BOTH) {
                                    showForceStopConfirmDialog = true
                                } else {
                                    coroutineScope.launch { coordinator.engage() }
                                }
                            } else {
                                coroutineScope.launch { coordinator.disengage() }
                            }
                        },
                        onRestoreOriginal = {
                            if (!isAccessibilityGranted) {
                                showOnboardingDialog = true
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

                if (showForceStopConfirmDialog) {
                    val eligible = remember(whitelist) { applicationManager.getEligibleAppsForForceStop(whitelist).size }
                    val allApps = remember(whitelist) { applicationManager.getInstalledApplications(whitelist) }
                    val protectedCount = allApps.count { it.isSystemApp || it.isProtectedRole }

                    ForceStopConfirmationDialog(
                        eligibleCount = eligible,
                        whitelistedCount = whitelist.size,
                        protectedCount = protectedCount,
                        onConfirm = {
                            showForceStopConfirmDialog = false
                            coroutineScope.launch { coordinator.engage() }
                        },
                        onDismiss = { showForceStopConfirmDialog = false }
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
    }
}
