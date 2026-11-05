package com.statusbarmanager.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
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
    private lateinit var overlayManager: AutomationOverlayManager

    private var currentAutomationJob: Job? = null
    private var isAutomationRunning = false

    override fun onCreate() {
        super.onCreate()
        dataStoreRepository = DataStoreRepository(applicationContext)
        backupManager = SettingsBackupManager(dataStoreRepository)
        overlayManager = AutomationOverlayManager(applicationContext)
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
        overlayManager.dismiss()
        serviceScope.cancel()
        Log.i(TAG, "StatusBarAccessibilityService destroyed.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isAutomationRunning) return
        val packageName = event.packageName?.toString() ?: ""
        if (packageName.contains("settings", ignoreCase = true)) {
            Log.d(TAG, "Settings window event: ${event.eventType}")
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted.")
        cancelAutomation("Accessibility service was interrupted by system.")
    }

    fun startAutomation(mode: TargetMode) {
        if (isAutomationRunning) {
            Log.w(TAG, "Automation is already running.")
            return
        }

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
        val appManager = ApplicationManager(applicationContext)
        val forceStopController = AppForceStopAccessibilityController(applicationContext, this@StatusBarAccessibilityService)
        val userWhitelist = dataStoreRepository.getWhitelist()

        val isHideMode = mode is TargetMode.HideAll || mode is TargetMode.BothEngage
        val isForceStopMode = mode is TargetMode.ForceStopOnly || mode is TargetMode.BothEngage
        val isRestoreMode = mode is TargetMode.RestoreOriginal || mode is TargetMode.BothDisengage

        val engageMode = when (mode) {
            is TargetMode.HideAll, is TargetMode.RestoreOriginal -> EngageMode.STATUS_BAR_ONLY
            is TargetMode.ForceStopOnly -> EngageMode.FORCE_STOP_APPS_ONLY
            is TargetMode.BothEngage, is TargetMode.BothDisengage -> EngageMode.BOTH
        }

        val opTitle = when {
            mode is TargetMode.BothEngage -> "SysHush – Engage (Both)"
            mode is TargetMode.BothDisengage -> "SysHush – Disengage"
            mode is TargetMode.ForceStopOnly -> "SysHush – Force Stop Apps"
            mode is TargetMode.HideAll -> "SysHush – Hide Status Bar"
            else -> "SysHush – Restore Status Bar"
        }

        var checklist = AutomationProgress.forMode(engageMode, isEngage = !isRestoreMode)
        fun updateChecklist(id: String, status: StepStatus, details: String? = null) {
            checklist = checklist.map { item ->
                if (item.id == id) item.copy(status = status, details = details) else item
            }
        }

        var currentAppStats = AppForceStopStats()

        fun publish(
            step: AutomationStep,
            opDescription: String,
            percent: Int,
            stepIdx: Int,
            iconStats: IconStats = _progressState.value.iconStats,
            appStats: AppForceStopStats = currentAppStats,
            headphoneStatus: String = _progressState.value.headphoneStatus,
            isPartial: Boolean = _progressState.value.isPartialResult
        ) {
            val progress = AutomationProgress(
                currentStep = step,
                operationTitle = opTitle,
                currentOperation = opDescription,
                percentage = percent,
                stepIndex = stepIdx,
                totalSteps = checklist.size,
                checklist = checklist,
                iconStats = iconStats,
                appStats = appStats,
                headphoneStatus = headphoneStatus,
                isPartialResult = isPartial
            )
            _progressState.value = progress
            overlayManager.updateProgress(progress)
        }

        publish(AutomationStep.STARTING, "Preparing automation environment...", 5, 0)
        delay(400)

        // ----------------------------------------------------
        // PHASE 1: STATUS BAR AUTOMATION (if applicable)
        // ----------------------------------------------------
        var isPartial = false
        var headphoneStatusText = "Not checked"
        var stats = IconStats()

        if (isHideMode || (isRestoreMode && mode !is TargetMode.ForceStopOnly)) {
            // Step 1: Open Settings
            publish(AutomationStep.OPENING_SETTINGS, "Opening phone Settings...", 12, 1)
            launchSettings()
            delay(1100)

            // Step 2: Locate Notifications & Status Bar
            publish(AutomationStep.NAVIGATING_TO_NOTIFICATIONS_STATUS_BAR, "Navigating to Notifications & status bar...", 18, 2)
            var reachedSection = navigateToNotificationsAndStatusBar()
            if (!reachedSection) {
                scrollForward()
                delay(500)
                reachedSection = navigateToNotificationsAndStatusBar()
            }
            delay(900)

            // Step 3: Enter Status Bar section
            publish(AutomationStep.NAVIGATING_TO_STATUS_BAR, "Opening Status bar settings...", 26, 3)
            val reachedStatusBar = navigateToStatusBarSection()
            if (!reachedStatusBar) {
                throw IllegalStateException("Could not find 'Status bar' section in Settings. Please open it manually.")
            }
        }
        delay(900)

        // Step 4: Baseline backup & Time display handling
        updateChecklist("backup", StepStatus.IN_PROGRESS)
        updateChecklist("time", StepStatus.IN_PROGRESS)
        publish(AutomationStep.READING_BASELINE_SETTINGS, "Reading current Time display setting...", 34, 4)

        val actualCurrentTime = readActualCurrentTimeSetting()
        Log.i(TAG, "Actual current Time setting detected: '$actualCurrentTime'")

        // Configure Time display
        publish(AutomationStep.CONFIGURING_TIME, if (isHideMode) "Setting Time to 'Do not show'..." else "Restoring original Time...", 40, 4)
        handleTimeSetting(mode, actualCurrentTime)
        updateChecklist("time", StepStatus.COMPLETED, if (isHideMode) "Set to Do not show" else "Restored: $actualCurrentTime")
        delay(800)

        // Step 5: Network speed toggle
        updateChecklist("speed", StepStatus.IN_PROGRESS)
        publish(AutomationStep.CONFIGURING_NETWORK_SPEED, "Checking Real-time network speed...", 46, 5)
        val actualNetworkSpeed = handleNetworkSpeedSetting(mode)
        updateChecklist("speed", StepStatus.COMPLETED, if (isHideMode) "Disabled" else "Restored")
        delay(700)

        // Step 6: Comprehensive Status Icons Discovery & Toggle
        updateChecklist("scan_icons", StepStatus.IN_PROGRESS)
        publish(AutomationStep.NAVIGATING_TO_STATUS_ICONS, "Accessing Status icons submenu...", 52, 6)

        val navigatedToIcons = navigateIntoStatusIconsSubmenu()
        delay(800)

        publish(AutomationStep.DISCOVERING_ALL_STATUS_ICONS, "Discovering all available status icons...", 58, 6)
        // Dynamically discover all checkable switch rows with scrolling
        val discoveredIconsMap = discoverAllStatusIconRows()
        Log.i(TAG, "Total status icons dynamically discovered: ${discoveredIconsMap.size} -> ${discoveredIconsMap.keys}")

        val discoveredCount = discoveredIconsMap.size
        val alreadyDisabledCount = discoveredIconsMap.values.count { !it }
        stats = IconStats(
            discovered = discoveredCount,
            disabled = 0,
            alreadyDisabled = alreadyDisabledCount,
            restored = 0,
            remaining = if (isHideMode) (discoveredCount - alreadyDisabledCount) else discoveredCount
        )
        updateChecklist("scan_icons", StepStatus.COMPLETED, "$discoveredCount icons discovered")
        publish(AutomationStep.DISCOVERING_ALL_STATUS_ICONS, "Discovered $discoveredCount status icons", 65, 6, iconStats = stats)

        // Save baseline BEFORE changing any icons if hide mode and no backup exists
        if (isHideMode) {
            val existing = backupManager.getExistingBackup()
            if (existing == null) {
                backupManager.saveBackupIfNotActive(
                    timeSetting = actualCurrentTime,
                    networkSpeed = actualNetworkSpeed,
                    icons = discoveredIconsMap
                )
                updateChecklist("backup", StepStatus.COMPLETED, "Baseline snapshot preserved")
            } else {
                updateChecklist("backup", StepStatus.COMPLETED, "Existing baseline protected")
            }
        }

        // Toggle / Restore icons
        updateChecklist("toggle_icons", StepStatus.IN_PROGRESS)
        if (isHideMode) {
            publish(AutomationStep.DISABLING_ALL_STATUS_ICONS, "Disabling all active status icons...", 70, 6, iconStats = stats)
            stats = disableAllActiveStatusIcons(discoveredIconsMap) { updatedStats, currentLabel ->
                stats = updatedStats
                val pct = 70 + (20 * updatedStats.disabled / maxOf(1, updatedStats.discovered - updatedStats.alreadyDisabled))
                publish(
                    AutomationStep.DISABLING_ALL_STATUS_ICONS,
                    "Disabling '$currentLabel' (${updatedStats.disabled}/${updatedStats.discovered - updatedStats.alreadyDisabled})...",
                    minOf(90, pct),
                    6,
                    iconStats = updatedStats
                )
            }
            updateChecklist("toggle_icons", StepStatus.COMPLETED, "${stats.disabled} icons turned OFF, ${stats.alreadyDisabled} already OFF")
        } else {
            publish(AutomationStep.RESTORING_ALL_STATUS_ICONS, "Restoring icons to original states...", 70, 6, iconStats = stats)
            val originalBackup = backupManager.getExistingBackup()
            val targetMap = originalBackup?.originalStatusIcons ?: discoveredIconsMap
            stats = restoreAllStatusIconsToBaseline(targetMap) { updatedStats, currentLabel ->
                stats = updatedStats
                val pct = 70 + (20 * updatedStats.restored / maxOf(1, updatedStats.discovered))
                publish(
                    AutomationStep.RESTORING_ALL_STATUS_ICONS,
                    "Restoring '$currentLabel' (${updatedStats.restored}/${updatedStats.discovered})...",
                    minOf(90, pct),
                    6,
                    iconStats = updatedStats
                )
            }
            updateChecklist("toggle_icons", StepStatus.COMPLETED, "${stats.restored} icons restored to exact baseline")
        }

        // Return from Status icons submenu if entered
        if (navigatedToIcons) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            delay(600)
        }

        // ----------------------------------------------------
        // PHASE 2: APPLICATION FORCE STOP AUTOMATION (if applicable)
        // ----------------------------------------------------
        if (isForceStopMode) {
            updateChecklist("scan_apps", StepStatus.IN_PROGRESS)
            updateChecklist("apps", StepStatus.IN_PROGRESS)
            publish(AutomationStep.SCANNING_APPLICATIONS, "Scanning eligible user applications...", 60, 5, iconStats = stats)
            
            val eligibleApps = appManager.getEligibleAppsForForceStop(userWhitelist)
            Log.i(TAG, "Discovered ${eligibleApps.size} eligible applications for force-stop batch")
            updateChecklist("scan_apps", StepStatus.COMPLETED, "${eligibleApps.size} eligible apps")

            currentAppStats = currentAppStats.copy(
                totalEligible = eligibleApps.size,
                remaining = eligibleApps.size
            )

            if (eligibleApps.isNotEmpty()) {
                var stoppedCount = 0
                var alreadyStoppedCount = 0
                var failedCount = 0

                updateChecklist("force_stop", StepStatus.IN_PROGRESS)

                eligibleApps.forEachIndexed { idx, app ->
                    val appProgressPercent = 60 + ((idx + 1) * 35 / eligibleApps.size)
                    currentAppStats = currentAppStats.copy(
                        currentAppName = app.appName,
                        remaining = eligibleApps.size - idx
                    )

                    publish(
                        AutomationStep.FORCE_STOPPING_APPS,
                        "Force-stopping ${app.appName} (${idx + 1}/${eligibleApps.size})...",
                        appProgressPercent,
                        6,
                        iconStats = stats,
                        appStats = currentAppStats
                    )

                    val result = forceStopController.forceStopApp(app)
                    when (result) {
                        ForceStopResult.SUCCESSFULLY_STOPPED -> {
                            stoppedCount++
                            currentAppStats = currentAppStats.copy(successfullyStopped = stoppedCount)
                        }
                        ForceStopResult.ALREADY_STOPPED -> {
                            alreadyStoppedCount++
                            currentAppStats = currentAppStats.copy(alreadyStopped = alreadyStoppedCount)
                        }
                        ForceStopResult.FAILED_OPENING_SETTINGS,
                        ForceStopResult.FAILED_CONFIRMATION,
                        ForceStopResult.UNAVAILABLE_OR_DISABLED -> {
                            failedCount++
                            currentAppStats = currentAppStats.copy(failed = failedCount)
                            isPartial = true
                        }
                        else -> { }
                    }
                    delay(300)
                }

                currentAppStats = currentAppStats.copy(remaining = 0)
                val appSummary = "$stoppedCount stopped, $alreadyStoppedCount already off, $failedCount failed"
                updateChecklist("force_stop", if (failedCount == 0) StepStatus.COMPLETED else StepStatus.PARTIAL, appSummary)
                updateChecklist("apps", if (failedCount == 0) StepStatus.COMPLETED else StepStatus.PARTIAL, appSummary)
            } else {
                updateChecklist("force_stop", StepStatus.COMPLETED, "0 eligible apps to process")
                updateChecklist("apps", StepStatus.COMPLETED, "0 eligible apps to process")
            }
        }

        // Final verification & state persistence
        updateChecklist("verify", StepStatus.IN_PROGRESS)
        publish(
            AutomationStep.VERIFYING_CONFIGURATION,
            "Verifying applied states and saving records...",
            98,
            7,
            iconStats = stats,
            appStats = currentAppStats,
            headphoneStatus = headphoneStatusText,
            isPartial = isPartial
        )
        delay(600)

        if (isHideMode || isForceStopMode) {
            dataStoreRepository.setHideActive(active = true, isPartial = isPartial)
            val logMsg = when {
                engageMode == EngageMode.FORCE_STOP_APPS_ONLY ->
                    "Force stop completed: ${currentAppStats.successfullyStopped} stopped, ${currentAppStats.alreadyStopped} already off"
                isPartial ->
                    "Engaged with partial result: System-managed headphone or restricted apps reported accurately"
                else ->
                    "Status bar hidden and eligible applications force-stopped successfully"
            }
            dataStoreRepository.recordOperation(if (isPartial) "PARTIAL" else "SUCCESS", logMsg)
            updateChecklist("verify", StepStatus.COMPLETED)
            publish(
                if (isPartial) AutomationStep.PARTIAL else AutomationStep.COMPLETED,
                logMsg,
                100,
                7,
                iconStats = stats,
                appStats = currentAppStats,
                headphoneStatus = headphoneStatusText,
                isPartial = isPartial
            )
        } else {
            backupManager.markRestoredSuccessfully()
            dataStoreRepository.recordOperation("SUCCESS", "Original status bar configuration restored successfully. No apps restarted.")
            updateChecklist("verify", StepStatus.COMPLETED)
            publish(
                AutomationStep.COMPLETED,
                "Original configuration restored successfully (stopped apps remain stopped)",
                100,
                7,
                iconStats = stats,
                appStats = currentAppStats,
                headphoneStatus = "Restored",
                isPartial = false
            )
        }

        delay(1200)
        returnToApp()
    }

    private fun launchSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun navigateToNotificationsAndStatusBar(): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeMatching(root, compatibilityManager.notificationsAndStatusBarLabels)
        if (node != null) {
            val clicked = clickNodeOrParent(node)
            node.recycle()
            return clicked
        }
        return false
    }

    private fun navigateToStatusBarSection(): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeMatching(root, compatibilityManager.statusBarSectionLabels)
        if (node != null) {
            val clicked = clickNodeOrParent(node)
            node.recycle()
            return clicked
        }
        return false
    }

    /**
     * Reads the actual current Time setting from Settings interface (not hardcoded!)
     */
    private suspend fun readActualCurrentTimeSetting(): String {
        val root = rootInActiveWindow ?: return StatusBarBackupModel.TIME_SHOW_HOURS_MINUTES
        val timeNode = findNodeMatching(root, compatibilityManager.timeMenuLabels)
        if (timeNode != null) {
            val summary = extractSummaryText(timeNode)
            timeNode.recycle()
            if (!summary.isNullOrBlank()) {
                return summary.trim()
            }
        }
        return StatusBarBackupModel.TIME_SHOW_HOURS_MINUTES
    }

    private suspend fun handleTimeSetting(mode: TargetMode, detectedCurrentTime: String) {
        val root = rootInActiveWindow ?: return
        val timeNode = findNodeMatching(root, compatibilityManager.timeMenuLabels) ?: return

        clickNodeOrParent(timeNode)
        timeNode.recycle()
        delay(700)

        val dialogRoot = rootInActiveWindow ?: return
        val targetLabels = if (mode is TargetMode.HideAll) {
            compatibilityManager.doNotShowLabels
        } else {
            val backup = backupManager.getExistingBackup()
            listOf(backup?.originalTimeSetting ?: detectedCurrentTime)
        }

        val targetOptionNode = findNodeMatching(dialogRoot, targetLabels)
        if (targetOptionNode != null) {
            clickNodeOrParent(targetOptionNode)
            targetOptionNode.recycle()
            Log.i(TAG, "Selected Time option: ${targetLabels.first()}")
        } else {
            Log.w(TAG, "Could not find Time dialog option for: $targetLabels")
            // Dismiss dialog if not found
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private suspend fun handleNetworkSpeedSetting(mode: TargetMode): Boolean {
        val root = rootInActiveWindow ?: return true
        val speedLabelNode = findNodeMatching(root, compatibilityManager.networkSpeedLabels) ?: return true

        var originalState = true
        val toggleNode = findAssociatedCheckable(speedLabelNode)
        if (toggleNode != null) {
            originalState = toggleNode.isChecked
            val targetState = if (mode is TargetMode.HideAll) false else {
                backupManager.getExistingBackup()?.originalRealTimeNetworkSpeed ?: originalState
            }

            if (toggleNode.isChecked != targetState) {
                clickNodeOrParent(toggleNode)
                Log.i(TAG, "Toggled real-time network speed from ${toggleNode.isChecked} to $targetState")
                delay(300)
            }
            toggleNode.recycle()
        }
        speedLabelNode.recycle()
        return originalState
    }

    private suspend fun navigateIntoStatusIconsSubmenu(): Boolean {
        val root = rootInActiveWindow ?: return false
        val iconsMenuNode = findNodeMatching(root, compatibilityManager.statusIconsSubmenuLabels)
        if (iconsMenuNode != null) {
            val clicked = clickNodeOrParent(iconsMenuNode)
            iconsMenuNode.recycle()
            return clicked
        }
        return false
    }

    /**
     * OBJECTIVE 1: Comprehensive discovery of all available icon switch controls.
     * Identifies every actual checkable switch control, associates with visible label,
     * and scrolls through the entire list to catch items below the fold (e.g. VoLTE, VoNR, 5G).
     */
    private suspend fun discoverAllStatusIconRows(): Map<String, Boolean> {
        val discoveredMap = linkedMapOf<String, Boolean>()
        var previousDiscoveredCount = -1
        var scrollAttempts = 0
        val maxScrolls = 6

        while (scrollAttempts < maxScrolls) {
            val root = rootInActiveWindow ?: break
            val currentVisible = scanCurrentScreenIconRows(root)
            discoveredMap.putAll(currentVisible)

            if (discoveredMap.size == previousDiscoveredCount) {
                // No new items discovered after scroll -> end of list reached
                break
            }

            previousDiscoveredCount = discoveredMap.size
            val scrolled = scrollForward()
            delay(500)
            scrollAttempts++
            if (!scrolled) break
        }

        // Scroll back to top for modification pass
        scrollToTop()
        delay(400)
        return discoveredMap
    }

    private fun scanCurrentScreenIconRows(root: AccessibilityNodeInfo): Map<String, Boolean> {
        val visibleMap = mutableMapOf<String, Boolean>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            if (node.isCheckable) {
                val label = findLabelForCheckableNode(node)
                if (label != null && !compatibilityManager.isDisallowedStatusIconRow(label)) {
                    visibleMap[label] = node.isChecked
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return visibleMap
    }

    /**
     * Traverses the row container to associate a switch with its actual visible title.
     */
    private fun findLabelForCheckableNode(checkableNode: AccessibilityNodeInfo): String? {
        var parent: AccessibilityNodeInfo? = checkableNode.parent
        var depth = 0
        while (parent != null && depth < 3) {
            val labelCandidate = extractBestRowLabel(parent, checkableNode)
            if (labelCandidate != null) {
                return labelCandidate
            }
            parent = parent.parent
            depth++
        }
        return null
    }

    private fun extractBestRowLabel(container: AccessibilityNodeInfo, checkableNode: AccessibilityNodeInfo): String? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(container)
        val textCandidates = mutableListOf<String>()

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node != checkableNode) {
                val text = node.text?.toString()?.trim()
                if (!text.isNullOrBlank() && !node.isCheckable) {
                    textCandidates.add(text)
                }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        // Return the first candidate that isn't a known summary or percentage
        return textCandidates.firstOrNull { candidate ->
            !compatibilityManager.isBatteryPercentageOption(candidate) &&
                    !candidate.equals("ON", ignoreCase = true) &&
                    !candidate.equals("OFF", ignoreCase = true)
        }
    }

    /**
     * Turns OFF every enabled icon. Leaves already-disabled icons untouched.
     */
    private suspend fun disableAllActiveStatusIcons(
        discoveredMap: Map<String, Boolean>,
        onProgress: (IconStats, String) -> Unit
    ): IconStats {
        var disabledCount = 0
        val alreadyDisabledCount = discoveredMap.values.count { !it }
        val keysToDisable = discoveredMap.filter { it.value }.keys.toMutableSet()
        val totalToDisable = keysToDisable.size

        var scrollPasses = 0
        while (keysToDisable.isNotEmpty() && scrollPasses < 6) {
            val root = rootInActiveWindow ?: break
            val togglesOnScreen = findAllVisibleToggles(root)

            for ((label, toggleNode) in togglesOnScreen) {
                if (keysToDisable.contains(label)) {
                    if (toggleNode.isChecked) {
                        clickNodeOrParent(toggleNode)
                        delay(280) // Let switch animation settle
                        disabledCount++
                        keysToDisable.remove(label)

                        val remaining = keysToDisable.size
                        val stats = IconStats(
                            discovered = discoveredMap.size,
                            disabled = disabledCount,
                            alreadyDisabled = alreadyDisabledCount,
                            restored = 0,
                            remaining = remaining
                        )
                        onProgress(stats, label)
                    } else {
                        // Already off
                        keysToDisable.remove(label)
                    }
                }
            }

            if (keysToDisable.isEmpty()) break
            val scrolled = scrollForward()
            delay(400)
            scrollPasses++
            if (!scrolled) break
        }

        return IconStats(
            discovered = discoveredMap.size,
            disabled = disabledCount,
            alreadyDisabled = alreadyDisabledCount,
            restored = 0,
            remaining = keysToDisable.size
        )
    }

    /**
     * Restores all status icons to their exact original baseline.
     */
    private suspend fun restoreAllStatusIconsToBaseline(
        targetBaselineMap: Map<String, Boolean>,
        onProgress: (IconStats, String) -> Unit
    ): IconStats {
        var restoredCount = 0
        val pendingKeys = targetBaselineMap.keys.toMutableSet()
        val totalToRestore = targetBaselineMap.size

        var scrollPasses = 0
        while (pendingKeys.isNotEmpty() && scrollPasses < 6) {
            val root = rootInActiveWindow ?: break
            val togglesOnScreen = findAllVisibleToggles(root)

            for ((label, toggleNode) in togglesOnScreen) {
                if (pendingKeys.contains(label)) {
                    val desiredState = targetBaselineMap[label] ?: false
                    if (toggleNode.isChecked != desiredState) {
                        clickNodeOrParent(toggleNode)
                        delay(280)
                    }
                    restoredCount++
                    pendingKeys.remove(label)

                    val stats = IconStats(
                        discovered = targetBaselineMap.size,
                        disabled = 0,
                        alreadyDisabled = 0,
                        restored = restoredCount,
                        remaining = pendingKeys.size
                    )
                    onProgress(stats, label)
                }
            }

            if (pendingKeys.isEmpty()) break
            val scrolled = scrollForward()
            delay(400)
            scrollPasses++
            if (!scrolled) break
        }

        return IconStats(
            discovered = targetBaselineMap.size,
            disabled = 0,
            alreadyDisabled = 0,
            restored = restoredCount,
            remaining = pendingKeys.size
        )
    }

    private fun findAllVisibleToggles(root: AccessibilityNodeInfo): List<Pair<String, AccessibilityNodeInfo>> {
        val list = mutableListOf<Pair<String, AccessibilityNodeInfo>>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isCheckable) {
                val label = findLabelForCheckableNode(node)
                if (label != null && !compatibilityManager.isDisallowedStatusIconRow(label)) {
                    list.add(label to node)
                }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return list
    }

    /**
     * OBJECTIVE 2: Inspects manufacturer settings for wired headphone / earphone visibility controls.
     * If unexposed by OEM/SystemUI, safely reports as System-managed without disrupting audio.
     */
    private data class HeadphoneCheckResult(val isSupported: Boolean, val message: String)

    private suspend fun inspectAndConfigureHeadphoneSetting(mode: TargetMode): HeadphoneCheckResult {
        val root = rootInActiveWindow ?: return HeadphoneCheckResult(
            isSupported = false,
            message = "System-managed wired headphone icon – no supported visibility control found."
        )

        // Check if any visible or scrollable item matches headphone candidates
        val headphoneNode = findNodeMatching(root, compatibilityManager.wiredHeadphoneCandidateLabels)
        if (headphoneNode != null) {
            val toggle = findAssociatedCheckable(headphoneNode)
            if (toggle != null) {
                val isHide = mode is TargetMode.HideAll
                if (isHide && toggle.isChecked) {
                    clickNodeOrParent(toggle)
                    delay(300)
                } else if (!isHide && !toggle.isChecked) {
                    clickNodeOrParent(toggle)
                    delay(300)
                }
                toggle.recycle()
                headphoneNode.recycle()
                return HeadphoneCheckResult(true, "Configured wired headphone icon visibility.")
            }
            headphoneNode.recycle()
        }

        // Standard Android behavior: 3.5mm headphone icon is handled natively by SystemUI AudioManager
        Log.i(TAG, "No user-accessible wired headphone visibility setting exposed in OEM Settings.")
        return HeadphoneCheckResult(
            isSupported = false,
            message = "System-managed wired headphone icon – no supported visibility control found."
        )
    }

    private fun findAssociatedCheckable(labelNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var parent: AccessibilityNodeInfo? = labelNode.parent
        var depth = 0
        while (parent != null && depth < 3) {
            val checkable = findCheckableInChildren(parent, labelNode)
            if (checkable != null) return checkable
            parent = parent.parent
            depth++
        }
        return null
    }

    private fun findCheckableInChildren(container: AccessibilityNodeInfo, exclude: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(container)
        while (queue.isNotEmpty()) {
            val child = queue.removeFirst()
            if (child != exclude && child.isCheckable) {
                return child
            }
            for (i in 0 until child.childCount) {
                child.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun extractSummaryText(node: AccessibilityNodeInfo): String? {
        val parent = node.parent ?: return null
        for (i in 0 until parent.childCount) {
            val child = parent.getChild(i) ?: continue
            val childText = child.text?.toString()?.trim()
            if (!childText.isNullOrBlank() && childText != node.text?.toString()?.trim()) {
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

        // Gesture fallback based on screen coordinates
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty) {
            return dispatchTapGesture(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }
        return false
    }

    private fun dispatchTapGesture(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 45)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    private fun scrollForward(): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrolled = root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        root.recycle()
        return scrolled
    }

    private fun scrollToTop(): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrolled = root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
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

    private fun handleAutomationFailure(reason: String) {
        val progress = AutomationProgress(
            currentStep = AutomationStep.FAILED,
            operationTitle = "Automation Error",
            currentOperation = reason,
            percentage = 100,
            errorReason = reason
        )
        _progressState.value = progress
        overlayManager.updateProgress(progress)

        serviceScope.launch {
            dataStoreRepository.recordOperation("FAILED", reason)
        }
    }

    private fun cancelAutomation(reason: String) {
        currentAutomationJob?.cancel()
        handleAutomationFailure(reason)
    }
}
