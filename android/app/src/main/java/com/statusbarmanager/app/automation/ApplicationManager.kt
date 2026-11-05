package com.statusbarmanager.app.automation

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.util.Log

data class AppItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val isWhitelisted: Boolean,
    val isProtectedRole: Boolean,
    val protectionReason: String? = null
)

class ApplicationManager(private val context: Context) {

    companion object {
        private const val TAG = "ApplicationManager"
    }

    private val packageManager: PackageManager = context.packageManager

    /**
     * Resolves default home launcher package
     */
    private fun getDefaultLauncherPackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    /**
     * Resolves active input method package (keyboard)
     */
    private fun getActiveInputMethodPackage(): String? {
        val defaultIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return defaultIme?.split("/")?.firstOrNull()
    }

    /**
     * Resolves default dialer/phone app
     */
    private fun getDefaultDialerPackage(): String? {
        val intent = Intent(Intent.ACTION_DIAL)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    /**
     * Resolves default SMS app
     */
    private fun getDefaultSmsPackage(): String? {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("smsto:")
        }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    /**
     * Discovers all installed applications with automatic system and critical role classification.
     */
    fun getInstalledApplications(userWhitelist: Set<String>): List<AppItem> {
        val installed = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve installed packages: ${e.message}")
            emptyList()
        }

        val myPackage = context.packageName
        val launcherPackage = getDefaultLauncherPackage()
        val imePackage = getActiveInputMethodPackage()
        val dialerPackage = getDefaultDialerPackage()
        val smsPackage = getDefaultSmsPackage()

        return installed.mapNotNull { appInfo ->
            val pkg = appInfo.packageName
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)

            val name = try {
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg
            }

            // Classification of critical protection
            var isProtectedRole = false
            var protectionReason: String? = null

            when {
                pkg == myPackage -> {
                    isProtectedRole = true
                    protectionReason = "Self (SysHush)"
                }
                pkg == launcherPackage -> {
                    isProtectedRole = true
                    protectionReason = "Active Home Launcher"
                }
                pkg == imePackage -> {
                    isProtectedRole = true
                    protectionReason = "Active Input Method (Keyboard)"
                }
                pkg == dialerPackage -> {
                    isProtectedRole = true
                    protectionReason = "Default Phone / Dialer"
                }
                pkg == smsPackage -> {
                    isProtectedRole = true
                    protectionReason = "Default Messaging Handler"
                }
                pkg.startsWith("com.android.systemui") || pkg.startsWith("com.google.android.inputmethod") -> {
                    isProtectedRole = true
                    protectionReason = "Essential System Component"
                }
            }

            val isWhitelisted = userWhitelist.contains(pkg)

            AppItem(
                packageName = pkg,
                appName = name,
                isSystemApp = isSystem,
                isWhitelisted = isWhitelisted,
                isProtectedRole = isProtectedRole,
                protectionReason = protectionReason
            )
        }.sortedWith(compareBy({ it.isSystemApp }, { it.isProtectedRole }, { it.appName.lowercase() }))
    }

    /**
     * Filters for apps that are eligible for Force Stop automation.
     * Strictly excludes system apps, self, protected roles, and user whitelisted apps.
     */
    fun getEligibleAppsForForceStop(userWhitelist: Set<String>): List<AppItem> {
        val all = getInstalledApplications(userWhitelist)
        return all.filter { app ->
            !app.isSystemApp &&
                    !app.isProtectedRole &&
                    !app.isWhitelisted &&
                    app.packageName != context.packageName
        }
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }
}
