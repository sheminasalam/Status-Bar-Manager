package com.statusbarmanager.app.automation

sealed class TargetMode {
    object HideAll : TargetMode()
    object RestoreOriginal : TargetMode()
}

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

data class AutomationProgress(
    val currentStep: AutomationStep = AutomationStep.IDLE,
    val operationTitle: String = "Status Bar Configuration",
    val currentOperation: String = "Ready to configure",
    val percentage: Int = 0,
    val stepIndex: Int = 0,
    val totalSteps: Int = 7,
    val checklist: List<ProcessChecklistItem> = defaultChecklist(),
    val iconStats: IconStats = IconStats(),
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
            ProcessChecklistItem("headphone", "Wired headphone indicator inspection"),
            ProcessChecklistItem("verify", "Final verification & state persistence")
        )
    }
}
