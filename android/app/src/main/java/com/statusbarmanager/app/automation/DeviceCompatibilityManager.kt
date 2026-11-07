package com.statusbarmanager.app.automation

import android.os.Build
import java.util.Locale

enum class DeviceOem {
    COLOR_OS_OXYGEN_OS, // OPPO, OnePlus, Realme
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
            manufacturer.contains("samsung") ->
                DeviceOem.SAMSUNG_ONE_UI
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") ->
                DeviceOem.XIAOMI_HYPER_OS
            manufacturer.contains("google") ->
                DeviceOem.PIXEL_AOSP
            else ->
                DeviceOem.GENERIC
        }
    }

    val oemDisplayName: String
        get() = when (currentOem) {
            DeviceOem.COLOR_OS_OXYGEN_OS -> "ColorOS / OxygenOS / Realme UI"
            DeviceOem.SAMSUNG_ONE_UI -> "Samsung One UI"
            DeviceOem.XIAOMI_HYPER_OS -> "Xiaomi HyperOS / MIUI"
            DeviceOem.PIXEL_AOSP -> "Google Pixel / AOSP"
            DeviceOem.GENERIC -> "${Build.MANUFACTURER} (${Build.MODEL})"
        }

    // Settings Navigation Target Labels
    val notificationsAndStatusBarLabels = listOf(
        "Notifications & status bar",
        "Notification & status bar",
        "Notifications & Status bar",
        "Notifications and status bar",
        "Notification and status bar",
        "Notifications & Status Bar",
        "Status bar & notifications",
        "Notifications & control center",
        "Notification & Control Centre",
        "Notifications"
    )

    val statusBarSectionLabels = listOf(
        "Status bar",
        "Status Bar",
        "Status-bar"
    )

    val timeMenuLabels = listOf(
        "Time",
        "Clock",
        "Time display",
        "Status bar clock"
    )

    val batteryStyleLabels = listOf(
        "Battery style",
        "Battery icon",
        "Battery display",
        "Battery icon style"
    )

    val doNotShowLabels = listOf(
        "Do not show",
        "Don't show",
        "Hide",
        "Do not display",
        "None",
        "Off",
        "Hidden"
    )

    val statusIconsSubmenuLabels = listOf(
        "Status icons",
        "Status Icons",
        "Icon manager",
        "System icons",
        "Status bar icons"
    )

    val networkSpeedLabels = listOf(
        "Real-time network speed",
        "Real-time network data",
        "Network speed",
        "Show network speed",
        "Display network speed"
    )

    // Investigation keywords for Wired Headphone / Earphone indicator controls
    val wiredHeadphoneCandidateLabels = listOf(
        "Wired headset",
        "Wired headphones",
        "Headset icon",
        "Earphones",
        "Headphone indicator",
        "Wired audio indicator",
        "Connected audio device icon",
        "Headphones",
        "Headset",
        "Earphone icon",
        "Audio jack icon"
    )

    /**
     * Match a Settings label as a complete word or phrase. This deliberately
     * permits labels such as "Show time" to match the candidate "Time", while
     * preventing the same short candidate from matching "Screen timeout".
     */
    fun matchesSettingsLabel(value: String, candidate: String): Boolean {
        val actual = value.trim()
        val expected = candidate.trim()
        if (actual.isEmpty() || expected.isEmpty()) return false
        return Regex(
            """(?<![\p{L}\p{N}])${Regex.escape(expected)}(?![\p{L}\p{N}])""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(actual)
    }

    /**
     * Strict safety check: Never toggle the battery percentage display option!
     * The screenshot and specification explicitly demand differentiating between
     * the actual battery icon vs. the separate "Show battery percentage" setting.
     */
    fun isBatteryPercentageOption(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return lower.contains("percentage") || lower.contains("percent")
    }

    /**
     * Checks if a label inside the status bar page is a known non-icon control
     * (such as general page headers, back buttons, or navigation items).
     */
    fun isDisallowedStatusIconRow(text: String): Boolean {
        if (isBatteryPercentageOption(text)) return true
        val lower = text.lowercase(Locale.ROOT)
        val prohibited = listOf(
            "notifications", "lock screen", "banner", "badges",
            "more settings", "manage notifications", "app notifications"
        )
        return prohibited.any { lower.contains(it) }
    }

    /**
     * Checks if a label represents an audio or headphone indicator setting.
     */
    fun isHeadphoneRelatedLabel(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return wiredHeadphoneCandidateLabels.any { lower.contains(it.lowercase(Locale.ROOT)) }
    }

    fun matchesAny(text: CharSequence?, candidates: List<String>): Boolean {
        if (text == null) return false
        val trimmed = text.toString().trim()
        return candidates.any { candidate ->
            trimmed.equals(candidate, ignoreCase = true) ||
                    trimmed.contains(candidate, ignoreCase = true)
        }
    }
}
