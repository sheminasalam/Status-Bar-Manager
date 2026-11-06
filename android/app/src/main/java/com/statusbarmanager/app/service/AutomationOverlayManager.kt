package com.statusbarmanager.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.statusbarmanager.app.MainActivity
import com.statusbarmanager.app.automation.AutomationProgress
import com.statusbarmanager.app.automation.AutomationStep

/**
 * Notification-only progress reporter.
 *
 * This class deliberately does not create WindowManager windows, application overlays,
 * accessibility overlays, dialogs, or views. Keeping progress outside the window hierarchy
 * lets the AccessibilityService inspect Android Settings and App Info without a SysHush
 * window being attached over the system UI.
 */
class AutomationOverlayManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "syshush_progress_channel"
        private const val NOTIFICATION_ID = 2001
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "SysHush progress",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Automation progress and completion status"
                    setShowBadge(false)
                }
            )
        }
    }

    fun updateProgress(progress: AutomationProgress) {
        updateNotification(progress)
    }

    private fun updateNotification(progress: AutomationProgress) {
        val manager = notificationManager ?: return
        val finished = progress.currentStep == AutomationStep.COMPLETED ||
            progress.currentStep == AutomationStep.PARTIAL ||
            progress.currentStep == AutomationStep.FAILED

        val title = when (progress.currentStep) {
            AutomationStep.COMPLETED -> "SysHush completed"
            AutomationStep.PARTIAL -> "SysHush partially completed"
            AutomationStep.FAILED -> "SysHush failed"
            else -> "SysHush is working"
        }
        val text = when (progress.currentStep) {
            AutomationStep.COMPLETED -> "Operation completed successfully"
            AutomationStep.PARTIAL -> "Some items could not be processed"
            AutomationStep.FAILED -> progress.errorReason
                ?: progress.currentOperation
                ?: "Operation could not be completed"
            else -> "Working… ${progress.percentage.coerceIn(0, 99)}%"
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle(title)
            .setContentText(text.take(180))
            .setContentIntent(pendingIntent)
            .setProgress(100, progress.percentage.coerceIn(0, 100), false)
            .setOngoing(!finished)
            .setAutoCancel(finished)
            .setOnlyAlertOnce(true)
            .build()

        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Automation continues even when notification permission is denied.
        }
    }

    fun dismiss() {
        notificationManager?.cancel(NOTIFICATION_ID)
    }
}
