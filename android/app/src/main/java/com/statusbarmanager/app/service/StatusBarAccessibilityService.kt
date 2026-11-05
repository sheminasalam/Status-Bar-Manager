package com.statusbarmanager.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.statusbarmanager.app.automation.*
import com.statusbarmanager.app.data.DataStoreRepository
import com.statusbarmanager.app.data.SettingsBackupManager
import com.statusbarmanager.app.data.StatusBarBackupModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StatusBarAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "StatusBarAccService"
        const val ACTION_START_AUTOMATION = "com.statusbarmanager.app.ACTION_START"
        const val EXTRA_TARGET_MODE = "extra_target_mode"

        var instance: StatusBarAccessibilityService? = null
            private set

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _progressState = MutableStateFlow(AutomationProgress())
        val progressState: StateFlow<AutomationProgress> = _progressState.asStateFlow()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val compatibilityManager = DeviceCompatibilityManager()
    private lateinit var dataStoreRepository: DataStoreRepository
    private lateinit var backupManager: SettingsBackupManager

    private var currentAutomationJob: Job? = null
    private var isAutomationRunning = false
    private var currentMode: TargetMode = TargetMode.HideAll

    override fun onCreate() {
        super.onCreate()
        dataStoreRepository = DataStoreRepository(applicationContext)
        backupManager = SettingsBackupManager(dataStoreRepository)
        Log.i(TAG, "StatusBarAccessibilityService created.")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true
        Log.i(TAG, "StatusBarAccessibilityService connected.")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceConnected.value = false
        serviceScope.cancel()
        Log.i(TAG, "StatusBarAccessibilityService destroyed.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility events are monitored during active automation cycles
        if (event == null || !isAutomationRunning) return

        val eventType = event.eventType
        val packageName = event.packageName?.toString() ?: ""

        if (packageName.contains("settings", ignoreCase = true)) {
            Log.d(TAG, "Settings event: type=$eventType from $packageName")
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted.")
        cancelAutomation("Accessibility service was interrupted by system.")
    }

    /**
     * Entry point to launch the Status Bar automation workflow
     */
    fun startAutomation(mode: TargetMode) {
        if (isAutomationRunning) {
            Log.w(TAG, "Automation is already running.")
            return
        }

        currentMode = mode
        isAutomationRunning = true
        currentAutomationJob?.cancel()

        currentAutomationJob = serviceScope.launch {
            try {
                executeWorkflow(mode)
            } catch (e: CancellationException) {
                Log.i(TAG, "Automation cancelled.")
            } catch (e: Exception) {
                Log.e(TAG, "Automation error: ${e.message}", e)
                handleAutomationFailure("Error: ${e.localizedMessage ?: "Unknown failure"}")
            } finally {
                isAutomationRunning = false
            }
        }
    }

    private suspend fun executeWorkflow(mode: TargetMode) = withContext(Dispatchers.Default) {
        val modeName = if (mode is TargetMode.HideAll) "Hiding" else "Restoring"
        updateProgress(AutomationStep.STARTING, "$modeName status bar information...", 0, 7)

        // Step 1: Launch Native Settings
        updateProgress(AutomationStep.OPENING_SETTINGS, "Opening phone Settings...", 1, 7)
        launchSettings()
        delay(1200)

        // Step 2: Navigate to "Notifications & status bar"
        updateProgress(AutomationStep.NAVIGATING_TO_NOTIFICATIONS_STATUS_BAR, "Locating Notifications & status bar...", 2, 7)
        var reachedSection = navigateToNotificationsAndStatusBar()
        if (!reachedSection) {
            // Attempt direct search or scroll
            scrollDown()
            delay(600)
            reachedSection = navigateToNotificationsAndStatusBar()
        }
        delay(1000)

        // Step 3: Navigate into "Status bar"
        updateProgress(AutomationStep.NAVIGATING_TO_STATUS_BAR, "Opening Status bar settings...", 3, 7)
        val reachedStatusBar = navigateToStatusBarSection()
        if (!reachedStatusBar) {
            throw IllegalStateException("Could not find 'Status bar' section in Settings. Please open it manually.")
        }
        delay(1000)

        // Step 4: Handle "Time" setting
        updateProgress(AutomationStep.READING_CURRENT_TIME, "Configuring Time display...", 4, 7)
        handleTimeSetting(mode)
        delay(800)

        // Step 5: Handle "Real-time network speed"
        updateProgress(AutomationStep.TOGGLING_NETWORK_SPEED, "Configuring Real-time network speed...", 5, 7)
        handleNetworkSpeedSetting(mode)
        delay(800)

        // Step 6: Handle Status Icons Submenu
        updateProgress(AutomationStep.NAVIGATING_TO_STATUS_ICONS, "Configuring Status Icons...", 6, 7)
        handleStatusIcons(mode)
        delay(1000)

        // Step 7: Verification & return
        updateProgress(AutomationStep.VERIFYING_CHANGES, "Verifying applied changes...", 7, 7)
        delay(600)

        if (mode is TargetMode.HideAll) {
            dataStoreRepository.setHideActive(true)
            dataStoreRepository.recordOperation("SUCCESS", "Status bar information hidden successfully")
            updateProgress(AutomationStep.COMPLETED, "Status bar information hidden", 7, 7)
        } else {
            backupManager.markRestoredSuccessfully()
            dataStoreRepository.recordOperation("SUCCESS", "Original configuration restored successfully")
            updateProgress(AutomationStep.COMPLETED, "Original configuration restored", 7, 7)
        }

        // Automatically navigate back to our app
        delay(1200)
        returnToApp()
    }

    private fun launchSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private suspend fun navigateToNotificationsAndStatusBar(): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeMatching(root, compatibilityManager.notificationsAndStatusBarLabels)
        if (node != null) {
            val clicked = clickNodeOrParent(node)
            node.recycle()
            return clicked
        }
        return false
    }

    private suspend fun navigateToStatusBarSection(): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeMatching(root, compatibilityManager.statusBarSectionLabels)
        if (node != null) {
            val clicked = clickNodeOrParent(node)
            node.recycle()
            return clicked
        }
        return false
    }

    private suspend fun handleTimeSetting(mode: TargetMode) {
        val root = rootInActiveWindow ?: return
        val timeNode = findNodeMatching(root, compatibilityManager.timeMenuLabels) ?: return
        
        // Read current state for backup if hiding
        val summaryText = extractSummaryText(timeNode)
        if (mode is TargetMode.HideAll && summaryText != null) {
            val existing = backupManager.getExistingBackup()
            if (existing == null) {
                // Will save along with icons
            }
        }

        // Click to open Time dialog
        clickNodeOrParent(timeNode)
        timeNode.recycle()
        delay(700)

        // In the dialog, select target option
        val dialogRoot = rootInActiveWindow ?: return
        val targetLabels = if (mode is TargetMode.HideAll) {
            compatibilityManager.doNotShowLabels
        } else {
            val backup = backupManager.getExistingBackup()
            listOf(backup?.originalTimeSetting ?: "Show hours and minutes")
        }

        val targetOptionNode = findNodeMatching(dialogRoot, targetLabels)
        if (targetOptionNode != null) {
            clickNodeOrParent(targetOptionNode)
            targetOptionNode.recycle()
            Log.i(TAG, "Selected Time option: ${targetLabels.first()}")
        } else {
            Log.w(TAG, "Could not find Time dialog option for: $targetLabels")
        }
    }

    private suspend fun handleNetworkSpeedSetting(mode: TargetMode) {
        val root = rootInActiveWindow ?: return
        val speedLabelNode = findNodeMatching(root, compatibilityManager.networkSpeedLabels) ?: return

        val toggleNode = findAssociatedToggle(speedLabelNode)
        if (toggleNode != null) {
            val isChecked = toggleNode.isChecked
            val targetState = if (mode is TargetMode.HideAll) false else {
                backupManager.getExistingBackup()?.originalRealTimeNetworkSpeed ?: true
            }

            if (isChecked != targetState) {
                clickNodeOrParent(toggleNode)
                Log.i(TAG, "Toggled network speed from $isChecked to $targetState")
            }
            toggleNode.recycle()
        }
        speedLabelNode.recycle()
    }

    private suspend fun handleStatusIcons(mode: TargetMode) {
        val root = rootInActiveWindow ?: return
        val iconsMenuNode = findNodeMatching(root, compatibilityManager.statusIconsSubmenuLabels)
        if (iconsMenuNode != null) {
            clickNodeOrParent(iconsMenuNode)
            iconsMenuNode.recycle()
            delay(800)
            
            // We are inside the "Status icons" submenu
            val iconsRoot = rootInActiveWindow ?: return
            val discoveredIcons = scanAvailableIcons(iconsRoot)
            
            if (mode is TargetMode.HideAll) {
                // Critical: Save baseline before changing if not already active!
                val existing = backupManager.getExistingBackup()
                if (existing == null) {
                    backupManager.saveBackupIfNotActive(
                        timeSetting = "Show hours and minutes",
                        networkSpeed = true,
                        icons = discoveredIcons
                    )
                }

                // Turn OFF every icon
                toggleAllIcons(iconsRoot, targetState = false, backup = null)
            } else {
                // Restore original state
                val backup = backupManager.getExistingBackup()
                val originalMap = backup?.originalStatusIcons ?: emptyMap()
                restoreIconsToOriginalState(iconsRoot, originalMap)
            }

            // Navigate back up to Status Bar menu
            performGlobalAction(GLOBAL_ACTION_BACK)
            delay(500)
        } else {
            Log.w(TAG, "No separate Status icons submenu found; scanning inline on status bar page.")
            // Some OEM ROMs place icon toggles directly inside the Status bar page
            val discoveredIcons = scanAvailableIcons(root)
            if (mode is TargetMode.HideAll) {
                backupManager.saveBackupIfNotActive("Show hours and minutes", true, discoveredIcons)
                toggleAllIcons(root, targetState = false, backup = null)
            } else {
                val backup = backupManager.getExistingBackup()
                restoreIconsToOriginalState(root, backup?.originalStatusIcons ?: emptyMap())
            }
        }
    }

    private fun scanAvailableIcons(root: AccessibilityNodeInfo): Map<String, Boolean> {
        val map = mutableMapOf<String, Boolean>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString() ?: node.contentDescription?.toString()
            if (text != null && compatibilityManager.isTargetStatusIconLabel(text)) {
                val toggle = findAssociatedToggle(node)
                if (toggle != null) {
                    map[text.trim()] = toggle.isChecked
                    toggle.recycle()
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return map
    }

    private suspend fun toggleAllIcons(root: AccessibilityNodeInfo, targetState: Boolean, backup: Map<String, Boolean>?) {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString() ?: node.contentDescription?.toString()
            
            // Strict safeguard: Never toggle battery percentage option!
            if (text != null && compatibilityManager.isTargetStatusIconLabel(text)) {
                val toggle = findAssociatedToggle(node)
                if (toggle != null) {
                    if (toggle.isChecked != targetState) {
                        clickNodeOrParent(toggle)
                        delay(250) // Allow UI animation to settle
                    }
                    toggle.recycle()
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
    }

    private suspend fun restoreIconsToOriginalState(root: AccessibilityNodeInfo, originalMap: Map<String, Boolean>) {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString() ?: node.contentDescription?.toString()

            if (text != null && compatibilityManager.isTargetStatusIconLabel(text)) {
                val iconKey = text.trim()
                // Use saved original state, or default to false if not found
                val targetState = originalMap[iconKey] ?: false
                val toggle = findAssociatedToggle(node)
                if (toggle != null) {
                    if (toggle.isChecked != targetState) {
                        clickNodeOrParent(toggle)
                        delay(250)
                    }
                    toggle.recycle()
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
    }

    private fun findAssociatedToggle(labelNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Look within siblings in the same row/container
        val parent = labelNode.parent ?: return null
        val count = parent.childCount
        for (i in 0 until count) {
            val child = parent.getChild(i) ?: continue
            if (child.isCheckable) {
                return child
            }
            // Check deeper in container
            for (j in 0 until child.childCount) {
                val grandChild = child.getChild(j) ?: continue
                if (grandChild.isCheckable) {
                    child.recycle()
                    return grandChild
                }
                grandChild.recycle()
            }
            child.recycle()
        }
        parent.recycle()
        return null
    }

    private fun extractSummaryText(node: AccessibilityNodeInfo): String? {
        val parent = node.parent ?: return null
        for (i in 0 until parent.childCount) {
            val child = parent.getChild(i) ?: continue
            val childText = child.text?.toString()
            if (childText != null && childText != node.text?.toString()) {
                parent.recycle()
                return childText
            }
            child.recycle()
        }
        parent.recycle()
        return null
    }

    private fun findNodeMatching(root: AccessibilityNodeInfo, candidates: List<String>): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val text = current.text
            val desc = current.contentDescription

            if (compatibilityManager.matchesAny(text, candidates) || compatibilityManager.matchesAny(desc, candidates)) {
                return current
            }

            for (i in 0 until current.childCount) {
                val child = current.getChild(i)
                if (child != null) queue.add(child)
            }
        }
        return null
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                val success = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (success) return true
            }
            current = current.parent
        }

        // Fallback: Dispatch gesture tap at node bounds
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty) {
            return dispatchTapGesture(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }
        return false
    }

    private fun dispatchTapGesture(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    private fun scrollDown(): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrolled = root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        root.recycle()
        return scrolled
    }

    private fun returnToApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        if (intent != null) {
            startActivity(intent)
        } else {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun updateProgress(step: AutomationStep, message: String, index: Int, total: Int) {
        _progressState.value = AutomationProgress(
            currentStep = step,
            message = message,
            stepIndex = index,
            totalSteps = total
        )
    }

    private fun handleAutomationFailure(reason: String) {
        updateProgress(AutomationStep.FAILED, reason, 0, 7)
        serviceScope.launch {
            dataStoreRepository.recordOperation("FAILED", reason)
        }
    }

    private fun cancelAutomation(reason: String) {
        currentAutomationJob?.cancel()
        handleAutomationFailure(reason)
    }
}
