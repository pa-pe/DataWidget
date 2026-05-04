package name.xoid.datawidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews

class DataWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DataWidgetProvider", "onReceive: ${intent.action}")
        super.onReceive(context, intent)
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
            views.removeAllViews(R.id.widget_container)
            val loadingView = RemoteViews(context.packageName, R.layout.widget_col_12)
            loadingView.setTextViewText(R.id.item_text, "Initializing...")
            views.addView(R.id.widget_container, loadingView)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        // Start the service to handle updates
        val intent = Intent(context, UpdateService::class.java).apply {
            action = UpdateService.ACTION_UPDATE_WIDGETS
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Log.e("DataWidgetProvider", "Failed to start service", e)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (id in appWidgetIds) {
            WidgetSettings.deleteUrl(context, id)
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
