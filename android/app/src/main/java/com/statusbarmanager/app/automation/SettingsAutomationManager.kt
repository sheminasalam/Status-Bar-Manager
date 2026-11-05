package com.statusbarmanager.app.automation

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.statusbarmanager.app.service.StatusBarAccessibilityService
import kotlinx.coroutines.flow.StateFlow

class SettingsAutomationManager(private val context: Context) {

    val isServiceConnected: StateFlow<Boolean> = StatusBarAccessibilityService.isServiceConnected
    val progressState: StateFlow<AutomationProgress> = StatusBarAccessibilityService.progressState

    /**
     * Checks whether the user has granted the accessibility permission
     */
    fun isAccessibilityPermissionGranted(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedPackage = context.packageName
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == expectedPackage }
    }

    /**
     * Opens system Accessibility Settings screen to let user enable the service
     */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Opens direct Settings screen for manual navigation
     */
    fun openSystemSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Starts the hide or restore automation
     */
    fun triggerAutomation(mode: TargetMode): Boolean {
        val service = StatusBarAccessibilityService.instance
        if (service == null || !isAccessibilityPermissionGranted()) {
            return false
        }
        service.startAutomation(mode)
        return true
    }
}
