package com.statusbarmanager.app.automation

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.Log

/**
 * Exclude cached-only packages: Android can retain their processes briefly after
 * an app task has been closed. This admits non-cached foreground, visible,
 * perceptible and service-backed processes.
 */
internal fun isActiveProcessImportance(importance: Int): Boolean =
    importance < ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED

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
        const val RECENT_APP_WINDOW_MINUTES = 5L
    }

    private val packageManager: PackageManager = context.packageManager

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
     * Selects only non-cached user apps that Android currently reports as running.
     *
     * Unlike UsageStatsManager's last-used timestamp, this does not include an app
     * merely because it was opened during a recent time window. Cached-only processes
     * are excluded because Android can retain them after the user closes an app.
     *
     * This is a live-process approximation, not a guaranteed list of Recents task cards.
     */
    fun getEligibleAppsForForceStop(userWhitelist: Set<String>): List<AppItem> {
        val myPackage = context.packageName
        val launcherPackage = getDefaultLauncherPackage()
        val imePackage = getActiveInputMethodPackage()
        val dialerPackage = getDefaultDialerPackage()
        val smsPackage = getDefaultSmsPackage()

        fun isProtected(pkg: String): Boolean =
            pkg == myPackage ||
            pkg == launcherPackage ||
            pkg == imePackage ||
            pkg == dialerPackage ||
            pkg == smsPackage ||
            pkg.startsWith("com.android.systemui") ||
            pkg.startsWith("com.google.android.inputmethod")

        // 1. Inspect live process importance if accessible by system
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val runningProcessPackages = linkedSetOf<String>()
        if (activityManager != null) {
            try {
                val runningProcesses = activityManager.runningAppProcesses.orEmpty()
                for (process in runningProcesses) {
                    if (!isActiveProcessImportance(process.importance)) continue
                    process.pkgList.orEmpty().forEach { pkg ->
                        if (!isProtected(pkg) && !userWhitelist.contains(pkg)) {
                            runningProcessPackages.add(pkg)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to inspect active processes: ${e.message}")
            }
        }

        // 2. Discover all installed third-party user applications
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

        val eligibleCandidates = mutableListOf<AppItem>()

        for (appInfo in installed) {
            val pkg = appInfo.packageName
            if (isProtected(pkg) || userWhitelist.contains(pkg)) continue

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)
            val isStopped = (appInfo.flags and ApplicationInfo.FLAG_STOPPED) != 0

            // Skip system apps and apps that are already in the stopped state
            if (isSystem || isStopped) continue

            // Must be a user-launchable application (e.g. WhatsApp, Spotify, Chrome)
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent == null && !runningProcessPackages.contains(pkg)) continue

            val label = try {
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                pkg
            }

            eligibleCandidates.add(
                AppItem(
                    packageName = pkg,
                    appName = label,
                    isSystemApp = false,
                    isWhitelisted = false,
                    isProtectedRole = false,
                    isStopped = false
                )
            )
        }

        // Prioritize applications explicitly identified in active processes, then sort alphabetically
        return eligibleCandidates.sortedWith(
            compareBy(
                { !runningProcessPackages.contains(it.packageName) },
                { it.appName.lowercase() }
            )
        ).also { eligible ->
            Log.i(TAG, "Found ${eligible.size} eligible active user applications for force-stop.")
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
