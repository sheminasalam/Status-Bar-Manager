package com.statusbarmanager.app.data

/**
 * Model representing a persistent snapshot of the user's status bar configuration
 * prior to applying "Hide Status Bar Information".
 */
data class StatusBarBackupModel(
    val id: String = "backup_v1",
    val timestamp: Long = System.currentTimeMillis(),
    val originalTimeSetting: String, // e.g. "Show hours and minutes", "Do not show"
    val originalRealTimeNetworkSpeed: Boolean,
    val originalStatusIcons: Map<String, Boolean>, // Icon name -> original checked state
    val deviceManufacturer: String,
    val deviceModel: String,
    val androidVersion: Int,
    val isHideActive: Boolean = false,
    val verifiedAllRestored: Boolean = false
) {
    companion object {
        const val TIME_DO_NOT_SHOW = "Do not show"
        const val TIME_SHOW_HOURS_MINUTES = "Show hours and minutes"
        const val TIME_SHOW_SECONDS = "Show hours, minutes and seconds"
        
        fun empty(): StatusBarBackupModel = StatusBarBackupModel(
            originalTimeSetting = TIME_SHOW_HOURS_MINUTES,
            originalRealTimeNetworkSpeed = true,
            originalStatusIcons = emptyMap(),
            deviceManufacturer = android.os.Build.MANUFACTURER,
            deviceModel = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.SDK_INT,
            isHideActive = false
        )
    }
}
