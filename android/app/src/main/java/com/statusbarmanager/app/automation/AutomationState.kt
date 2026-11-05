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
    READING_CURRENT_TIME,
    CHANGING_TIME,
    READING_NETWORK_SPEED,
    TOGGLING_NETWORK_SPEED,
    NAVIGATING_TO_STATUS_ICONS,
    SCANNING_STATUS_ICONS,
    TOGGLING_STATUS_ICONS,
    VERIFYING_CHANGES,
    NAVIGATING_BACK_TO_APP,
    COMPLETED,
    FAILED,
    MANUAL_ASSISTANCE_REQUIRED
}

data class AutomationProgress(
    val currentStep: AutomationStep = AutomationStep.IDLE,
    val message: String = "Ready to configure",
    val stepIndex: Int = 0,
    val totalSteps: Int = 8,
    val scannedIcons: Map<String, Boolean> = emptyMap(),
    val changedIcons: Set<String> = emptySet(),
    val failedItems: List<String> = emptyList(),
    val errorReason: String? = null
)
