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
            DeviceOem.COLOR_OS_OXYGEN_OS -> "ColorOS / OxygenOS / Realme UI (Target Hardware)"
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
        "Notifications & Status Bar",
        "Status bar & notifications",
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

    val doNotShowLabels = listOf(
        "Do not show",
        "Don't show",
        "Hide",
        "Do not display",
        "None"
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

    /**
     * Checks if a label refers to battery percentage rather than the actual battery icon.
     * Requirement: Do not mistake the percentage display option for the battery icon visibility setting.
     */
    fun isBatteryPercentageOption(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return lower.contains("percentage") || lower.contains("percent")
    }

    /**
     * Matches known status bar icons while filtering out prohibited percentage toggles.
     */
    fun isTargetStatusIconLabel(text: String): Boolean {
        if (isBatteryPercentageOption(text)) return false
        val lower = text.lowercase(Locale.ROOT)
        val iconKeywords = listOf(
            "sim", "network", "mobile", "wi-fi", "wifi", "bluetooth",
            "alarm", "nfc", "earphones", "headset", "hotspot",
            "vpn", "data saving", "high performance", "ring", "vibrate",
            "silent", "battery" // battery icon (not percentage)
        )
        return iconKeywords.any { lower.contains(it) }
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
