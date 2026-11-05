package com.statusbarmanager.app.automation

import android.content.Context
import android.util.Log
import com.statusbarmanager.app.data.DataStoreRepository
import com.statusbarmanager.app.data.SettingsBackupManager
import com.statusbarmanager.app.service.StatusBarAccessibilityService
import com.statusbarmanager.app.widget.StatusBarWidgetProvider
import kotlinx.coroutines.flow.StateFlow

/**
 * OBJECTIVE 2: Central Operation Controller.
 * Unified coordinator handling all entry points:
 * - Main application Engage button
 * - Main application Disengage button
 * - Home-screen widget
 * - Quick actions
 *
 * Ensures no duplicate automation, safe partial-failure recovery,
 * and respects the 3 selectable modes: STATUS_BAR_ONLY, FORCE_STOP_APPS_ONLY, BOTH.
 */
class OperationCoordinator(
    private val context: Context,
    private val dataStoreRepository: DataStoreRepository,
    private val backupManager: SettingsBackupManager
) {

    companion object {
        private const val TAG = "OperationCoordinator"
    }

    val isServiceConnected: StateFlow<Boolean> = StatusBarAccessibilityService.isServiceConnected
    val progressState: StateFlow<AutomationProgress> = StatusBarAccessibilityService.progressState

    /**
     * Executes the Engage action according to the currently configured EngageMode.
     */
    suspend fun engage(): Boolean {
        val service = StatusBarAccessibilityService.instance
        if (service == null) {
            Log.e(TAG, "Cannot engage: StatusBarAccessibilityService is not connected")
            return false
        }

        val mode = dataStoreRepository.getEngageMode()
        Log.i(TAG, "Initiating Engage with mode: $mode")

        val targetMode = when (mode) {
            EngageMode.STATUS_BAR_ONLY -> TargetMode.HideAll
            EngageMode.FORCE_STOP_APPS_ONLY -> TargetMode.ForceStopOnly
            EngageMode.BOTH -> TargetMode.BothEngage
        }

        service.startAutomation(targetMode)
        StatusBarWidgetProvider.updateAllWidgets(context)
        return true
    }

    /**
     * Executes the Disengage action according to the currently configured EngageMode.
     * Restores original status bar configuration. NEVER relaunches or restarts stopped apps.
     */
    suspend fun disengage(): Boolean {
        val service = StatusBarAccessibilityService.instance
        if (service == null) {
            Log.e(TAG, "Cannot disengage: StatusBarAccessibilityService is not connected")
            return false
        }

        val mode = dataStoreRepository.getEngageMode()
        Log.i(TAG, "Initiating Disengage with mode: $mode")

        val targetMode = when (mode) {
            EngageMode.STATUS_BAR_ONLY -> TargetMode.RestoreOriginal
            EngageMode.FORCE_STOP_APPS_ONLY -> {
                // In Force Stop Only mode, disengaging performs no app restart
                dataStoreRepository.setHideActive(false)
                dataStoreRepository.recordOperation("SUCCESS", "Apps remain in stopped state. Status bar was untouched.")
                StatusBarWidgetProvider.updateAllWidgets(context)
                return true
            }
            EngageMode.BOTH -> TargetMode.BothDisengage
        }

        service.startAutomation(targetMode)
        StatusBarWidgetProvider.updateAllWidgets(context)
        return true
    }
}
