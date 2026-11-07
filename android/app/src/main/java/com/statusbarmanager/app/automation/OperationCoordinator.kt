package com.statusbarmanager.app.automation

import android.content.Context
import android.util.Log
import com.statusbarmanager.app.data.DataStoreRepository
import com.statusbarmanager.app.data.SettingsBackupManager
import com.statusbarmanager.app.service.StatusBarAccessibilityService
import com.statusbarmanager.app.widget.StatusBarWidgetProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

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
    suspend fun engage(fromWidget: Boolean = false): Boolean {
        val service = StatusBarAccessibilityService.instance
        if (service == null) {
            Log.e(TAG, "Cannot engage: StatusBarAccessibilityService is not connected")
            return false
        }

        val mode = dataStoreRepository.getEngageMode()
        Log.i(TAG, "Initiating Engage with mode: $mode (fromWidget=$fromWidget)")
        // Only status-bar modes create a persistent engagement. App-only
        // force stop is a one-shot action and must leave Hush Button state unchanged.
        if (mode == EngageMode.FORCE_STOP_APPS_ONLY) {
            dataStoreRepository.clearActiveEngagementMode()
        } else {
            dataStoreRepository.setActiveEngagementMode(mode)
        }

        val targetMode = when (mode) {
            EngageMode.STATUS_BAR_ONLY -> TargetMode.HideAll
            EngageMode.FORCE_STOP_APPS_ONLY -> TargetMode.ForceStopOnly
            EngageMode.BOTH -> TargetMode.BothEngage
        }

        service.startAutomation(targetMode, returnToHome = fromWidget)
        StatusBarWidgetProvider.updateAllWidgets(context)
        return true
    }

    /**
     * Toggles only the status-bar workflow without changing the configured Hush Button mode.
     * Restoring the status bar never relaunches apps that were force-stopped earlier.
     */
    suspend fun toggleStatusBarOnly(fromWidget: Boolean = false): Boolean {
        val isCurrentlyActive = dataStoreRepository.isHideActiveFlow.first()
        val activeMode = dataStoreRepository.getActiveEngagementMode()
        if (isCurrentlyActive && activeMode != EngageMode.FORCE_STOP_APPS_ONLY) {
            return disengage(fromWidget)
        }

        val service = StatusBarAccessibilityService.instance
        if (service == null) {
            Log.e(TAG, "Cannot hide status bar: accessibility service is unavailable")
            return false
        }

        dataStoreRepository.setActiveEngagementMode(EngageMode.STATUS_BAR_ONLY)
        service.startAutomation(TargetMode.HideAll, returnToHome = fromWidget)
        StatusBarWidgetProvider.updateAllWidgets(context)
        return true
    }

    /** Runs the dedicated app-only widget action without changing the configured Hush Button mode. */
    suspend fun forceStopAppsOnly(fromWidget: Boolean = false): Boolean {
        val service = StatusBarAccessibilityService.instance
        if (service == null) {
            Log.e(TAG, "Cannot stop running apps: accessibility service is unavailable")
            return false
        }
        // A dedicated Stop Apps action is one-shot. Keep any existing
        // status-bar engagement untouched, and do not create a fake active mode.
        service.startAutomation(TargetMode.ForceStopOnly, returnToHome = fromWidget)
        StatusBarWidgetProvider.updateAllWidgets(context)
        return true
    }

    /**
     * Executes the Disengage action according to the previously engaged mode.
     * Restores original status bar configuration only if status bar was actually hidden.
     * NEVER relaunches or restarts stopped apps.
     */
    suspend fun disengage(fromWidget: Boolean = false): Boolean {
        val service = StatusBarAccessibilityService.instance
        if (service == null) {
            Log.e(TAG, "Cannot disengage: StatusBarAccessibilityService is not connected")
            return false
        }

        // Read the actual mode that was engaged, falling back to configured mode
        val activeMode = dataStoreRepository.getActiveEngagementMode() ?: dataStoreRepository.getEngageMode()
        Log.i(TAG, "Initiating Disengage with active engagement mode: $activeMode (fromWidget=$fromWidget)")

        val targetMode = when (activeMode) {
            EngageMode.STATUS_BAR_ONLY -> TargetMode.RestoreOriginal
            EngageMode.FORCE_STOP_APPS_ONLY -> {
                // In Force Stop Only mode, disengaging performs no app restart and leaves status bar untouched
                dataStoreRepository.setHideActive(false)
                dataStoreRepository.clearActiveEngagementMode()
                dataStoreRepository.recordOperation("SUCCESS", "Apps remain in stopped state. Status bar was untouched.")
                StatusBarWidgetProvider.updateAllWidgets(context)
                return true
            }
            EngageMode.BOTH -> TargetMode.BothDisengage
        }

        dataStoreRepository.clearActiveEngagementMode()
        service.startAutomation(targetMode, returnToHome = fromWidget)
        StatusBarWidgetProvider.updateAllWidgets(context)
        return true
    }
}
