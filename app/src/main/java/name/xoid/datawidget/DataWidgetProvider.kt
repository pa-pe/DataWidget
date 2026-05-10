package name.xoid.datawidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews

class DataWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DataWidgetProvider", "onReceive: ${intent.action}")
        super.onReceive(context, intent)
        
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, DataWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val serviceIntent = Intent(context, UpdateService::class.java).apply {
                    action = UpdateService.ACTION_UPDATE_WIDGETS
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("DataWidgetProvider", "onUpdate called for ${appWidgetIds.joinToString()}")
        
        // Set initial "Loading..." state
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_main)
            
            // Setup minimal click handling even in Initializing state
            val toggleIntent = Intent(context, UpdateService::class.java).apply {
                action = UpdateService.ACTION_TOGGLE_CONTROLS
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val togglePendingIntent = android.app.PendingIntent.getService(
                context, appWidgetId, toggleIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, togglePendingIntent)

            // Apply background from settings
            val bgColor = WidgetSettings.getBgColor(context, appWidgetId)
            val bgAlpha = WidgetSettings.getBgAlpha(context, appWidgetId)
            val finalColor = Color.argb(
                (bgAlpha * 255).toInt().coerceIn(0, 255),
                Color.red(bgColor),
                Color.green(bgColor),
                Color.blue(bgColor)
            )
            views.setInt(R.id.widget_root, "setBackgroundColor", finalColor)

            views.removeAllViews(R.id.widget_container)
            val loadingView = RemoteViews(context.packageName, R.layout.widget_col_12)
            loadingView.setTextViewText(R.id.item_text, "Initializing...")
            views.addView(R.id.widget_container, loadingView)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        // Start the service to handle updates
        val serviceIntent = Intent(context, UpdateService::class.java).apply {
            action = UpdateService.ACTION_UPDATE_WIDGETS
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        try {
            context.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            Log.e("DataWidgetProvider", "Failed to start service", e)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (id in appWidgetIds) {
            WidgetSettings.deleteSettings(context, id)
        }
        val intent = Intent(context, UpdateService::class.java).apply {
            action = UpdateService.ACTION_REMOVE_WIDGETS
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        context.startForegroundService(intent)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        val intent = Intent(context, UpdateService::class.java)
        context.stopService(intent)
    }
}
