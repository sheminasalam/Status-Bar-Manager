package com.statusbarmanager.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.statusbarmanager.app.MainActivity
import com.statusbarmanager.app.R
import com.statusbarmanager.app.automation.EngageMode
import com.statusbarmanager.app.automation.OperationCoordinator
import com.statusbarmanager.app.automation.SettingsAutomationManager
import com.statusbarmanager.app.data.DataStoreRepository
import com.statusbarmanager.app.data.SettingsBackupManager
import com.statusbarmanager.app.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * OBJECTIVE 4: Native Android Home-Screen Widget Provider.
 * Allows users to Engage and Disengage StatusBar Manager directly from their home screen.
 * - Displays current state: NORMAL / ENGAGED / RUNNING / PARTIAL
 * - Shows selected Engage Button Behaviour
 * - Action button triggers central OperationCoordinator
 * - If Accessibility permission is missing, opens permission onboarding in MainActivity
 */
class StatusBarWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_TOGGLE = "com.statusbarmanager.app.widget.ACTION_WIDGET_TOGGLE"
        const val ACTION_WIDGET_FORCE_STOP = "com.statusbarmanager.app.widget.ACTION_WIDGET_FORCE_STOP"
        const val ACTION_WIDGET_STATUS_BAR_ONLY = "com.statusbarmanager.app.widget.ACTION_WIDGET_STATUS_BAR_ONLY"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, StatusBarWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, StatusBarWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repo = DataStoreRepository(context)
        val automationManager = SettingsAutomationManager(context)

        CoroutineScope(Dispatchers.IO).launch {
            val isHideActive = repo.isHideActiveFlow.first()
            val backupModel = repo.backupModelFlow.first()
            val engageMode = repo.engageModeFlow.first()
            val isAccGranted = automationManager.isAccessibilityPermissionGranted()

            for (widgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId, isHideActive, backupModel?.isPartial ?: false, engageMode, isAccGranted)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_WIDGET_TOGGLE &&
            intent.action != ACTION_WIDGET_FORCE_STOP &&
            intent.action != ACTION_WIDGET_STATUS_BAR_ONLY
        ) return

        val repo = DataStoreRepository(context)
        val backupManager = SettingsBackupManager(repo)
        val automationManager = SettingsAutomationManager(context)
        val coordinator = OperationCoordinator(context, repo, backupManager)

        fun openMain(prompt: String) {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(prompt, true)
            }
            context.startActivity(openIntent)
        }

        if (!automationManager.isAccessibilityPermissionGranted()) {
            openMain("prompt_accessibility")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            if (intent.action == ACTION_WIDGET_STATUS_BAR_ONLY) {
                coordinator.toggleStatusBarOnly()
                updateAllWidgets(context)
                return@launch
            }

            if (intent.action == ACTION_WIDGET_FORCE_STOP) {
                coordinator.forceStopAppsOnly()
                updateAllWidgets(context)
                return@launch
            }

            val isHideActive = repo.isHideActiveFlow.first()
            if (isHideActive) {
                coordinator.disengage()
                updateAllWidgets(context)
                return@launch
            }

            val engageMode = repo.engageModeFlow.first()
            coordinator.engage()
            updateAllWidgets(context)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        isHideActive: Boolean,
        isPartial: Boolean,
        engageMode: EngageMode,
        isAccessibilityGranted: Boolean
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_status_bar_manager)

        // Set Icon container background, bubble, and badge according to state
        when {
            !isAccessibilityGranted -> {
                views.setInt(R.id.widget_icon_container, "setBackgroundResource", R.drawable.widget_icon_bg_normal)
                views.setInt(R.id.widget_notification_bubble, "setBackgroundResource", R.drawable.widget_bubble_error)
                views.setTextViewText(R.id.widget_notification_bubble, "!")
                views.setTextViewText(R.id.widget_status_badge, "PERMISSION")
                views.setTextColor(R.id.widget_status_badge, Color.parseColor("#EF4444"))
            }
            isPartial -> {
                views.setInt(R.id.widget_icon_container, "setBackgroundResource", R.drawable.widget_icon_bg_active)
                views.setInt(R.id.widget_notification_bubble, "setBackgroundResource", R.drawable.widget_bubble_partial)
                views.setTextViewText(R.id.widget_notification_bubble, "!")
                views.setTextViewText(R.id.widget_status_badge, "PARTIAL")
                views.setTextColor(R.id.widget_status_badge, Color.parseColor("#F59E0B"))
            }
            isHideActive -> {
                views.setInt(R.id.widget_icon_container, "setBackgroundResource", R.drawable.widget_icon_bg_active)
                views.setInt(R.id.widget_notification_bubble, "setBackgroundResource", R.drawable.widget_bubble_active)
                views.setTextViewText(R.id.widget_notification_bubble, "✓")
                views.setTextViewText(R.id.widget_status_badge, "HUSHED")
                views.setTextColor(R.id.widget_status_badge, Color.parseColor("#38BDF8"))
            }
            else -> {
                views.setInt(R.id.widget_icon_container, "setBackgroundResource", R.drawable.widget_icon_bg_normal)
                views.setInt(R.id.widget_notification_bubble, "setBackgroundResource", R.drawable.widget_bubble_normal)
                views.setTextViewText(R.id.widget_notification_bubble, "")
                views.setTextViewText(R.id.widget_status_badge, "NORMAL")
                views.setTextColor(R.id.widget_status_badge, Color.parseColor("#10B981"))
            }
        }

        // Toggle PendingIntent on click
        val toggleIntent = Intent(context, StatusBarWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_TOGGLE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_icon_container, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
