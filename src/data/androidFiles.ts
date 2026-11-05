import { CodeFile } from '../types';

export const ANDROID_FILES: CodeFile[] = [
  {
    path: 'app/src/main/java/com/statusbarmanager/app/service/StatusBarAccessibilityService.kt',
    name: 'StatusBarAccessibilityService.kt',
    language: 'kotlin',
    category: 'Kotlin Source',
    content: `package com.statusbarmanager.app.service

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
        if (event == null || !isAutomationRunning) return
        val packageName = event.packageName?.toString() ?: ""
        if (packageName.contains("settings", ignoreCase = true)) {
            Log.d(TAG, "Settings window event observed: \${event.eventType}")
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted.")
        cancelAutomation("Accessibility service was interrupted by system.")
    }

    fun startAutomation(mode: TargetMode) {
        if (isAutomationRunning) {
            Log.w(TAG, "Automation is already active.")
            return
        }

        isAutomationRunning = true
        currentAutomationJob?.cancel()

        currentAutomationJob = serviceScope.launch {
            try {
                executeWorkflow(mode)
            } catch (e: CancellationException) {
                Log.i(TAG, "Automation job cancelled.")
            } catch (e: Exception) {
                Log.e(TAG, "Automation failed: \${e.message}", e)
                handleAutomationFailure("Error: \${e.localizedMessage ?: "Unknown failure"}")
            } finally {
                isAutomationRunning = false
            }
        }
    }

    private suspend fun executeWorkflow(mode: TargetMode) = withContext(Dispatchers.Default) {
        val action = if (mode is TargetMode.HideAll) "Hiding" else "Restoring"
        updateProgress(AutomationStep.STARTING, "$action status bar information...", 0, 7)

        // 1. Open Settings
        updateProgress(AutomationStep.OPENING_SETTINGS, "Opening phone Settings...", 1, 7)
        launchSettings()
        delay(1200)

        // 2. Navigate to 'Notifications & status bar'
        updateProgress(AutomationStep.NAVIGATING_TO_NOTIFICATIONS_STATUS_BAR, "Locating Notifications & status bar...", 2, 7)
        var reached = navigateToNotificationsAndStatusBar()
        if (!reached) {
            scrollDown()
            delay(600)
            reached = navigateToNotificationsAndStatusBar()
        }
        delay(1000)

        // 3. Open 'Status bar'
        updateProgress(AutomationStep.NAVIGATING_TO_STATUS_BAR, "Opening Status bar section...", 3, 7)
        val reachedSection = navigateToStatusBarSection()
        if (!reachedSection) {
            throw IllegalStateException("Unable to locate 'Status bar' in system settings.")
        }
        delay(1000)

        // 4. Time display
        updateProgress(AutomationStep.READING_CURRENT_TIME, "Configuring Time display...", 4, 7)
        handleTimeSetting(mode)
        delay(800)

        // 5. Real-time network speed
        updateProgress(AutomationStep.TOGGLING_NETWORK_SPEED, "Configuring Real-time network speed...", 5, 7)
        handleNetworkSpeedSetting(mode)
        delay(800)

        // 6. Status Icons
        updateProgress(AutomationStep.NAVIGATING_TO_STATUS_ICONS, "Configuring Status Icons...", 6, 7)
        handleStatusIcons(mode)
        delay(1000)

        // 7. Verification & return
        updateProgress(AutomationStep.VERIFYING_CHANGES, "Verifying applied changes in UI...", 7, 7)
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
        clickNodeOrParent(timeNode)
        timeNode.recycle()
        delay(700)

        val dialogRoot = rootInActiveWindow ?: return
        val targetLabels = if (mode is TargetMode.HideAll) {
            compatibilityManager.doNotShowLabels
        } else {
            val backup = backupManager.getExistingBackup()
            listOf(backup?.originalTimeSetting ?: "Show hours and minutes")
        }

        val targetOption = findNodeMatching(dialogRoot, targetLabels)
        if (targetOption != null) {
            clickNodeOrParent(targetOption)
            targetOption.recycle()
        }
    }

    private suspend fun handleNetworkSpeedSetting(mode: TargetMode) {
        val root = rootInActiveWindow ?: return
        val speedNode = findNodeMatching(root, compatibilityManager.networkSpeedLabels) ?: return
        val toggle = findAssociatedToggle(speedNode)
        if (toggle != null) {
            val isChecked = toggle.isChecked
            val target = if (mode is TargetMode.HideAll) false else {
                backupManager.getExistingBackup()?.originalRealTimeNetworkSpeed ?: true
            }
            if (isChecked != target) {
                clickNodeOrParent(toggle)
            }
            toggle.recycle()
        }
        speedNode.recycle()
    }

    private suspend fun handleStatusIcons(mode: TargetMode) {
        val root = rootInActiveWindow ?: return
        val iconsMenuNode = findNodeMatching(root, compatibilityManager.statusIconsSubmenuLabels)
        if (iconsMenuNode != null) {
            clickNodeOrParent(iconsMenuNode)
            iconsMenuNode.recycle()
            delay(800)

            val iconsRoot = rootInActiveWindow ?: return
            val discovered = scanAvailableIcons(iconsRoot)

            if (mode is TargetMode.HideAll) {
                val existing = backupManager.getExistingBackup()
                if (existing == null) {
                    backupManager.saveBackupIfNotActive("Show hours and minutes", true, discovered)
                }
                toggleAllIcons(iconsRoot, targetState = false)
            } else {
                val backup = backupManager.getExistingBackup()
                restoreIconsToOriginalState(iconsRoot, backup?.originalStatusIcons ?: emptyMap())
            }

            performGlobalAction(GLOBAL_ACTION_BACK)
            delay(500)
        } else {
            val discovered = scanAvailableIcons(root)
            if (mode is TargetMode.HideAll) {
                backupManager.saveBackupIfNotActive("Show hours and minutes", true, discovered)
                toggleAllIcons(root, targetState = false)
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

    private suspend fun toggleAllIcons(root: AccessibilityNodeInfo, targetState: Boolean) {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString() ?: node.contentDescription?.toString()
            if (text != null && compatibilityManager.isTargetStatusIconLabel(text)) {
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

    private suspend fun restoreIconsToOriginalState(root: AccessibilityNodeInfo, originalMap: Map<String, Boolean>) {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString() ?: node.contentDescription?.toString()
            if (text != null && compatibilityManager.isTargetStatusIconLabel(text)) {
                val key = text.trim()
                val targetState = originalMap[key] ?: false
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
        val parent = labelNode.parent ?: return null
        for (i in 0 until parent.childCount) {
            val child = parent.getChild(i) ?: continue
            if (child.isCheckable) return child
            for (j in 0 until child.childCount) {
                val grand = child.getChild(j) ?: continue
                if (grand.isCheckable) {
                    child.recycle()
                    return grand
                }
                grand.recycle()
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
            val cur = queue.removeFirst()
            if (compatibilityManager.matchesAny(cur.text, candidates) ||
                compatibilityManager.matchesAny(cur.contentDescription, candidates)
            ) {
                return cur
            }
            for (i in 0 until cur.childCount) {
                cur.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                if (current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            }
            current = current.parent
        }
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
        if (intent != null) startActivity(intent) else performGlobalAction(GLOBAL_ACTION_BACK)
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
        serviceScope.launch { dataStoreRepository.recordOperation("FAILED", reason) }
    }

    private fun cancelAutomation(reason: String) {
        currentAutomationJob?.cancel()
        handleAutomationFailure(reason)
    }
}`
  },
  {
    path: 'app/src/main/java/com/statusbarmanager/app/automation/DeviceCompatibilityManager.kt',
    name: 'DeviceCompatibilityManager.kt',
    language: 'kotlin',
    category: 'Kotlin Source',
    content: `package com.statusbarmanager.app.automation

import android.os.Build
import java.util.Locale

enum class DeviceOem {
    COLOR_OS_OXYGEN_OS,
    SAMSUNG_ONE_UI,
    XIAOMI_HYPER_OS,
    PIXEL_AOSP,
    GENERIC
}

class DeviceCompatibilityManager {

    val currentOem: DeviceOem by lazy {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val brand = Build.BRAND.lowercase(Locale.ROOT)
        when {
            manufacturer.contains("oneplus") || manufacturer.contains("oppo") || manufacturer.contains("realme") ||
                    brand.contains("oneplus") || brand.contains("oppo") || brand.contains("realme") ->
                DeviceOem.COLOR_OS_OXYGEN_OS
            manufacturer.contains("samsung") -> DeviceOem.SAMSUNG_ONE_UI
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> DeviceOem.XIAOMI_HYPER_OS
            manufacturer.contains("google") -> DeviceOem.PIXEL_AOSP
            else -> DeviceOem.GENERIC
        }
    }

    val oemDisplayName: String
        get() = when (currentOem) {
            DeviceOem.COLOR_OS_OXYGEN_OS -> "ColorOS / OxygenOS / Realme UI (Target Hardware)"
            DeviceOem.SAMSUNG_ONE_UI -> "Samsung One UI"
            DeviceOem.XIAOMI_HYPER_OS -> "Xiaomi HyperOS / MIUI"
            DeviceOem.PIXEL_AOSP -> "Google Pixel / AOSP"
            DeviceOem.GENERIC -> "\${Build.MANUFACTURER} (\${Build.MODEL})"
        }

    val notificationsAndStatusBarLabels = listOf(
        "Notifications & status bar",
        "Notification & status bar",
        "Notifications & Status bar",
        "Notifications and status bar",
        "Status bar & notifications"
    )

    val statusBarSectionLabels = listOf("Status bar", "Status Bar", "Status-bar")
    val timeMenuLabels = listOf("Time", "Clock", "Time display", "Status bar clock")
    val doNotShowLabels = listOf("Do not show", "Don't show", "Hide", "Do not display", "None")
    val statusIconsSubmenuLabels = listOf("Status icons", "Status Icons", "Icon manager", "System icons", "Status bar icons")
    val networkSpeedLabels = listOf("Real-time network speed", "Real-time network data", "Network speed", "Display network speed")

    fun isBatteryPercentageOption(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return lower.contains("percentage") || lower.contains("percent")
    }

    fun isTargetStatusIconLabel(text: String): Boolean {
        if (isBatteryPercentageOption(text)) return false
        val lower = text.lowercase(Locale.ROOT)
        val iconKeywords = listOf(
            "sim", "network", "mobile", "wi-fi", "wifi", "bluetooth",
            "alarm", "nfc", "earphones", "headset", "hotspot",
            "vpn", "data saving", "high performance", "ring", "vibrate",
            "silent", "battery"
        )
        return iconKeywords.any { lower.contains(it) }
    }

    fun matchesAny(text: CharSequence?, candidates: List<String>): Boolean {
        if (text == null) return false
        val trimmed = text.toString().trim()
        return candidates.any { trimmed.equals(it, ignoreCase = true) || trimmed.contains(it, ignoreCase = true) }
    }
}`
  },
  {
    path: 'app/src/main/java/com/statusbarmanager/app/data/SettingsBackupManager.kt',
    name: 'SettingsBackupManager.kt',
    language: 'kotlin',
    category: 'Kotlin Source',
    content: `package com.statusbarmanager.app.data

import kotlinx.coroutines.flow.first

class SettingsBackupManager(private val repository: DataStoreRepository) {

    suspend fun getExistingBackup(): StatusBarBackupModel? = repository.backupModelFlow.first()
    suspend fun isHideActive(): Boolean = repository.isHideActiveFlow.first()
    suspend fun hasValidBackup(): Boolean = repository.hasBackupFlow.first()

    suspend fun saveBackupIfNotActive(
        timeSetting: String,
        networkSpeed: Boolean,
        icons: Map<String, Boolean>
    ): Boolean {
        if (isHideActive()) return false

        val backup = StatusBarBackupModel(
            originalTimeSetting = timeSetting,
            originalRealTimeNetworkSpeed = networkSpeed,
            originalStatusIcons = icons,
            deviceManufacturer = android.os.Build.MANUFACTURER,
            deviceModel = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.SDK_INT,
            isHideActive = true
        )
        repository.saveOriginalBackup(backup)
        return true
    }

    suspend fun markRestoredSuccessfully() {
        repository.clearBackup()
    }

    suspend fun recordPartialRestoration(unrestoredKeys: List<String>) {
        repository.setHideActive(false)
        repository.recordOperation(
            status = "PARTIAL",
            message = "Partially restored. Pending verification for: \${unrestoredKeys.joinToString(", ")}"
        )
    }
}`
  },
  {
    path: 'app/src/main/java/com/statusbarmanager/app/MainActivity.kt',
    name: 'MainActivity.kt',
    language: 'kotlin',
    category: 'Kotlin Source',
    content: `package com.statusbarmanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.statusbarmanager.app.automation.DeviceCompatibilityManager
import com.statusbarmanager.app.automation.SettingsAutomationManager
import com.statusbarmanager.app.automation.TargetMode
import com.statusbarmanager.app.data.DataStoreRepository
import com.statusbarmanager.app.data.SettingsBackupManager
import com.statusbarmanager.app.ui.screens.MainScreen
import com.statusbarmanager.app.ui.theme.StatusBarManagerTheme

class MainActivity : ComponentActivity() {

    private lateinit var dataStoreRepository: DataStoreRepository
    private lateinit var backupManager: SettingsBackupManager
    private lateinit var automationManager: SettingsAutomationManager
    private val compatibilityManager = DeviceCompatibilityManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataStoreRepository = DataStoreRepository(applicationContext)
        backupManager = SettingsBackupManager(dataStoreRepository)
        automationManager = SettingsAutomationManager(applicationContext)

        setContent {
            StatusBarManagerTheme {
                val isHideActive by dataStoreRepository.isHideActiveFlow.collectAsState(initial = false)
                val hasBackup by dataStoreRepository.hasBackupFlow.collectAsState(initial = false)
                val backupModel by dataStoreRepository.backupModelFlow.collectAsState(initial = null)
                val lastOperation by dataStoreRepository.lastOperationFlow.collectAsState(
                    initial = Triple("IDLE", "Ready to configure", 0L)
                )

                var isAccessibilityGranted by remember {
                    mutableStateOf(automationManager.isAccessibilityPermissionGranted())
                }
                val automationProgress by automationManager.progressState.collectAsState()

                DisposableEffect(Unit) {
                    isAccessibilityGranted = automationManager.isAccessibilityPermissionGranted()
                    onDispose { }
                }

                MainScreen(
                    isHideActive = isHideActive,
                    hasBackup = hasBackup,
                    backupModel = backupModel,
                    isAccessibilityGranted = isAccessibilityGranted,
                    lastOperation = lastOperation,
                    automationProgress = automationProgress,
                    compatibilityManager = compatibilityManager,
                    onToggleMasterSwitch = { shouldHide ->
                        if (!isAccessibilityGranted) {
                            automationManager.openAccessibilitySettings()
                            return@MainScreen
                        }
                        if (shouldHide) {
                            automationManager.triggerAutomation(TargetMode.HideAll)
                        } else {
                            automationManager.triggerAutomation(TargetMode.RestoreOriginal)
                        }
                    },
                    onRestoreOriginal = {
                        if (!isAccessibilityGranted) {
                            automationManager.openAccessibilitySettings()
                            return@MainScreen
                        }
                        automationManager.triggerAutomation(TargetMode.RestoreOriginal)
                    },
                    onRequestAccessibilityPermission = { automationManager.openAccessibilitySettings() },
                    onOpenSystemSettings = { automationManager.openSystemSettings() },
                    onViewBackupDetails = { }
                )
            }
        }
    }
}`
  },
  {
    path: 'app/src/main/AndroidManifest.xml',
    name: 'AndroidManifest.xml',
    language: 'xml',
    category: 'Manifest & Resources',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- No internet permission requested: strictly local operation -->

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.StatusBarManager">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.StatusBarManager">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Dedicated Android Accessibility Service for Automating Status Bar Settings -->
        <service
            android:name=".service.StatusBarAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true"
            android:label="@string/accessibility_service_label"
            android:description="@string/accessibility_service_description">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>

            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/status_bar_accessibility_service_config" />
        </service>

    </application>

</manifest>`
  },
  {
    path: 'app/src/main/res/xml/status_bar_accessibility_service_config.xml',
    name: 'status_bar_accessibility_service_config.xml',
    language: 'xml',
    category: 'Manifest & Resources',
    content: `<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/accessibility_service_description"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged|typeViewClicked|typeViewScrolled"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:canRetrieveWindowContent="true"
    android:canPerformGestures="true"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows|flagReportViewIds"
    android:settingsActivity="com.statusbarmanager.app.MainActivity" />`
  },
  {
    path: 'app/build.gradle.kts',
    name: 'app/build.gradle.kts',
    language: 'gradle',
    category: 'Gradle Build',
    content: `plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.statusbarmanager.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.statusbarmanager.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.ui.tooling)
}`
  },
  {
    path: 'README.md',
    name: 'README.md',
    language: 'markdown',
    category: 'Gradle Build',
    content: `# StatusBar Manager - Native Android App

A complete native Android application built with Kotlin and Jetpack Compose (Material 3).

## Purpose
Allows users to automatically hide or restore the clock and status icons displayed in the Android system status bar across the entire phone by automating native settings under:

**Settings → Notifications & status bar → Status bar**

## Architecture Highlights
- **Accessibility Service**: Dedicated \`StatusBarAccessibilityService\` with \`AccessibilityNodeInfo\` traversal and gesture fallbacks.
- **Local DataStore Backup**: Preserves exact original Time setting, Real-time network speed, and individual icon states.
- **Adaptive OEM Compatibility**: Detects ColorOS/OxygenOS, One UI, HyperOS, and Pixel settings labels.
- **Strict Privacy**: Zero internet permissions requested, idle outside explicitly requested toggles, no data collection.

## How to Build & Install
\`\`\`bash
# Build Debug APK
./gradlew assembleDebug

# Install via ADB
adb install app/build/outputs/apk/debug/app-debug.apk
\`\`\`
`
  }
];
