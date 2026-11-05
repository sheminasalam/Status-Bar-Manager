package com.statusbarmanager.app.automation

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.statusbarmanager.app.service.StatusBarAccessibilityService
import kotlinx.coroutines.flow.StateFlow

class SettingsAutomationManager(private val context: Context) {

    val isServiceConnected: StateFlow<Boolean> = StatusBarAccessibilityService.isServiceConnected
    val progressState: StateFlow<AutomationProgress> = StatusBarAccessibilityService.progressState

    /**
     * Checks whether the user has enabled the Accessibility Service
     */
    fun isAccessibilityPermissionGranted(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedPackage = context.packageName
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == expectedPackage }
    }

    /**
     * The progress cover uses TYPE_ACCESSIBILITY_OVERLAY and is owned by the enabled
     * AccessibilityService. Android does not require the separate SYSTEM_ALERT_WINDOW grant.
     */
    fun canDrawOverlays(): Boolean = true

    /**
     * Opens system Accessibility Settings screen
     */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Opens Display over other apps settings for floating overlay progress
     */
    fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val genericIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(genericIntent)
        }
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
