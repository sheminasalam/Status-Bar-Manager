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

        // Settings can take more than a fixed 600 ms to open. Wait for the
        // requested app's own App Info title before reading or clicking controls.
        var targetPageSeen = false
        var forceStopNode: AccessibilityNodeInfo? = null
        repeat(20) { attempt ->
            if (forceStopNode == null) {
                val activeRoot = service.rootInActiveWindow
                if (activeRoot != null) {
                    if (isTargetAppInfoPage(activeRoot, appItem)) {
                        targetPageSeen = true
                        forceStopNode = findForceStopButton(activeRoot)
                    }
                    activeRoot.recycle()
                }
                if (forceStopNode == null && attempt < 19) delay(250)
            }
        }

        if (!targetPageSeen) {
            Log.e(TAG, "App Info screen did not confirm target app ${appItem.packageName}")
            return ForceStopResult.FAILED_OPENING_SETTINGS
        }
        if (forceStopNode == null) {
            Log.w(TAG, "Force Stop button not found in target app UI for ${appItem.packageName}")
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

        // Step 6 & 7: Handle the native confirmation dialog and verify the
        // Force stop button becomes disabled. Never report success merely because
        // the click action was dispatched.
        var confirmationClicked = false
        repeat(10) {
            val activeRoot = service.rootInActiveWindow
            if (activeRoot != null) {
                if (!confirmationClicked) {
                    val confirmNode = findConfirmationButton(activeRoot)
                    if (confirmNode != null) {
                        val confirmed = try {
                            clickNodeOrParent(confirmNode)
                        } finally {
                            confirmNode.recycle()
                        }
                        if (!confirmed) {
                            activeRoot.recycle()
                            Log.e(TAG, "Could not activate Force Stop confirmation for ${appItem.packageName}")
                            return ForceStopResult.FAILED_CONFIRMATION
                        }
                        confirmationClicked = true
                        Log.i(TAG, "Confirmed Force Stop dialog for ${appItem.packageName}")
                    }
                }

                val postButton = findForceStopButton(activeRoot)
                if (postButton != null) {
                    val isNowDisabled = !postButton.isEnabled
                    postButton.recycle()
                    activeRoot.recycle()
                    if (isNowDisabled) {
                        Log.i(TAG, "Verified Force Stop for ${appItem.packageName}")
                        return ForceStopResult.SUCCESSFULLY_STOPPED
                    }
                } else {
                    activeRoot.recycle()
                }
            }
            delay(250)
        }

        Log.e(
            TAG,
            "Could not verify that Force Stop completed for ${appItem.packageName}; " +
                "confirmationClicked=$confirmationClicked"
        )
        return ForceStopResult.FAILED_CONFIRMATION
    }

    private fun isTargetAppInfoPage(root: AccessibilityNodeInfo, appItem: AppItem): Boolean {
        val expectedLabel = appItem.appName.trim()
        val expectedPackage = appItem.packageName.trim()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        var matched = false
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val values = listOfNotNull(
                node.text?.toString()?.trim(),
                node.contentDescription?.toString()?.trim()
            )
            if (values.any { value ->
                    value.equals(expectedLabel, ignoreCase = true) ||
                        value.contains(expectedPackage, ignoreCase = true)
                }) {
                matched = true
                while (queue.isNotEmpty()) queue.removeFirst().recycle()
                if (node !== root) node.recycle()
                break
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::add)
            }
            if (node !== root) node.recycle()
        }
        return matched
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

            // Exclude Uninstall or Clear storage controls.
            val isForbidden = text.contains("uninstall") || desc.contains("uninstall") ||
                text.contains("desinstalar") || text.contains("clear") || desc.contains("clear")

            val parent = if (!node.isClickable) node.parent else null
            val hasClickableParent = parent?.isClickable == true
            parent?.recycle()

            if (matches && !isForbidden && (node.isClickable || hasClickableParent)) {
                while (queue.isNotEmpty()) queue.removeFirst().recycle()
                return if (node === root) AccessibilityNodeInfo.obtain(node) else node
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::add)
            }
            if (node !== root) node.recycle()
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
            val isCancel = text.contains("cancel") || desc.contains("cancel") ||
                text.contains("annuler")

            val parent = if (!node.isClickable) node.parent else null
            val hasClickableParent = parent?.isClickable == true
            parent?.recycle()

            if (matches && !isCancel && (node.isClickable || hasClickableParent)) {
                while (queue.isNotEmpty()) queue.removeFirst().recycle()
                return if (node === root) AccessibilityNodeInfo.obtain(node) else node
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::add)
            }
            if (node !== root) node.recycle()
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
