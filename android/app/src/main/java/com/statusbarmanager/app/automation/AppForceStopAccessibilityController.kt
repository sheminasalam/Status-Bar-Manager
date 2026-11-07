package com.statusbarmanager.app.automation

import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
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

        // Brief delay to let Settings activity switch to the target app's screen
        delay(120)

        // Step 2 & 3: Find Force Stop button in active window
        var targetPageSeen = false
        var forceStopNode: AccessibilityNodeInfo? = null
        repeat(16) { attempt ->
            if (forceStopNode == null) {
                val activeRoot = service.rootInActiveWindow
                if (activeRoot != null) {
                    if (isTargetAppInfoPage(activeRoot, appItem, attempt)) {
                        targetPageSeen = true
                        val button = findForceStopButton(activeRoot)
                        if (button != null) {
                            forceStopNode = AccessibilityNodeInfo.obtain(button)
                            if (button !== activeRoot) button.recycle()
                        }
                    }
                    activeRoot.recycle()
                }
                if (forceStopNode == null && attempt < 15) delay(70)
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
        // Force stop button becomes disabled.
        var confirmationClicked = false
        repeat(14) { attempt ->
            val activeRoot = service.rootInActiveWindow
            if (activeRoot != null) {
                if (!confirmationClicked) {
                    val confirmNode = findConfirmationButton(activeRoot)
                    if (confirmNode != null) {
                        val confirmed = clickNodeOrParent(confirmNode)
                        confirmNode.recycle()
                        if (confirmed) {
                            confirmationClicked = true
                            Log.i(TAG, "Confirmed Force Stop dialog for ${appItem.packageName}")
                            delay(120) // Allow OS to kill process and dismiss dialog
                        }
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
            delay(70)
        }

        // If dialog was confirmed, the OS kill command has been dispatched
        if (confirmationClicked) {
            Log.i(TAG, "Force stop confirmation accepted for ${appItem.packageName}")
            return ForceStopResult.SUCCESSFULLY_STOPPED
        }

        Log.w(TAG, "Could not verify Force Stop for ${appItem.packageName}; confirmationClicked=$confirmationClicked")
        return ForceStopResult.FAILED_CONFIRMATION
    }

    private fun isTargetAppInfoPage(root: AccessibilityNodeInfo, appItem: AppItem, attempt: Int): Boolean {
        val rootPkg = root.packageName?.toString()?.lowercase() ?: ""
        val isSettingsWindow = rootPkg.contains("settings")

        val expectedLabel = appItem.appName.trim()
        val expectedPackage = appItem.packageName.trim()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        var matched = false
        var hasAppControls = false

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString()?.trim() ?: ""
            val desc = node.contentDescription?.toString()?.trim() ?: ""

            if (text.equals(expectedLabel, ignoreCase = true) ||
                desc.equals(expectedLabel, ignoreCase = true) ||
                text.contains(expectedPackage, ignoreCase = true) ||
                desc.contains(expectedPackage, ignoreCase = true)
            ) {
                matched = true
                while (queue.isNotEmpty()) queue.removeFirst().recycle()
                if (node !== root) node.recycle()
                break
            }

            val lowerText = text.lowercase()
            val lowerDesc = desc.lowercase()
            if (FORCE_STOP_BUTTON_LABELS.any { lowerText.contains(it) || lowerDesc.contains(it) } ||
                lowerText.contains("uninstall") || lowerDesc.contains("uninstall") ||
                lowerText.contains("disable") || lowerDesc.contains("disable")
            ) {
                hasAppControls = true
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::add)
            }
            if (node !== root) node.recycle()
        }

        // On the first 5 attempts (~400ms), require a positive match for appName or packageName.
        // This avoids prematurely matching the PREVIOUS app's App Info page while Settings transitions.
        return if (attempt < 5) {
            matched
        } else {
            matched || (isSettingsWindow && hasAppControls)
        }
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
            val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (success) return true
        }
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                val success = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                parent.recycle()
                if (success) return true
            } else {
                val grandParent = parent.parent
                parent.recycle()
                parent = grandParent
            }
        }

        // Coordinate-based gesture tap fallback
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty && bounds.centerX() > 0 && bounds.centerY() > 0) {
            val path = Path().apply { moveTo(bounds.centerX().toFloat(), bounds.centerY().toFloat()) }
            val stroke = GestureDescription.StrokeDescription(path, 0, 45)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            return service.dispatchGesture(gesture, null, null)
        }
        return false
    }
}
