package com.statusbarmanager.app.automation

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import android.provider.Settings
import android.util.Log

data class AppItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val isWhitelisted: Boolean,
    val isProtectedRole: Boolean,
    val protectionReason: String? = null,
    val isStopped: Boolean = false
)

class ApplicationManager(private val context: Context) {

    companion object {
        private const val TAG = "ApplicationManager"
        const val RECENT_APP_WINDOW_MINUTES = 15L
    }

    private val packageManager: PackageManager = context.packageManager

    fun hasUsageAccess(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.w(TAG, "Unable to check Usage Access: ${e.message}")
            false
        }
    }

    private fun getDefaultLauncherPackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    private fun getActiveInputMethodPackage(): String? {
        val defaultIme = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )
        return defaultIme?.split("/")?.firstOrNull()
    }

    private fun getDefaultDialerPackage(): String? {
        val resolveInfo = packageManager.resolveActivity(
            Intent(Intent.ACTION_DIAL),
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolveInfo?.activityInfo?.packageName
    }

    private fun getDefaultSmsPackage(): String? {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("smsto:")
        }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    fun getInstalledApplications(userWhitelist: Set<String>): List<AppItem> {
        val installed = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
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

        return installed.map { appInfo ->
            val pkg = appInfo.packageName
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)
            val isStopped = (appInfo.flags and ApplicationInfo.FLAG_STOPPED) != 0

            val name = try {
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg
            }

            val protectionReason = when {
                pkg == myPackage -> "Self (SysHush)"
                pkg == launcherPackage -> "Active Home Launcher"
                pkg == imePackage -> "Active Input Method (Keyboard)"
                pkg == dialerPackage -> "Default Phone / Dialer"
                pkg == smsPackage -> "Default Messaging Handler"
                pkg.startsWith("com.android.systemui") ||
                    pkg.startsWith("com.google.android.inputmethod") ->
                    "Essential System Component"
                else -> null
            }

            AppItem(
                packageName = pkg,
                appName = name,
                isSystemApp = isSystem,
                isWhitelisted = userWhitelist.contains(pkg),
                isProtectedRole = protectionReason != null,
                protectionReason = protectionReason,
                isStopped = isStopped
            )
        }.sortedWith(compareBy({ it.isSystemApp }, { it.isProtectedRole }, { it.appName.lowercase() }))
    }

    /**
     * Finds recently used user apps instead of iterating over every installed app.
     *
     * Android does not expose a reliable third-party list of all live background
     * processes on modern Android. Usage Access supplies a bounded recent-use
     * candidate list. FLAG_STOPPED additionally excludes packages Android already
     * considers stopped (including apps that have been force-stopped).
     */
    fun getEligibleAppsForForceStop(
        userWhitelist: Set<String>,
        recentWindowMinutes: Long = RECENT_APP_WINDOW_MINUTES
    ): List<AppItem> {
        if (!hasUsageAccess()) {
            Log.w(TAG, "Usage Access is required to select recently used applications.")
            return emptyList()
        }

        val now = System.currentTimeMillis()
        val cutoff = now - recentWindowMinutes.coerceAtLeast(1L) * 60_000L
        val recentPackages = try {
            val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            usageManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, cutoff, now)
                .orEmpty()
                .filter { it.lastTimeUsed in cutoff..now }
                .map { it.packageName }
                .toSet()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to read recent app usage: ${e.message}", e)
            emptySet()
        }

        if (recentPackages.isEmpty()) return emptyList()

        return getInstalledApplications(userWhitelist).filter { app ->
            recentPackages.contains(app.packageName) &&
                !app.isSystemApp &&
                !app.isProtectedRole &&
                !app.isWhitelisted &&
                !app.isStopped &&
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
