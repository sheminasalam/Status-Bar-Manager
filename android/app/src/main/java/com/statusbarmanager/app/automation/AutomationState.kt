package com.statusbarmanager.app.automation

enum class EngageMode {
    STATUS_BAR_ONLY,
    FORCE_STOP_APPS_ONLY,
    BOTH;

    companion object {
        fun fromString(value: String): EngageMode {
            return try {
                valueOf(value)
            } catch (e: Exception) {
                BOTH
            }
        }
    }
}

sealed class TargetMode {
    object HideAll : TargetMode()
    object RestoreOriginal : TargetMode()
    object ForceStopOnly : TargetMode()
    object BothEngage : TargetMode()
    object BothDisengage : TargetMode()
}

/** Whether a completed operation should mark the persistent status bar as hidden. */
internal fun shouldPersistStatusBarHidden(mode: TargetMode): Boolean =
    mode is TargetMode.HideAll || mode is TargetMode.BothEngage

enum class AutomationStep {
    IDLE,
    STARTING,
    OPENING_SETTINGS,
    NAVIGATING_TO_NOTIFICATIONS_STATUS_BAR,
    NAVIGATING_TO_STATUS_BAR,
    READING_BASELINE_SETTINGS,
    CONFIGURING_TIME,
    CONFIGURING_NETWORK_SPEED,
    NAVIGATING_TO_STATUS_ICONS,
    DISCOVERING_ALL_STATUS_ICONS,
    DISABLING_ALL_STATUS_ICONS,
    RESTORING_ALL_STATUS_ICONS,
    CHECKING_HEADPHONE_INDICATOR,
    SCANNING_APPLICATIONS,
    FORCE_STOPPING_APPS,
    VERIFYING_CONFIGURATION,
    NAVIGATING_BACK_TO_APP,
    COMPLETED,
    PARTIAL,
    FAILED,
    MANUAL_ASSISTANCE_REQUIRED
}

enum class StepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    PARTIAL,
    FAILED,
    SKIPPED
}

data class ProcessChecklistItem(
    val id: String,
    val title: String,
    val status: StepStatus = StepStatus.PENDING,
    val details: String? = null
)

data class IconStats(
    val discovered: Int = 0,
    val disabled: Int = 0,
    val alreadyDisabled: Int = 0,
    val restored: Int = 0,
    val remaining: Int = 0
)

data class AppForceStopStats(
    val totalEligible: Int = 0,
    val successfullyStopped: Int = 0,
    val alreadyStopped: Int = 0,
    val skippedProtected: Int = 0,
    val skippedWhitelisted: Int = 0,
    val failed: Int = 0,
    val remaining: Int = 0,
    val currentAppName: String = ""
)

data class AutomationProgress(
    val currentStep: AutomationStep = AutomationStep.IDLE,
    val operationTitle: String = "Status Bar Configuration",
    val currentOperation: String = "Ready to configure",
    val percentage: Int = 0,
    val stepIndex: Int = 0,
    val totalSteps: Int = 7,
    val checklist: List<ProcessChecklistItem> = defaultChecklist(),
    val iconStats: IconStats = IconStats(),
    val appStats: AppForceStopStats = AppForceStopStats(),
    val scannedIcons: Map<String, Boolean> = emptyMap(),
    val changedIcons: Set<String> = emptySet(),
    val failedItems: List<String> = emptyList(),
    val headphoneStatus: String = "Not checked",
    val isPartialResult: Boolean = false,
    val errorReason: String? = null
) {
    companion object {
        fun defaultChecklist(): List<ProcessChecklistItem> = listOf(
            ProcessChecklistItem("backup", "Original settings baseline backup"),
            ProcessChecklistItem("time", "Time display configuration"),
            ProcessChecklistItem("speed", "Real-time network speed configuration"),
            ProcessChecklistItem("scan_icons", "Comprehensive status icons discovery"),
            ProcessChecklistItem("toggle_icons", "Configuring status icons (VoLTE, SIM, Wi-Fi, etc.)"),
            ProcessChecklistItem("apps", "Force-stop eligible non-system apps"),
            ProcessChecklistItem("verify", "Final verification & state persistence")
        )

        fun forMode(mode: EngageMode, isEngage: Boolean): List<ProcessChecklistItem> {
            return when (mode) {
                EngageMode.STATUS_BAR_ONLY -> {
                    if (isEngage) {
                        listOf(
                            ProcessChecklistItem("backup", "Original settings baseline backup"),
                            ProcessChecklistItem("time", "Time display configuration"),
                            ProcessChecklistItem("speed", "Real-time network speed configuration"),
                            ProcessChecklistItem("scan_icons", "Comprehensive status icons discovery"),
                            ProcessChecklistItem("toggle_icons", "Configuring status icons"),
                            ProcessChecklistItem("headphone", "Wired headphone indicator inspection"),
                            ProcessChecklistItem("verify", "Final verification & state persistence")
                        )
                    } else {
                        listOf(
                            ProcessChecklistItem("time", "Restoring Time display"),
                            ProcessChecklistItem("speed", "Restoring Real-time network speed"),
                            ProcessChecklistItem("toggle_icons", "Restoring original status icons"),
                            ProcessChecklistItem("verify", "Verifying restored configuration")
                        )
                    }
                }
                EngageMode.FORCE_STOP_APPS_ONLY -> {
                    listOf(
                        ProcessChecklistItem("scan_apps", "Discover installed user applications"),
                        ProcessChecklistItem("filter_apps", "Apply system & user whitelists"),
                        ProcessChecklistItem("force_stop", "Accessibility Force Stop automation"),
                        ProcessChecklistItem("verify_apps", "Record final application outcomes")
                    )
                }
                EngageMode.BOTH -> {
                    if (isEngage) {
                        listOf(
                            ProcessChecklistItem("backup", "Original settings baseline backup"),
                            ProcessChecklistItem("status_bar", "Status bar hiding automation"),
                            ProcessChecklistItem("scan_apps", "Application discovery & whitelisting"),
                            ProcessChecklistItem("force_stop", "Force-stopping eligible apps"),
                            ProcessChecklistItem("verify", "Final verification & persistence")
                        )
                    } else {
                        listOf(
                            ProcessChecklistItem("time", "Restoring Time display"),
                            ProcessChecklistItem("speed", "Restoring Real-time network speed"),
                            ProcessChecklistItem("toggle_icons", "Restoring original status icons"),
                            ProcessChecklistItem("verify", "Verifying restored configuration")
                        )
                    }
                }
            }
        }
    }
}
