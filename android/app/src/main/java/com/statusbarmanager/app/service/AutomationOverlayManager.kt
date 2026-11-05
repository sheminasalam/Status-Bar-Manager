package com.statusbarmanager.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.app.NotificationCompat
import com.statusbarmanager.app.MainActivity
import com.statusbarmanager.app.automation.AutomationProgress
import com.statusbarmanager.app.automation.AutomationStep

/**
 * Manages the floating progress overlay (TYPE_APPLICATION_OVERLAY) shown over Settings,
 * with automatic fallback to ongoing status notifications when overlay permission is unavailable.
 */
class AutomationOverlayManager(private val context: Context) {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "status_bar_manager_channel"
        private const val NOTIFICATION_ID = 2001
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: View? = null
    private var isOverlayAttached = false
    private var isMinimized = false

    // UI elements in overlay
    private var titleView: TextView? = null
    private var operationView: TextView? = null
    private var percentageView: TextView? = null
    private var progressBar: ProgressBar? = null
    private var statsView: TextView? = null
    private var contentContainer: LinearLayout? = null

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Status Bar Automation Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live progress during status bar automation operations"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun canDrawOverlay(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Shows or updates the floating overlay and fallback notification
     */
    fun updateProgress(progress: AutomationProgress) {
        mainHandler.post {
            // Update Floating Window if permission is granted
            if (canDrawOverlay()) {
                ensureOverlayViewCreated()
                updateOverlayContent(progress)
            } else {
                removeOverlayView()
            }

            // Always update fallback notification as secondary or primary status feed
            updateNotification(progress)

            // Auto-dismiss after completion or failure
            if (progress.currentStep == AutomationStep.COMPLETED ||
                progress.currentStep == AutomationStep.PARTIAL ||
                progress.currentStep == AutomationStep.FAILED
            ) {
                mainHandler.postDelayed({
                    dismiss()
                }, 2200)
            }
        }
    }

    private fun ensureOverlayViewCreated() {
        if (overlayView != null || windowManager == null) return

        val density = context.resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        // Outer Container Card
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#0F172A")) // Dark surface slate-900
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.parseColor("#3B82F6")) // Vibrant blue accent
            }
            background = bg
            setPadding(dp(16), dp(12), dp(16), dp(14))
            elevation = dp(8).toFloat()
        }

        // Header Row (Title + Percentage + Minimize/Close)
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(context).apply {
            text = "Status Bar Automation"
            setTextColor(Color.parseColor("#F8FAFC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleView = title
        headerRow.addView(title)

        val percent = TextView(context).apply {
            text = "0%"
            setTextColor(Color.parseColor("#38BDF8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(8), 0, dp(12), 0)
        }
        percentageView = percent
        headerRow.addView(percent)

        val minBtn = Button(context).apply {
            text = "—"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            val btnBg = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = dp(6).toFloat()
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(26))
            setPadding(0, 0, 0, 0)
            setOnClickListener {
                toggleMinimize()
            }
        }
        headerRow.addView(minBtn)

        root.addView(headerRow)

        // Expandable Content Body
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        contentContainer = body

        // Determinate Progress Bar
        val pBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            val pbg = GradientDrawable().apply {
                cornerRadius = dp(4).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(6)
            ).apply {
                setMargins(0, dp(6), 0, dp(8))
            }
        }
        progressBar = pBar
        body.addView(pBar)

        // Current Operation Text
        val opText = TextView(context).apply {
            text = "Preparing..."
            setTextColor(Color.parseColor("#E2E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        operationView = opText
        body.addView(opText)

        // Detailed Statistics Text
        val stats = TextView(context).apply {
            text = "Icons: 0 discovered"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, dp(4), 0, 0)
        }
        statsView = stats
        body.addView(stats)

        root.addView(body)
        overlayView = root

        // Setup WindowManager LayoutParams
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(24) // Top offset below status bar
            // Margins in pixel
            width = context.resources.displayMetrics.widthPixels - dp(32)
        }

        try {
            windowManager.addView(root, params)
            isOverlayAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
            isOverlayAttached = false
        }
    }

    private fun toggleMinimize() {
        isMinimized = !isMinimized
        contentContainer?.visibility = if (isMinimized) View.GONE else View.VISIBLE
    }

    private fun updateOverlayContent(progress: AutomationProgress) {
        titleView?.text = progress.operationTitle
        percentageView?.text = "${progress.percentage}%"
        progressBar?.progress = progress.percentage
        operationView?.text = progress.currentOperation

        val stats = progress.iconStats
        val statsText = when {
            stats.discovered > 0 && stats.disabled > 0 ->
                "Icons: ${stats.discovered} found | ${stats.disabled} disabled | ${stats.alreadyDisabled} off | ${stats.remaining} left"
            stats.discovered > 0 && stats.restored > 0 ->
                "Icons: ${stats.discovered} found | ${stats.restored} restored | ${stats.remaining} left"
            stats.discovered > 0 ->
                "Icons: ${stats.discovered} discovered"
            else ->
                "Step ${progress.stepIndex} of ${progress.totalSteps}"
        }
        statsView?.text = statsText
    }

    private fun updateNotification(progress: AutomationProgress) {
        if (notificationManager == null) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isFinished = progress.currentStep == AutomationStep.COMPLETED ||
                progress.currentStep == AutomationStep.PARTIAL ||
                progress.currentStep == AutomationStep.FAILED

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle(progress.operationTitle)
            .setContentText(progress.currentOperation)
            .setContentIntent(pendingIntent)
            .setProgress(100, progress.percentage, false)
            .setOngoing(!isFinished)
            .setOnlyAlertOnce(true)

        if (progress.iconStats.discovered > 0) {
            val stats = progress.iconStats
            builder.setSubText("Icons: ${stats.discovered} found, ${stats.disabled + stats.restored} processed")
        }

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            // Ignored on missing notification permission
        }
    }

    fun dismiss() {
        removeOverlayView()
        notificationManager?.cancel(NOTIFICATION_ID)
    }

    private fun removeOverlayView() {
        if (isOverlayAttached && overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
            isOverlayAttached = false
        }
    }
}
