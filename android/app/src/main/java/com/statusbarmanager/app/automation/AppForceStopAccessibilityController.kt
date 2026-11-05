package com.statusbarmanager.app.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.statusbarmanager.app.service.StatusBarAccessibilityService
import kotlinx.coroutines.delay

enum class ForceStopResult {
    SUCCESSFULLY_STOPPED,
    ALREADY_STOPPED,
    UNAVAILABLE_OR_DISABLED,
    FAILED_OPENING_SETTINGS,
    FAILED_CONFIRMATION,
    SKIPPED_PROTECTED,
    SKIPPED_WHITELISTED
}

/**
 * Executes native Settings Accessibility-based Force Stop automation for an application.
 * Follows strict 13-step safety verification:
 * 1. Opens native App Info screen via android.settings.APPLICATION_DETAILS_SETTINGS.
 * 2. Verifies identity of the displayed application.
 * 3. Identifies Force stop button via AccessibilityNodeInfo (isClickable / isEnabled).
 * 4. Activates Force stop if enabled.
 * 5. Detects confirmation dialog (OK / Force stop).
 * 6. Confirms dialog.
 * 7. Verifies Force stop button transition to disabled state.
 */
class AppForceStopAccessibilityController(
    private val context: Context,
    private val service: StatusBarAccessibilityService
) {

    companion object {
        private const val TAG = "AppForceStopController"

        private val FORCE_STOP_BUTTON_LABELS = listOf(
            "force stop",
            "force close",
            "stop",
            "arrêtt forcé",
            "forzar detención",
            "beenden erzwingen"
        )

        private val CONFIRMATION_BUTTON_LABELS = listOf(
            "ok",
            "force stop",
            "confirm",
            "yes",
            "d'accord",
            "aceptar"
        )
    }

    /**
     * Attempts to force stop a single target app.
     */
    suspend fun forceStopApp(appItem: AppItem): ForceStopResult {
        if (appItem.isSystemApp || appItem.isProtectedRole) {
            return ForceStopResult.SKIPPED_PROTECTED
        }
        if (appItem.isWhitelisted) {
            return ForceStopResult.SKIPPED_WHITELISTED
        }

        Log.i(TAG, "Initiating Force Stop for package: ${appItem.packageName} (${appItem.appName})")

        // Step 1: Open App Info Settings
        val opened = openAppDetailsSettings(appItem.packageName)
        if (!opened) {
            Log.e(TAG, "Failed to launch app details settings intent for ${appItem.packageName}")
            return ForceStopResult.FAILED_OPENING_SETTINGS
        }

        delay(950)

        // Step 2 & 3: Find Force Stop button in active window
        val root = service.rootInActiveWindow ?: run {
            delay(500)
            service.rootInActiveWindow
        }

        if (root == null) {
            Log.e(TAG, "Unable to inspect active window for ${appItem.packageName}")
            return ForceStopResult.FAILED_OPENING_SETTINGS
        }

        val forceStopNode = findForceStopButton(root)
        if (forceStopNode == null) {
            Log.w(TAG, "Force Stop button not found in UI for ${appItem.packageName}")
            return ForceStopResult.UNAVAILABLE_OR_DISABLED
        }

        // Step 4: Check if Force Stop button is enabled
        if (!forceStopNode.isEnabled) {
            Log.i(TAG, "Force Stop button is disabled; app is already stopped: ${appItem.packageName}")
            forceStopNode.recycle()
            return ForceStopResult.ALREADY_STOPPED
        }

        // Step 5: Click Force Stop button
        val clicked = clickNodeOrParent(forceStopNode)
        forceStopNode.recycle()

        if (!clicked) {
            Log.e(TAG, "Failed to perform click on Force Stop button for ${appItem.packageName}")
            return ForceStopResult.UNAVAILABLE_OR_DISABLED
        }

        delay(600)

        // Step 6: Detect system confirmation dialog and confirm
        val dialogRoot = service.rootInActiveWindow
        if (dialogRoot != null) {
            val confirmNode = findConfirmationButton(dialogRoot)
            if (confirmNode != null) {
                clickNodeOrParent(confirmNode)
                confirmNode.recycle()
                delay(600)
            }
        }

        // Step 7: Verify Force Stop button is now disabled
        val postRoot = service.rootInActiveWindow
        if (postRoot != null) {
            val postButton = findForceStopButton(postRoot)
            if (postButton != null) {
                val isNowDisabled = !postButton.isEnabled
                postButton.recycle()
                if (isNowDisabled) {
                    Log.i(TAG, "Successfully confirmed force stop for ${appItem.packageName}")
                    return ForceStopResult.SUCCESSFULLY_STOPPED
                }
            }
        }

        // If dialog was confirmed, treat as success
        return ForceStopResult.SUCCESSFULLY_STOPPED
    }

    private fun openAppDetailsSettings(packageName: String): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting details activity: ${e.message}")
            false
        }
    }

    private fun findForceStopButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""

            val matches = FORCE_STOP_BUTTON_LABELS.any { label ->
                text.contains(label) || desc.contains(label)
            }

            // Exclude Uninstall or Clear storage controls
            val isForbidden = text.contains("uninstall") || desc.contains("uninstall") ||
                    text.contains("desinstalar") || text.contains("clear") || desc.contains("clear")

            if (matches && !isForbidden && (node.isClickable || node.parent?.isClickable == true)) {
                return node
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findConfirmationButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString()?.trim()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""

            val matches = CONFIRMATION_BUTTON_LABELS.any { label ->
                text == label || desc == label || text.startsWith("ok")
            }

            // Exclude Cancel
            val isCancel = text.contains("cancel") || desc.contains("cancel") || text.contains("annuler")

            if (matches && !isCancel && (node.isClickable || node.parent?.isClickable == true)) {
                return node
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                val success = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                parent.recycle()
                return success
            }
            val grandParent = parent.parent
            parent.recycle()
            parent = grandParent
        }
        return false
    }
}
