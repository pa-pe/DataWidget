package name.xoid.datawidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log

class DataWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("DataWidgetProvider", "onUpdate called for ${appWidgetIds.joinToString()}")
        // Start the service to handle updates
        val intent = Intent(context, UpdateService::class.java).apply {
            action = UpdateService.ACTION_UPDATE_WIDGETS
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        context.startForegroundService(intent)
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
