package com.statusbarmanager.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.statusbarmanager.app.R

abstract class IconActionWidgetProvider : AppWidgetProvider() {
    protected abstract val layoutId: Int
    protected abstract val action: String
    protected abstract val requestCodeBase: Int

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, layoutId)
            val actionIntent = Intent(context, StatusBarWidgetProvider::class.java).apply {
                action = this@IconActionWidgetProvider.action
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCodeBase + appWidgetId,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.quick_action_root, pendingIntent)
            manager.updateAppWidget(appWidgetId, views)
        }
    }
}

class HushButtonIconWidgetProvider : IconActionWidgetProvider() {
    override val layoutId = R.layout.widget_syshush_hush_button_icon
    override val action = StatusBarWidgetProvider.ACTION_WIDGET_TOGGLE
    override val requestCodeBase = 4100
}

class StopAppsWidgetProvider : IconActionWidgetProvider() {
    override val layoutId = R.layout.widget_syshush_stop_apps_icon
    override val action = StatusBarWidgetProvider.ACTION_WIDGET_FORCE_STOP
    override val requestCodeBase = 4200
}
