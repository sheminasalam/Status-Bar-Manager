package com.statusbarmanager.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.statusbarmanager.app.MainActivity
import com.statusbarmanager.app.automation.AutomationProgress
import com.statusbarmanager.app.automation.AutomationStep

/** Full-screen, minimal progress cover shown above Settings while SysHush runs. */
class AutomationOverlayManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "syshush_progress_channel"
        private const val NOTIFICATION_ID = 2001
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: View? = null
    private var attached = false
    private var titleView: TextView? = null
    private var percentView: TextView? = null
    private var progressBar: ProgressBar? = null
    private var detailView: TextView? = null
    private var doneButton: Button? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "SysHush progress", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Overall progress while SysHush applies system changes"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
    fun canDrawOverlay(): Boolean = Settings.canDrawOverlays(context)

    fun updateProgress(progress: AutomationProgress) {
        mainHandler.post {
            if (canDrawOverlay()) {
                ensureOverlay()
                render(progress)
            } else {
                removeOverlay()
            }
            updateNotification(progress)
        }
    }

    private fun ensureOverlay() {
        if (overlayView != null || windowManager == null) return
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            setOnTouchListener { _, _ -> true }
            setPadding(dp(28), dp(36), dp(28), dp(36))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(6, 12, 27), Color.rgb(13, 27, 48), Color.rgb(7, 14, 28))
            )
        }

        root.addView(TextView(context).apply {
            text = "SYSHUSH"
            setTextColor(Color.rgb(56, 189, 248))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            letterSpacing = 0.18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })

        titleView = TextView(context).apply {
            text = "Hush Button"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 29f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(10))
        }.also(root::addView)

        percentView = TextView(context).apply {
            text = "0%"
            setTextColor(Color.rgb(56, 189, 248))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(28), 0, dp(18))
        }.also(root::addView)

        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(14, 165, 233))
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(30, 41, 59))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8))
        }.also(root::addView)

        detailView = TextView(context).apply {
            text = ""
            setTextColor(Color.rgb(254, 202, 202))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            maxLines = 8
            setPadding(0, dp(22), 0, dp(8))
            visibility = View.GONE
        }.also(root::addView)

        doneButton = Button(context).apply {
            text = "Done"
            visibility = View.GONE
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.rgb(14, 165, 233))
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
                dismiss()
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // The overlay is still dismissed even if Android blocks the app launch.
                }
            }
            layoutParams = LinearLayout.LayoutParams(dp(180), dp(48)).apply {
                gravity = Gravity.CENTER
                topMargin = dp(30)
            }
        }.also(root::addView)

        overlayView = root
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
        try {
            windowManager.addView(root, params)
            attached = true
        } catch (_: Exception) {
            overlayView = null
            attached = false
        }
    }

    private fun render(progress: AutomationProgress) {
        val finished = progress.currentStep == AutomationStep.COMPLETED ||
            progress.currentStep == AutomationStep.PARTIAL ||
            progress.currentStep == AutomationStep.FAILED
        titleView?.text = when (progress.currentStep) {
            AutomationStep.COMPLETED -> "Hushed"
            AutomationStep.PARTIAL -> "Partially complete"
            AutomationStep.FAILED -> "Not completed"
            else -> "Hush Button"
        }
        percentView?.text = "${progress.percentage.coerceIn(0, 100)}%"
        progressBar?.progress = progress.percentage.coerceIn(0, 100)

        if (progress.currentStep == AutomationStep.FAILED) {
            detailView?.text = progress.errorReason
                ?: progress.currentOperation
                ?: "SysHush could not complete this operation."
            detailView?.visibility = View.VISIBLE
            doneButton?.text = "Back to SysHush"
        } else {
            detailView?.text = ""
            detailView?.visibility = View.GONE
            doneButton?.text = "Done"
        }
        doneButton?.visibility = if (finished) View.VISIBLE else View.GONE
    }

    private fun updateNotification(progress: AutomationProgress) {
        val manager = notificationManager ?: return
        val finished = progress.currentStep == AutomationStep.COMPLETED ||
            progress.currentStep == AutomationStep.PARTIAL ||
            progress.currentStep == AutomationStep.FAILED
        val text = when (progress.currentStep) {
            AutomationStep.COMPLETED -> "All done"
            AutomationStep.PARTIAL -> "Finished with some items skipped"
            AutomationStep.FAILED -> "Operation needs attention"
            else -> "Applying system changes"
        }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, NOTIFICATION_ID, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle(if (finished) "SysHush finished" else "SysHush is running")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setProgress(100, progress.percentage.coerceIn(0, 100), false)
            .setOngoing(!finished)
            .setOnlyAlertOnce(true)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // The full-screen overlay is the primary progress surface.
        }
    }

    fun dismiss() {
        removeOverlay()
        notificationManager?.cancel(NOTIFICATION_ID)
    }

    private fun removeOverlay() {
        if (attached && overlayView != null && windowManager != null) {
            try { windowManager.removeView(overlayView) } catch (_: Exception) { }
        }
        overlayView = null
        attached = false
    }
}
